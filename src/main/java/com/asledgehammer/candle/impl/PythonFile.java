package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import com.asledgehammer.rosetta.RosettaParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.TypeVariable;
import java.util.*;

import static com.asledgehammer.candle.impl.PythonTypingsRenderer.ILLEGAL;

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

    String i = "  ".repeat(indent);
    List<String> lines = new ArrayList<>();

    String sName = clazz.getClazz().getSimpleName();
    if (sName.contains("$")) {
      String[] split = sName.split("\\$");
      sName = split[split.length - 1];
    }

    // Anonymous, Lambda, Internal JDK classes does this.
    if (sName.isEmpty()) return lines;

    String classDefLine = i + "class " + sName;

    TypeVariable[] tp = clazz.getClazz().getTypeParameters();
    if (tp.length != 0) {
      StringBuilder s = new StringBuilder();

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

        s.append(name).append(", ");
      }

      s = new StringBuilder(s.substring(0, s.length() - 2));
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
        cLines.addAll(digestStaticField(clazz, fields.get(fName), indent + 1));
        if (!cLines.get(cLines.size() - 1).isEmpty()) {
          cLines.add("");
        }
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
        if (!cLines.get(cLines.size() - 1).isEmpty()) {
          cLines.add("");
        }
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
        if (!cLines.get(cLines.size() - 1).isEmpty()) {
          cLines.add("");
        }
      }
    }

    if (clazz.hasConstructors()) {
      hasContent = true;
      cLines.addAll(digestConstructors2(clazz, clazz.getConstructors(), indent + 1));
      if (!cLines.get(cLines.size() - 1).isEmpty()) {
        cLines.add("");
      }
    }

    Class<?>[] innerClazzes = clazz.getClazz().getDeclaredClasses();
    for (Class<?> innerClazz : innerClazzes) {
      if (!graph.classes.containsKey(innerClazz)) {
        continue;
      }
      hasContent = true;
      CandleClass cc = graph.classes.get(innerClazz);
      cLines.addAll(digestClass(graph, cc, indent + 1));
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

    return lines;
  }

  List<String> digestStaticField(CandleClass clazz, CandleField field, int indent) {

    String i = "  ".repeat(indent);

    List<String> lines = new ArrayList<>();

    reference(CandleUtils.resolveList(PythonUtils.extractClasses(field.getFullType(), false)));

    String name = field.getName();
    String type = getTypeString(field.getClazz(), field.getBasicType());
    String line = name + ": " + type;

    boolean commented = false;

    if (ILLEGAL.contains(name)) {
      commented = true;
    } else {
      // Check to see if there's a conflicting attribute.
      for (Class<?> c : clazz.getClazz().getClasses()) {
        if (c.getSimpleName().equals(name)) {
          commented = true;
          break;
        }
      }
    }

    if (commented) {
      line = i + "# " + line;
    } else {
      line = i + line;
    }

    lines.add(line);

    return lines;
  }

  String digestParameters(
      @NotNull List<CandleParameter> parameters, @Nullable Map<String, String> map) {
    StringBuilder line = new StringBuilder();
    for (CandleParameter param : parameters) {
      String type = getTypeString(param.getClazz(), param.getBasicType());

      // (Generic params in subclasses needs to accurately affect the Python-alias to prevent
      // mapping conflicts)
      if (map != null && map.containsKey(type)) {
        type = map.get(type);
      }

      reference(CandleUtils.resolveList(PythonUtils.extractClasses(param.getFullType(), false)));

      String pName = param.getJavaParameter().getName();
      RosettaParameter rParam = param.getDocs();
      String rParamName = rParam.getName();
      if (!ILLEGAL.contains(rParamName)) {
        pName = rParamName;
      }

      line.append(pName).append(": ").append(type).append(", ");
    }

    return line.toString();
  }

  List<String> digestConstructors2(
      CandleClass clazz, CandleExecutableCluster<CandleConstructor> cluster, int indent) {

    boolean wroteFields = false;

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
      }
      line = line.substring(0, line.length() - 2);
      line += "):";

      if (constructors.size() != 1) {
        lines.add(i + "@overload");
      }
      if (!wroteFields && clazz.hasInstanceFields()) {
        lines.add(line);

        Map<String, CandleField> fields = clazz.getInstanceFields();

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        fieldNames.sort(Comparator.naturalOrder());

        for (String fieldName : fieldNames) {
          String i2 = "  ".repeat(indent + 1);
          CandleField field = fields.get(fieldName);
          List<Class<?>> references =
              CandleUtils.resolveList(PythonUtils.extractClasses(field.getFullType(), false));
          reference(references);

          line = "self." + fieldName + ": " + getTypeString(field.getClazz(), field.getBasicType());

          boolean commented = false;

          if (ILLEGAL.contains(fieldName)) {
            commented = true;
          } else {
            // Check to see if there's a conflicting attribute.
            for (Class<?> c : clazz.getClazz().getClasses()) {
              if (c.getSimpleName().equals(fieldName)) {
                commented = true;
                break;
              }
            }
          }

          if (commented) {
            line = i2 + "# " + line;
          } else {
            line = i2 + line;
          }

          lines.add(line);
          if (constructors.size() != 1) {
            lines.add("");
          }
        }

        wroteFields = true;

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
            CandleUtils.resolveList(PythonUtils.extractClasses(method.getFullReturnType(), false)));
      } catch (Exception e) {
        // Subclass with generics = fun so ignore because good programmer. =)
      }

      line += ")";
      line += " -> " + getTypeString(method.getReturnType(), method.getBasicReturnType());
      line += ": ...";

      try {
        if (method.getLuaName().equals("getNearestBuilding")) {
          System.out.println(method.getFullReturnType());
          System.out.println(PythonUtils.extractClasses(method.getFullReturnType(), false));
          System.out.println(
              CandleUtils.resolveList(
                  PythonUtils.extractClasses(method.getFullReturnType(), false)));
        }
        reference(
            CandleUtils.resolveList(PythonUtils.extractClasses(method.getFullReturnType(), false)));
      } catch (Exception e) {
        e.printStackTrace(System.err);
      }

      if (methods.size() != 1) {
        lines.add(i + "@overload");
      }
      lines.add(line);
      if (methods.size() != 1) {
        lines.add("");
      }
    }

    return lines;
  }

  String getTypeString(@Nullable Class<?> clazz, @NotNull String typeName) {
    return getTypeString(clazz, typeName, 0);
  }

  String getTypeString(@Nullable Class<?> clazz, @NotNull String typeName, int inside) {
    typeName =
        typeName
            .replaceAll("\\? extends ", "")
            .replaceAll("\\? super ", "")
            .replaceAll("\\?", "Any");

    if (clazz != null) {
      if (clazz.isArray()) {
        reference(clazz.getComponentType());

        String name = clazz.getSimpleName();
        Class<?> eClazz = clazz.getEnclosingClass();
        while (eClazz != null) {
          name = eClazz.getSimpleName() + "." + name;
          eClazz = eClazz.getEnclosingClass();
        }

        return "list[" + getTypeString(clazz.getComponentType(), name, inside + 1) + "]";

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
      List<String> args = PythonUtils.commaSplit(typeName);
      StringBuilder first = new StringBuilder(typeName.substring(0, typeName.indexOf("<")));
      first.append("<");
      for (String arg : args) {
        first.append(getTypeString(null, arg, inside + 1)).append(", ");
      }
      typeName = first.substring(0, first.length() - 2) + ">";
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

    return typeName;
  }

  boolean shouldReference(@NotNull Class<?> clazz) {
    Package pkg = clazz.getPackage();

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
    imports.add("from java.lang.annotation import Annotation");

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

    StringBuilder pPath = new StringBuilder();
    String pName = pkg.getName();

    if (pName.contains(".")) {
      String[] split = pName.split("\\.");
      for (int index = 0; index < split.length - 1; index++) {
        pPath.append(split[index]).append("/");
      }
      pPath = new StringBuilder(pPath.substring(0, pPath.length() - 1));
      pName = split[split.length - 1];
    }

    File dir;
    if (pPath.isEmpty()) {
      dir = PythonTypingsRenderer.dir;
    } else {
      dir = new File(PythonTypingsRenderer.dir, pPath.toString());
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
}
