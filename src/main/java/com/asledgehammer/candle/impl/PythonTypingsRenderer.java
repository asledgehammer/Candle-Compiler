package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;
import java.util.*;

import static com.asledgehammer.candle.impl.PythonTypingsRenderer.ILLEGAL;

public class PythonTypingsRenderer {

  public static List<String> ILLEGAL;

  static {
    ILLEGAL =
        new ArrayList<>(
            Arrays.stream(
                    new String[] {
                      "False",
                      "def",
                      "if",
                      "raise",
                      "None",
                      "del",
                      "import",
                      "return",
                      "True",
                      "elif",
                      "in",
                      "try",
                      "and",
                      "else",
                      "is",
                      "while",
                      "as",
                      "except",
                      "lambda",
                      "with",
                      "assert",
                      "finally",
                      "nonlocal",
                      "yield",
                      "break",
                      "for",
                      "not",
                      "class",
                      "from",
                      "or",
                      "continue",
                      "global",
                      "pass"
                    })
                .toList());
  }

  Map<Package, PythonFile> mapFiles = new HashMap<>();

  static File dir;

  public void render(CandleGraph graph) {

    dir = new File("python/typings/");
    if (!dir.exists()) dir.mkdirs();
    List<Class<?>> clazzes = new ArrayList<>(graph.classes.keySet());
    clazzes.sort(Comparator.comparing(Class::getSimpleName));
    for (Class<?> clazz : clazzes) {
      if (clazz.getDeclaringClass() == null) {
        renderClass(graph, graph.classes.get(clazz));
      }
    }

    for (Package pkg : mapFiles.keySet()) {
      mapFiles.get(pkg).write();
    }
  }

  public void renderClass(CandleGraph graph, CandleClass clazz) {
    Package pkg = clazz.getClazz().getPackage();

    if (pkg == null) return;

    PythonFile file;
    if (mapFiles.containsKey(pkg)) {
      file = mapFiles.get(pkg);
    } else {
      file = new PythonFile(pkg);
      mapFiles.put(pkg, file);
    }

    file.lines.addAll(file.digestClass(graph, clazz, 0));
  }
}

class PythonFile {

  final List<Class<?>> referencedTypes = new ArrayList<>();
  final List<String> lines = new ArrayList<>();
  final List<String> typeVars = new ArrayList<>();
  final Package pkg;

  final Map<Class<?>, Map<String, String>> remappedTypeVariables = new HashMap<>();

  PythonFile(@NotNull Package pkg) {
    this.pkg = pkg;
  }

  List<String> digestClass(@NotNull CandleGraph graph, @NotNull CandleClass clazz, int indent) {

    if (clazz.getClazz().equals(Annotation.class)) {
      System.out.println("### GENCLASS: Annotation");
    }

    String i = "  ".repeat(indent);
    List<String> lines = new ArrayList<>();

    String sName = clazz.getClazz().getSimpleName();
    if (sName.contains("$")) {
      String[] split = sName.split("\\$");
      sName = split[split.length - 1];
    }

    String classDefLine = i + "class " + sName;

    TypeVariable[] tp = clazz.getClazz().getTypeParameters();
    if (tp.length != 0) {
      String s = "";

      for (TypeVariable tv : tp) {
        String name = tv.getName();

        boolean found = false;
        Class<?> tvc = clazz.getClazz().getEnclosingClass();
        while (tvc != null) {

          TypeVariable[] tp2 = tvc.getTypeParameters();
          for (TypeVariable tv2 : tp2) {
            if (tv2.getName().equals(name)) {
              found = true;
              break;
            }
          }

          if (found) {
            break;
          }

          tvc = tvc.getEnclosingClass();
        }

        if (found) {
          name = clazz.getClazz().getSimpleName() + "_" + name;
        }
        Map<String, String> map =
            remappedTypeVariables.computeIfAbsent(clazz.getClazz(), k -> new HashMap<>());
        map.put(tv.getName(), name);

        if (!typeVars.contains(name)) {
          typeVars.add(name);
        }

        s += name + ", ";
      }

      s = s.substring(0, s.length() - 2);
      classDefLine += "[" + s + "]";
    }

    Class<?> superClazz = clazz.getClazz().getSuperclass();
    if (superClazz != null && !superClazz.equals(Object.class)) {

      String toState = superClazz.getSimpleName();
      Class<?> encSuperClazz = superClazz.getEnclosingClass();
      while (encSuperClazz != null) {
        toState = encSuperClazz.getSimpleName() + '.' + toState;
        encSuperClazz = encSuperClazz.getEnclosingClass();
      }

      reference(superClazz);
      classDefLine += "(" + toState + ")";
    }
    classDefLine += ":";

    List<String> cLines = new ArrayList<>();

    boolean hasContent = false;

    if (clazz.hasStaticFields()) {

      Map<String, CandleField> fields = clazz.getStaticFields();

      List<String> fNames = new ArrayList<>(fields.keySet());
      fNames.sort(Comparator.naturalOrder());

      for (String fName : fNames) {
        if (ILLEGAL.contains(fName)
            || clazz.hasStaticMethod(fName)
            || clazz.hasInstanceMethod(fName)) {
          continue;
        }
        hasContent = true;
        cLines.addAll(digestStaticField(fields.get(fName), indent + 1));
        cLines.add("");
      }
    }

    if (clazz.hasInstanceMethods()) {

      Map<String, CandleExecutableCluster<CandleMethod>> clusters = clazz.getMethods();

      List<String> mNames = new ArrayList<>(clusters.keySet());
      mNames.sort(Comparator.naturalOrder());

      for (String mName : mNames) {
        if (ILLEGAL.contains(mName)) continue;
        hasContent = true;
        cLines.addAll(digestMethod2(clazz, clusters.get(mName), indent + 1));
        cLines.add("");
      }
    }

    if (clazz.hasStaticMethods()) {

      Map<String, CandleExecutableCluster<CandleMethod>> clusters = clazz.getStaticMethods();

      List<String> mNames = new ArrayList<>(clusters.keySet());
      mNames.sort(Comparator.naturalOrder());

      for (String mName : mNames) {

        if (ILLEGAL.contains(mName)) continue;
        else if (clazz.hasInstanceMethod(mName)) continue;

        hasContent = true;
        cLines.addAll(digestMethod2(clazz, clusters.get(mName), indent + 1));
        cLines.add("");
      }
    }

    if (clazz.hasConstructors()) {
      hasContent = true;
      cLines.addAll(digestConstructors2(clazz, clazz.getConstructors(), indent + 1));
      cLines.add("");
    }

    Class<?>[] innerClazzes = clazz.getClazz().getDeclaredClasses();
    if (innerClazzes.length != 0) {

      for (Class<?> innerClazz : innerClazzes) {
        if (!graph.classes.containsKey(innerClazz)) {
          continue;
        }
        hasContent = true;
        CandleClass cc = graph.classes.get(innerClazz);
        cLines.addAll(digestClass(graph, cc, indent + 1));
      }
    }

    if (!hasContent) {
      classDefLine += " ...";
      lines.add(classDefLine);
      lines.add("");
    } else {
      lines.add(classDefLine);
      lines.add("");
      lines.addAll(cLines);
    }

    if (indent == 0) {
      lines.add("");
    }

    if (clazz.getClazz().equals(Annotation.class)) {
      System.out.println(lines);
    }

    return lines;
  }

  List<String> digestStaticField(CandleField field, int indent) {

    String i = "  ".repeat(indent);

    List<String> lines = new ArrayList<>();

    reference(field.getClazz());

    String name = field.getName();
    String type = getTypeString(field.getClazz(), field.getBasicType(), false);
    String line = i + name + ": " + type;

    lines.add(line);

    return lines;
  }

  String digestParameters(
      @NotNull List<CandleParameter> parameters, @Nullable Map<String, String> map) {
    String line = "";
    for (CandleParameter param : parameters) {
      String type = getTypeString(param.getClazz(), param.getBasicType(), false);

      // (Generic params in sub-classes needs to accurately affect the Python-alias to prevent
      // mapping conflicts)
      if (map != null && map.containsKey(type)) {
        type = map.get(type);
      }

      reference(param.getClazz());
      line += param.getJavaParameter().getName() + ": " + type + ", ";
    }

    return line;
  }

  List<String> digestConstructors2(
      CandleClass clazz, CandleExecutableCluster<CandleConstructor> cluster, int indent) {

    String i = "  ".repeat(indent);

    List<String> lines = new ArrayList<>();

    Map<String, String> map = remappedTypeVariables.get(clazz.getClazz());

    List<CandleConstructor> constructors = cluster.getExecutables();
    for (CandleConstructor cons : constructors) {

      TypeVariable[] tv = cons.getExecutable().getTypeParameters();
      for (TypeVariable v : tv) {
        String name = v.getName();
        if (map != null && map.containsKey(name)) {
          name = map.get(name);
        }
        if (!typeVars.contains(name)) typeVars.add(name);
      }

      String line = i + "def __init__(";
      line += "self, ";
      if (cons.hasParameters()) {
        line += digestParameters(cons.getParameters(), map);
        //        for (CandleParameter param : cons.getParameters()) {
        //          String type = getTypeString(param.getClazz(), param.getBasicType(), false);
        //          line += param.getJavaParameter().getName() + ": " + type + ", ";
        //        }
      }
      line = line.substring(0, line.length() - 2);
      line += "):";

      if (constructors.size() != 1) {
        lines.add(i + "@overload");
      }
      if (clazz.hasInstanceFields()) {
        lines.add(line);

        Map<String, CandleField> fields = clazz.getInstanceFields();

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        fieldNames.sort(Comparator.naturalOrder());

        for (String fieldName : fieldNames) {
          String i2 = "  ".repeat(indent + 1);
          CandleField field = fields.get(fieldName);
          List<Class<?>> references =
              CandleUtils.resolveList(CandleUtils.extractClasses(field.getFullType(), false));
          reference(references);

          line =
              i2
                  + "self."
                  + field.getName()
                  + ": "
                  + getTypeString(field.getClazz(), field.getBasicType(), false);
          lines.add(line);
        }

      } else {
        line += " ...";
        lines.add(line);
      }
    }

    return lines;
  }

  List<String> digestMethod2(
      CandleClass clz, CandleExecutableCluster<CandleMethod> cluster, int indent) {

    String i = "  ".repeat(indent);

    List<String> lines = new ArrayList<>();

    Map<String, String> map = remappedTypeVariables.get(clz.getClazz());

    List<CandleMethod> methods = cluster.getExecutables();
    for (CandleMethod method : methods) {

      //       TODO: Look into this later on.
      //      if (Modifier.isNative(method.getExecutable().getModifiers())) {
      //        continue;
      //      }

      TypeVariable[] tv = method.getExecutable().getTypeParameters();
      for (TypeVariable v : tv) {
        String name = v.getName();
        if (map != null && map.containsKey(name)) {
          name = map.get(name);
        }
        if (!typeVars.contains(name)) typeVars.add(name);
      }

      boolean isStatic = method.isStatic();
      if (isStatic) {
        lines.add(i + "@staticmethod");
      }

      String line = i + "def " + cluster.getLuaName() + "(";
      boolean hasParam = false;
      if (!isStatic) {
        hasParam = true;
        line += "self, ";
      }
      if (method.hasParameters()) {
        hasParam = true;
        line += digestParameters(method.getParameters(), map);
      }
      if (hasParam) {
        line = line.substring(0, line.length() - 2);
      }

      try {
        reference(
            CandleUtils.resolveList(CandleUtils.extractClasses(method.getFullReturnType(), false)));
      } catch (Exception e) {
        // Sub-class with generics = fun so ignore because good programmer. =)
      }

      line += ")";
      line += " -> " + getTypeString(method.getReturnType(), method.getBasicReturnType(), false);
      line += ": ...";

      try {
        reference(
            CandleUtils.resolveList(CandleUtils.extractClasses(method.getFullReturnType(), false)));
      } catch (Exception e) {
        e.printStackTrace(System.err);
      }

      if (methods.size() != 1) {
        lines.add(i + "@overload");
      }
      lines.add(line);
    }

    return lines;
  }

  String getTypeString(@Nullable Class<?> clazz, @NotNull String typeName, boolean debug) {
    return getTypeString(clazz, typeName, debug, 0);
  }

  String getTypeString(
      @Nullable Class<?> clazz, @NotNull String typeName, boolean debug, int inside) {

    // JDK internal type for some lang packages.
    if (clazz == null && typeName.equals("F") && !typeVars.contains("F")) {
      typeVars.add("F");
    }

    if (debug) {
      System.out.println("getTypeString(" + clazz + ", " + typeName + ", " + inside + ")");
    }
    typeName =
        typeName
            .replaceAll("\\? extends ", "")
            .replaceAll("\\? super ", "")
            .replaceAll("\\?", "Any");

    boolean s = false;
    if (typeName.equals("Kind[]")) {
      s = true;
      System.out.println("getTypeString(" + clazz + ", " + typeName + ", " + inside + ")");
    }

    if (clazz != null) {
      if (clazz.isArray()) {
        reference(clazz.getComponentType());

        String name = clazz.getSimpleName();
        Class<?> eClazz = clazz.getEnclosingClass();
        while (eClazz != null) {
          name = eClazz.getSimpleName() + "." + name;
          eClazz = eClazz.getEnclosingClass();
        }

        return "list[" + getTypeString(clazz.getComponentType(), name, debug, inside + 1) + "]";

      } else if (clazz.equals(String.class)
          || clazz.equals(Character.class)
          || clazz.equals(char.class)) {
        return "str";
      } else if (clazz.equals(byte.class)
          || clazz.equals(short.class)
          || clazz.equals(int.class)
          || clazz.equals(long.class)) {
        return "int";
      } else if (clazz.equals(float.class) || clazz.equals(double.class)) {
        return "float";
      } else if (clazz.equals(void.class)) {
        return "None";
      } else if (clazz.equals(Object.class)) {
        return "object";
      } else if (clazz.equals(boolean.class)) {
        return "bool";
      }

      Class<?> encClazz = clazz.getEnclosingClass();
      if (encClazz != null) {
        typeName = clazz.getSimpleName();
        while (encClazz != null) {
          typeName = encClazz.getSimpleName() + "." + typeName;
          encClazz = encClazz.getEnclosingClass();
        }
      }
    }

    if (typeName.contains("<")) {
      List<String> args = commaSplit(typeName);
      if (debug) {
        System.out.println("args:");
        System.out.println(args);
      }

      String first = typeName.substring(0, typeName.indexOf("<"));
      first += "<";
      for (String arg : args) {
        first += getTypeString(null, arg, debug, inside + 1) + ", ";
      }
      typeName = first.substring(0, first.length() - 2);
    }

    typeName =
        switch (typeName.trim()) {
          case "String", "Character", "char" -> "str";
          case "boolean" -> "bool";
          case "short", "byte", "long" -> "int";
          case "double" -> "float";
          case "void" -> "None";
          case "Object" -> "object";
          default ->
              typeName.replaceAll("\\[]", "").replaceAll("<", "[").replaceAll(">", "]").trim();
        };

    if (inside == 0) {
      int count = 0;
      for (int index = 0; index < typeName.length(); index++) {
        char c = typeName.charAt(index);
        if (c == ']') {
          count--;
        } else if (c == '[') {
          count++;
        }
      }

      if (count != 0) {
        if (count > 0) {
          while (count > 0) {
            typeName += ']';
            count--;
          }
        } else {
          throw new RuntimeException("typeName is uneven with generics: " + typeName);
        }
      }
    }

    if (s) {
      System.out.println("Result: " + typeName);
    }

    return typeName;
  }

  public static List<String> commaSplit(@NotNull String s) {

    int inside = 0;
    List<String> args = new ArrayList<>();
    StringBuilder arg = new StringBuilder();

    for (int index = 0; index < s.length(); index++) {
      char c = s.charAt(index);

      if (c == '<') {
        inside++;
        if (inside == 1) {
          continue;
        }
      } else if (c == '>') {
        inside--;
        if (inside == 0) {
          break;
        }
      } else if (c == ',' && inside == 1) {
        args.add(arg.toString().trim());
        arg = new StringBuilder();
        continue;
      }
      if (inside > 0) {
        arg.append(c);
      }
    }

    if (!arg.isEmpty()) {
      args.add(arg.toString().trim());
    }

    return args;
  }

  boolean shouldReference(@NotNull Class<?> clazz) {
    Package pkg = clazz.getPackage();

    if (clazz.equals(Annotation.class)) {
      System.out.println("this.pkg: " + this.pkg.getName());
      System.out.println("Annotation.class.getPackage(): " + pkg.getName());
    }

    if (pkg != null && clazz.getPackage().getName().equals(this.pkg.getName())) {
      return false;
    } else if (clazz.equals(String.class)) {
      return false;
    }
    return true;
  }

  void write() {

    Map<Package, List<Class<?>>> map = new HashMap<>();
    for (Class<?> ref : referencedTypes) {
      Package pkg = ref.getPackage();

      List<Class<?>> list = map.computeIfAbsent(pkg, k -> new ArrayList<>());
      list.add(ref);
    }

    List<String> imports = new ArrayList<>();

    imports.add("from typing import Any, overload, TypeVar");

    List<Package> pkgs =
        new ArrayList<>(new ArrayList<>(map.keySet()).stream().filter(Objects::nonNull).toList());
    pkgs.sort(Comparator.comparing(Package::getName));

    for (Package pkg : pkgs) {

      if (pkg == null) continue;

      List<Class<?>> list = map.get(pkg);
      if (list.isEmpty()) continue;

      String line = "from " + pkg.getName() + " import ";
      for (Class<?> clazz : list) {
        line += clazz.getSimpleName() + ", ";
      }
      line = line.substring(0, line.length() - 2);
      imports.add(line);
    }

    List<String> lines = new ArrayList<>();
    if (!imports.isEmpty()) {
      lines.addAll(imports);

      lines.add("");
    }

    if (!typeVars.isEmpty()) {
      for (String tv : typeVars) {
        lines.add(tv + " = TypeVar('" + tv + "', default=Any)");
      }
      lines.add("");
    }

    lines.addAll(this.lines);

    StringBuilder s = new StringBuilder();
    for (String line : lines) {
      s.append(line).append("\n");
    }

    if (s.toString().endsWith("\n\n")) {
      s = new StringBuilder(s.substring(0, s.length() - 1));
    }

    String pPath = "";
    String pName = pkg.getName();

    if (pName.contains(".")) {
      String[] split = pName.split("\\.");
      for (int index = 0; index < split.length - 1; index++) {
        pPath += split[index] + "/";
      }
      pPath = pPath.substring(0, pPath.length() - 1);
      pName = split[split.length - 1];
    }

    File dir;
    if (pPath.isEmpty()) {
      dir = PythonTypingsRenderer.dir;
    } else {
      dir = new File(PythonTypingsRenderer.dir, pPath);
      if (!dir.exists()) dir.mkdirs();
    }

    File file = new File(dir, pName + ".pyi");

    System.out.println("Writing: " + file.getPath() + "..");
    CandleGraph.write(file, s.toString());
  }

  void reference(@NotNull Class<?> clazz) {
    if (clazz.equals(Object.class)) return;
    Class<?> enclosing = clazz.getEnclosingClass();
    if (enclosing != null) {
      reference(enclosing);
      return;
    }
    if (!referencedTypes.contains(clazz) && shouldReference(clazz)) referencedTypes.add(clazz);
  }

  void reference(@NotNull List<Class<?>> clazzes) {
    for (Class<?> clazz : clazzes) {
      reference(clazz);
    }
  }

  public static void main(String[] argz) {
    String s =
        new PythonFile(PythonFile.class.getPackage())
            .getTypeString(null, "Collection<? extends Map.Entry<Object, Object>>", true, 0);
    System.out.println(s);
  }
}
