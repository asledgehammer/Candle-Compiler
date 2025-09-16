package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import com.asledgehammer.rosetta.*;

import java.util.*;

@SuppressWarnings("SameParameterValue")
public class TypescriptRenderer implements CandleRenderAdapter {

  private static boolean wrapNamespace = false;
  private static boolean wrapFile = false;

  public static String wrapAsTSFile(String text) {
    String s = "";
    s += "/** @noSelfInFile */\n";
    s += "declare module '@asledgehammer/pipewrench'";
    if (!text.isEmpty()) {

      String[] split = text.split("\\R");
      String join = "";
      for (String next : split) {
        if (join.isEmpty()) {
          join = "    " + next;
        } else {
          join += "\n    " + next;
        }
      }

      return s + " {\n\n" + join + "\n}";
    }
    return s + " {}\n";
  }

  public static String wrapAsTSNamespace(String namespace, String text) {
    final String s = "export namespace " + namespace;
    if (!text.isEmpty()) {
      String[] split = text.split("\\R");
      String join = "";
      for (String next : split) {
        if (join.isEmpty()) {
          join = "    " + next;
        } else {
          join += "\n    " + next;
        }
      }
      return s + " {\n\n" + join + "\n}";
    }
    return s + " {}\n";
  }

  private static String tsType(String type, boolean optional) {
    String result = type;
    switch (type) {
      case "String":
        {
          result = "string";
          break;
        }
      case "KahluaTable":
        {
          result = "any";
          break;
        }
      default:
        {
          break;
        }
    }
    if (optional) {
      result += " | null";
    }
    return result;
  }

  private static String applyTSDocumentation(List<String> ds, String s, int indent) {
    final String i = " ".repeat(indent * 4);
    if (!ds.isEmpty()) {
      if (ds.size() == 1) {
        s += i + "/** " + ds.get(0) + " */\n";
      } else {
        s += i + "/**\n";
        final StringBuilder sBuilder = new StringBuilder(s);
        for (String next : ds) {
          sBuilder.append(i).append(" * ").append(next).append('\n');
        }
        s = sBuilder.toString();
        s = s.substring(0, s.length() - 1);
        s += '\n' + i + " */\n";
      }
    }
    return s;
  }

  private static List<String> _line(String line, int length) {
    final String[] split = line.split("\\s+");
    final List<String> result = new ArrayList<>();
    StringBuilder s = new StringBuilder(split[0]);
    for (int i = 1; i < split.length; i++) {
      String word = split[i];
      if (s.length() + word.length() + 1 <= length) {
        s.append(' ').append(word);
      } else {
        result.add(s.toString());
        s = new StringBuilder(word);
      }
    }
    if (!s.isEmpty()) {
      result.add(s.toString());
    }
    return result;
  }

  private static List<String> paginateNotes(String notes, int length) {
    List<String> res = new ArrayList<>();
    String[] lines = notes.split("\\R");
    for (String line : lines) {
      res.addAll(_line(line, length));
    }
    return res;
  }

  private static String javaFieldToTS(CandleField field, int indent, int notesLength) {

    if (!field.isPublic()) return "";

    final String i = " ".repeat(indent * 4);
    String s = "";

    /* Documentation */
    List<String> ds = new ArrayList<>();
    if (field.getDocs().isDeprecated()) ds.add("@deprecated");

    RosettaField docs = field.getDocs();

    String notes = docs.getNotes();
    if (notes != null && !notes.isEmpty()) {
      if (!ds.isEmpty()) ds.add("");
      ds.addAll(paginateNotes(notes, notesLength));
    }

    s = applyTSDocumentation(ds, s, indent);
    s += i;

    /* Definition-line */
    if (field.isStatic()) s += "static ";
    if (field.isFinal()) s += "readonly ";
    s += field.getLuaName() + ": " + tsType(field.getBasicType(), field.isNullable()) + ';';

    // Format documented variables as spaced for better legibility.
    if (!ds.isEmpty()) s += '\n';

    return s;
  }

  private static String javaConstructorToTS(CandleConstructor con, int indent, int notesLength) {

    if (!con.isPublic()) return "";

    String i = " ".repeat(indent * 4);
    List<String> ds = javaConstructorDocumentation(con, notesLength);

    String ps = "";
    List<CandleParameter> parameters = con.getParameters();
    if (!parameters.isEmpty()) {
      ps += "(";
      for (CandleParameter parameter : parameters) {
        ps +=
            parameter.getLuaName()
                + ": "
                + tsType(parameter.getBasicType(), parameter.isNullable())
                + ", ";
      }
      ps = ps.substring(0, ps.length() - 2) + ')';
    } else {
      ps = "()";
    }

    String fs = i + "constructor" + ps + ';';
    if (fs.length() > notesLength) {
      fs = i;
      fs += "constructor(\n";
      for (CandleParameter parameter : parameters) {
        fs +=
            i
                + "    "
                + parameter.getLuaName()
                + ": "
                + tsType(parameter.getBasicType(), parameter.isNullable())
                + ", \n";
      }
      fs += i + ");";
    }

    return applyTSDocumentation(ds, "", indent) + fs + '\n';
  }

  private static String javaConstructorsToTS(
      CandleExecutableCluster<CandleConstructor> cluster, int indent, int notesLength) {
    String i = " ".repeat(indent * 4);
    String s = "";

    List<CandleConstructor> cons = new ArrayList<>();
    for (CandleConstructor c : cluster.getExecutables()) {
      if (!c.isPublic()) continue;
      cons.add(c);
    }

    if (!cons.isEmpty()) {
      cons.sort(
          (a, b) -> {

            // Smaller param count = first.
            int apl = a.getParameterCount();
            int bpl = b.getParameterCount();
            int compare = apl - bpl;

            // If same count, compare type strings. a < b.
            if (compare == 0) {
              for (int index = 0; index < apl; index++) {
                CandleParameter ap = a.getParameters().get(index);
                CandleParameter bp = b.getParameters().get(index);
                compare = ap.getBasicType().compareTo(bp.getBasicType());
                if (compare != 0) break;
              }
            }

            return compare;
          });

      for (CandleConstructor con : cons) {
        if (!con.isPublic()) continue;
        s += javaConstructorToTS(con, indent, notesLength) + "\n\n";
      }

      // Remove trailing new-line.
      s = s.substring(0, s.length() - 3);
    }

    return s;
  }

  private static List<String> javaConstructorDocumentation(CandleConstructor con, int notesLength) {
    List<String> ds = new ArrayList<>();

    RosettaConstructor docs = con.getDocs();

    /* (Annotations) */
    if (docs.isDeprecated()) ds.add("@deprecated");

    /* (Notes) */
    String notes = docs.getNotes();
    if (notes != null && !notes.isEmpty()) {
      if (!ds.isEmpty()) ds.add("");
      List<String> nnotes = paginateNotes(notes, notesLength);
      ds.addAll(nnotes);
    }

    /* (Parameters) */
    List<CandleParameter> parameters = con.getParameters();
    if (parameters != null && !parameters.isEmpty()) {
      if (!ds.isEmpty()) ds.add("");
      for (CandleParameter param : parameters) {
        String pnotes = param.getDocs().getNotes();
        if (pnotes != null && !pnotes.isEmpty()) {
          List<String> ppnotes =
              paginateNotes("@param " + param.getLuaName() + " " + pnotes, notesLength);
          ds.addAll(ppnotes);
        } else {
          ds.add("@param " + param.getLuaName());
        }
      }
    }

    return ds;
  }

  private static String javaMethodClusterToTS(
      CandleExecutableCluster<CandleMethod> cluster, int indent, int notesLength) {
    List<CandleMethod> executables = cluster.getExecutables();
    if (executables.size() == 1) {
      return javaMethodToTS(executables.get(0), indent, notesLength);
    }

    String s = "";

    List<CandleMethod> methods = new ArrayList<>();
    for (CandleMethod m : executables) {
      if (!m.isPublic()) continue;
      methods.add(m);
    }

    if (!methods.isEmpty()) {
      methods.sort(
          (a, b) -> {
            // Smaller param count = first.
            int apl = a.getParameterCount();
            int bpl = b.getParameterCount();
            int compare = apl - bpl;

            // If same count, compare type strings. a < b.
            if (compare == 0) {
              for (int index = 0; index < apl; index++) {
                CandleParameter ap = a.getParameters().get(index);
                CandleParameter bp = b.getParameters().get(index);
                compare = ap.getBasicType().compareTo(bp.getBasicType());
                if (compare != 0) break;
              }
            }

            return compare;
          });

      for (CandleMethod method : executables) {
        if (!method.isPublic()) continue;
        s += javaMethodToTS(method, indent, notesLength) + '\n';
      }

      // Remove trailing new-line.
      s = s.substring(0, s.length() - 1);
    }

    return s;
  }

  private static List<String> javaMethodDocumentation(
      CandleMethod method, int notesLength, boolean overload) {
    List<String> ds = new ArrayList<>();

    RosettaMethod docs = method.getDocs();

    /* (Annotations) */
    if (overload) ds.add("@overload");
    if (method.isDeprecated()) ds.add("@deprecated");

    /* (Notes) */
    String notes = docs.getNotes();
    if (notes != null && !notes.isEmpty()) {
      if (!ds.isEmpty()) ds.add("");
      ds.addAll(paginateNotes(notes, notesLength));
    }

    /* (Parameters) */
    List<CandleParameter> parameters = method.getParameters();
    if (parameters != null && !parameters.isEmpty()) {
      if (!ds.isEmpty()) ds.add("");
      for (CandleParameter param : parameters) {
        String pnotes = param.getDocs().getNotes();
        if (pnotes != null && !pnotes.isEmpty()) {
          ds.addAll(paginateNotes("@param " + param.getLuaName() + " " + pnotes, notesLength));
        } else {
          ds.add("@param " + param.getLuaName());
        }
      }
    }

    /* (Returns) */
    RosettaReturn returns = docs.getReturn();
    String rnotes = returns.getNotes();
    if (rnotes != null && !rnotes.isEmpty()) {
      if (!ds.isEmpty()) ds.add("");
      ds.addAll(paginateNotes("@returns " + rnotes, notesLength));
    }

    return ds;
  }

  private static String javaMethodToTS(CandleMethod method, int indent, int notesLength) {

    if (!method.isPublic()) return "";

    String i = " ".repeat(indent * 4);
    List<String> ds = javaMethodDocumentation(method, notesLength, false);

    String ps = "";
    if (method.getParameterCount() != 0) {
      ps += '(';
      for (CandleParameter parameter : method.getParameters()) {
        ps +=
            parameter.getLuaName()
                + ": "
                + tsType(parameter.getBasicType(), parameter.isNullable())
                + ", ";
      }
      ps = ps.substring(0, ps.length() - 2) + ')';
    } else {
      ps = "()";
    }

    String rs = tsType(method.getBasicReturnType(), method.isReturnTypeNullable());

    String fs = i;
    if (method.isStatic()) fs += "static ";
    if (method.isFinal()) fs += "readonly ";

    String mName = method.getLuaName();
    if (mName.equals("__toString__")) mName = "toString";

    fs += mName + ps + ": " + rs + ";\n";
    if (fs.length() > notesLength) {
      fs = i;
      if (method.isStatic()) fs += "static ";
      if (method.isFinal()) fs += "readonly ";
      fs += mName + "(\n";
      for (CandleParameter parameter : method.getParameters()) {
        fs +=
            i
                + "    "
                + parameter.getLuaName()
                + ": "
                + tsType(parameter.getBasicType(), parameter.isNullable())
                + ", \n";
      }
      fs += i + "): " + rs + '\n';
    }

    return applyTSDocumentation(ds, "", indent) + fs;
  }

  @Override
  public CandleRenderer<CandleClass> getClassRenderer() {
    return clazz -> {
      //      System.out.println("Rendering class: " + clazz.getLuaName());
      String s = "";

      List<String> staticFieldNames = new ArrayList<>(clazz.getStaticFields().keySet());
      staticFieldNames.sort(Comparator.naturalOrder());

      List<String> fieldNames = new ArrayList<>(clazz.getInstanceFields().keySet());
      fieldNames.sort(Comparator.naturalOrder());

      List<String> staticMethodNames = new ArrayList<>(clazz.getStaticMethods().keySet());
      staticMethodNames.sort(Comparator.naturalOrder());

      List<String> methodNames = new ArrayList<>(clazz.getMethods().keySet());
      methodNames.sort(Comparator.naturalOrder());

      final Map<String, CandleField> instanceFields = clazz.getInstanceFields();
      final Map<String, CandleField> staticFields = clazz.getStaticFields();
      final Map<String, CandleExecutableCluster<CandleMethod>> methods = clazz.getMethods();
      final Map<String, CandleExecutableCluster<CandleMethod>> staticMethods =
          clazz.getStaticMethods();
      final CandleExecutableCluster<CandleConstructor> constructors = clazz.getConstructors();

      int notesLength = 96;
      //      if (wrapFile) {
      notesLength -= 4;
      //      }
      //      if (wrapNamespace) {
      notesLength -= 4;
      //      }

      final RosettaClass clazzDocs = clazz.getDocs();
      final String clazzNotes = clazzDocs.getNotes();

      List<String> ds = new ArrayList<>();
      ds.add("@customConstructor " + clazz.getLuaName() + ".new()");
      ds.add("");
      ds.add("Class: " + clazz.getClazz().getName());
      if (clazzNotes != null) {
        ds.add("");
        List<String> lines = paginateNotes(clazzNotes, notesLength);
        ds.addAll(lines);
      }

      s = applyTSDocumentation(ds, s, 0);

      s += "export class " + clazz.getLuaName() + ' ';

      String i = "    ";
      String is = "";
      String temp = "";

      /* STATIC FIELDS */
      if (!staticFieldNames.isEmpty()) {
        temp = "";
        for (String fieldName : staticFieldNames) {
          CandleField field = staticFields.get(fieldName);
          // Only 'public static final' fields are exposed in Kahlua.
          if (!field.isPublic() || !field.isFinal()) {
            continue;
          }

          temp += javaFieldToTS(field, 1, notesLength) + '\n';
        }
        if (!temp.isEmpty()) {
          is += i + "/* ------------------------------------ */\n";
          is += i + "/* ---------- STATIC FIELDS ----------- */\n";
          is += i + "/* ------------------------------------ */\n";
          is += '\n';
          is += temp;
        }
      }

      /* CONSTRUCTORS */
      if (constructors != null && !constructors.getExecutables().isEmpty()) {
        temp = javaConstructorsToTS(clazz.getConstructors(), 1, notesLength) + '\n';
        if (!temp.isEmpty()) {
          if (!is.isEmpty()) is += '\n';
          is += i + "/* ------------------------------------ */\n";
          is += i + "/* ----------- CONSTRUCTORS ----------- */\n";
          is += i + "/* ------------------------------------ */\n";
          is += '\n';
          is += temp;
        }
      }

      /* INSTANCE METHODS */
      if (!methods.isEmpty()) {
        temp = "";
        for (String methodName : methodNames) {
          CandleExecutableCluster<CandleMethod> cluster = methods.get(methodName);
          temp += javaMethodClusterToTS(cluster, 1, notesLength) + '\n';
        }
        if (!temp.isEmpty()) {
          if (!is.isEmpty()) is += '\n';
          is += i + "/* ------------------------------------ */\n";
          is += i + "/* ------------- METHODS -------------- */\n";
          is += i + "/* ------------------------------------ */\n";
          is += '\n';
          is += temp;
        }
      }

      /* STATIC METHODS */
      if (!staticMethods.isEmpty()) {
        temp = "";
        for (String methodName : staticMethodNames) {
          CandleExecutableCluster<CandleMethod> cluster = staticMethods.get(methodName);
          temp += javaMethodClusterToTS(cluster, 1, notesLength) + '\n';
        }
        if (!temp.isEmpty()) {
          if (!is.isEmpty()) is += '\n';
          is += i + "/* ------------------------------------ */\n";
          is += i + "/* ---------- STATIC METHODS ---------- */\n";
          is += i + "/* ------------------------------------ */\n";
          is += '\n';
          is += temp;
        }
      }

      if (!is.isEmpty()) {
        s += "{\n\n" + is + '}';
      } else {
        s += "{}\n";
      }

      if (wrapNamespace) s = wrapAsTSNamespace(clazz.getClazz().getPackage().getName(), s);
      if (wrapFile) return wrapAsTSFile(s);
      return s;
    };
  }

  @Override
  public CandleRenderer<CandleAlias> getAliasRenderer() {
    return candleAlias -> {
      System.out.println("Rendering alias: " + candleAlias.getLuaName());
      String s = "";
      return s;
    };
  }
}
