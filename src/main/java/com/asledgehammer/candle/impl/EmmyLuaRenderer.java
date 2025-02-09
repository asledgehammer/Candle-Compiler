package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import com.asledgehammer.rosetta.*;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaArray;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaClosure;

import java.util.*;

import static java.util.Map.entry;

public class EmmyLuaRenderer implements CandleRenderAdapter {

  String classNameLegalCurrent = "";

  static Map<Class<?>, String> luaTypeNameMap = Map.ofEntries(
    entry(float.class, "number"),
    entry(Float.class, "number"),
    entry(double.class, "number"),
    entry(Double.class, "number"),
    entry(byte.class, "integer"),
    entry(Byte.class, "integer"),
    // not representing short as integer because there are bugs relating to it, so knowing its java type is important
    entry(int.class, "integer"),
    entry(Integer.class, "integer"),
    entry(long.class, "integer"),
    entry(Long.class, "integer"),
    entry(KahluaTableImpl.class, "table"),
    entry(KahluaTable.class, "table"),
    entry(KahluaArray.class, "table"),
    entry(LuaClosure.class, "function"),
    entry(String.class, "string"),
    entry(char.class, "string"),
    entry(Character.class, "string"),
    entry(Object.class, "any"),
    entry(void.class, "nil"),
    entry(Void.class, "nil"),
    entry(Boolean.class, "boolean")
  );

  static HashSet<String> illegalIdentifiers = new HashSet<>(
    Arrays.stream(new String[] {
      "and",
      "break",
      "do",
      "else",
      "elseif",
      "end",
      "false",
      "for",
      "function",
      "if",
      "in",
      "local",
      "nil",
      "not",
      "or",
      "repeat",
      "return",
      "then",
      "true",
      "until",
      "while"
    }).toList());

  static String getTypeLuaName(Class<?> clazz) {
    return luaTypeNameMap.getOrDefault(clazz, clazz.getSimpleName());
  }

  CandleRenderer<CandleField> fieldRenderer =
      field -> {
        RosettaField yaml = field.getDocs();

        String f =
            "--- @field "
                + (field.isPublic() ? "public " : "")
                + field.getLuaName()
                + " "
                + getTypeLuaName(field.getClazz());

        if (yaml != null && yaml.hasNotes()) f += ' ' + yaml.getNotes().replaceAll("\\n", "");

        return f;
      };

  CandleRenderer<CandleExecutableCluster<CandleConstructor>> constructorRenderer =
      cluster -> {
        List<CandleConstructor> constructors = cluster.getExecutables();
        CandleConstructor first = constructors.get(0);

        RosettaConstructor yamlFirst = first.getDocs();

        byte argOffset = 1;

        StringBuilder builder = new StringBuilder();
        builder.append("--- @public\n");
        if (first.isStatic()) builder.append("--- @static\n");

        if (yamlFirst != null) {
          if (yamlFirst.hasNotes()) {
            builder.append("---\n");
            List<String> lines = paginate(yamlFirst.getNotes().replaceAll("\\n", ""), 80);
            for (String line : lines) {
              builder.append("--- ").append(line).append('\n');
            }
            builder.append("---\n");
          }
        }

        StringBuilder paramBuilder = new StringBuilder();
        if (first.hasParameters()) {
          List<CandleParameter> parameters = first.getParameters();
          for (CandleParameter parameter : parameters) {
            String pName = parameter.getLuaName();
            if (illegalIdentifiers.contains(pName)) {
              pName = "arg" + argOffset++;
            }
            String pType = getTypeLuaName(parameter.getJavaParameter().getType());
            builder.append("--- @param ").append(pName).append(' ').append(pType).append('\n');
            paramBuilder.append(pName).append(", ");
          }
          paramBuilder.setLength(paramBuilder.length() - 2);
        }

        String clazzName = first.getExecutable().getDeclaringClass().getSimpleName();

        builder.append("--- @return ").append(clazzName).append('\n');

        if (cluster.hasOverloads()) {
          for (int index = 1; index < constructors.size(); index++) {
            CandleConstructor overload = constructors.get(index);
            builder.append("--- @overload fun(");
            if (overload.hasParameters()) {
              List<CandleParameter> parameters = overload.getParameters();
              for (CandleParameter parameter : parameters) {
                builder
                    .append(parameter.getLuaName())
                    .append(": ")
                    .append(getTypeLuaName(parameter.getJavaParameter().getType()))
                    .append(", ");
              }
              builder.setLength(builder.length() - 2);
            }
            builder.append("): ");
            builder.append(classNameLegalCurrent).append('\n');
          }
        }

        builder
            .append("function ")
            .append(classNameLegalCurrent)
            .append(".new(")
            .append(paramBuilder)
            .append(") end");

        return builder.toString();
      };

  CandleRenderer<CandleExecutableCluster<CandleMethod>> methodRenderer =
      cluster -> {
        List<CandleMethod> methods = cluster.getExecutables();
        CandleMethod first = methods.get(0);
        RosettaMethod yamlFirst = first.getDocs();

        byte argOffset = 1;

        StringBuilder builder = new StringBuilder();
        builder.append("--- @public\n");
        if (first.isStatic()) builder.append("--- @static\n");

        if (yamlFirst != null) {
          if (yamlFirst.hasNotes()) {
            builder.append("---\n");
            List<String> lines = paginate(yamlFirst.getNotes().replaceAll("\\n", ""), 80);
            for (String line : lines) {
              builder.append("--- ").append(line).append('\n');
            }
            builder.append("---\n");
          }
        }

        StringBuilder paramBuilder = new StringBuilder();
        if (first.hasParameters()) {
          List<CandleParameter> parameters = first.getParameters();
          for (CandleParameter parameter : parameters) {
            String pName = parameter.getLuaName();
            RosettaParameter yaml = parameter.getDocs();

            if (illegalIdentifiers.contains(pName)) {
              pName = "arg" + argOffset++;
            }
            String pType = getTypeLuaName(parameter.getJavaParameter().getType());
            builder.append("--- @param ").append(pName).append(' ').append(pType);

            if (yaml != null && yaml.hasNotes()) {
              builder.append(' ').append(yaml.getNotes().replaceAll("\\n", ""));
            }

            builder.append('\n');

            paramBuilder.append(pName).append(", ");
          }
          paramBuilder.setLength(paramBuilder.length() - 2);
        }

        builder.append("--- @return ").append(getTypeLuaName(first.getReturnType()));
        if (yamlFirst != null) {
          RosettaReturns yamlReturn = yamlFirst.getReturns();
          if (yamlReturn.hasNotes()) {
            builder.append(' ').append(yamlReturn.getNotes().replaceAll("\\n", ""));
          }
        }

        builder.append('\n');

        boolean deprecated = first.isDeprecated();

        if (cluster.hasOverloads()) {
          for (int index = 1; index < methods.size(); index++) {
            CandleMethod overload = methods.get(index);
            RosettaMethod yaml = overload.getDocs();

            // all overloads must be deprecated to mark the function as deprecated
            deprecated = deprecated && overload.isDeprecated();

            boolean isStatic = overload.isStatic();

            builder.append("--- @overload fun(");

            boolean hasParams = false;
            if (!isStatic) {
              builder.append("self: ").append(classNameLegalCurrent).append(", ");
              hasParams = true;
            }

            if (overload.hasParameters()) {
              hasParams = true;
              List<CandleParameter> parameters = overload.getParameters();
              for (CandleParameter parameter : parameters) {
                builder
                    .append(parameter.getLuaName())
                    .append(": ")
                    .append(getTypeLuaName(parameter.getJavaParameter().getType()))
                    .append(", ");
              }
            }
            if (hasParams) {
              builder.setLength(builder.length() - 2);
            }
            builder.append("): ");

            builder.append(getTypeLuaName(overload.getReturnType()));

            if (yaml != null) {
              RosettaReturns yamlReturn = yaml.getReturns();
              if (yamlReturn.hasNotes()) {
                builder.append(' ').append(yamlReturn.getNotes().replaceAll("\\n", ""));
              }
            }

            builder.append('\n');
          }
        }

        if (deprecated) {
            builder.append("--- @deprecated\n");
        }

        String methodName = cluster.getLuaName();
        if (illegalIdentifiers.contains(methodName)) {
          builder
                  .append(classNameLegalCurrent)
                  .append("[\"")
                  .append(methodName)
                  .append("\"] = function(")
                  .append(first.isStatic() ? "" : "self, ")
                  .append(paramBuilder)
                  .append(") end");
        } else {
          builder
                  .append("function ")
                  .append(classNameLegalCurrent)
                  .append(first.isStatic() ? '.' : ':')
                  .append(methodName)
                  .append("(")
                  .append(paramBuilder)
                  .append(") end");
        }

        String resultCode = builder.toString();
        cluster.setRenderedCode(resultCode);
        return resultCode;
      };

  @Override
  public CandleRenderer<CandleClass> getClassRenderer() {
    return candleClass -> {
      Map<String, CandleField> fields = candleClass.getStaticFields();
      Map<String, CandleExecutableCluster<CandleMethod>> methodsStatic =
          candleClass.getStaticMethods();
      Map<String, CandleExecutableCluster<CandleMethod>> methods = candleClass.getMethods();

      boolean alt = false;
      String className = candleClass.getLuaName();
      String classNameLegal = className;
      if (className.contains("$")) {
        classNameLegal = "_G['" + className + "']";
        alt = true;
      }

      classNameLegalCurrent = classNameLegal;

      Class<?> clazz = candleClass.getClazz();
      Class<?> parentClass = clazz.getSuperclass();
      String parentName = parentClass != null ? parentClass.getSimpleName() : "";
      String supersString =
          parentClass != null && !parentName.equals("Object") ? parentName : "";
      Class<?>[] interfazes = clazz.getInterfaces();
      if (interfazes.length > 0) {
          supersString += (supersString.isEmpty() ? "" : ", ") + interfazes[0].getSimpleName();
          for (int i = 1; i < interfazes.length; i++) {
              supersString += ", " + interfazes[i].getSimpleName();
          }
      }
      if (!supersString.isEmpty()) {
          supersString = ": " + supersString;
      }

      StringBuilder builder = new StringBuilder("--- @meta _\n\n");
      builder.append("--- @class ").append(className).append(supersString);

      RosettaClass yaml = candleClass.getDocs();

      if (yaml != null && yaml.hasNotes()) {
        builder.append(' ').append(yaml.getNotes().replaceAll("\\n", ""));
      }
      builder.append('\n');
      builder.append("--- @field public class any\n");

      if (!fields.isEmpty()) {
        List<String> keysSorted = new ArrayList<>(fields.keySet());
        keysSorted.sort(Comparator.naturalOrder());
        for (String fieldName : keysSorted) {
          builder.append(fieldRenderer.onRender(fields.get(fieldName))).append('\n');
        }
      }

      builder.append(classNameLegal).append(" = {};").append('\n');
      builder.append('\n');

      if (alt) {
        builder.append("local temp = ").append(classNameLegal).append(";\n");
      }

      if (!methodsStatic.isEmpty()) {
        builder.append("------------------------------------\n");
        builder.append("---------- STATIC METHODS ----------\n");
        builder.append("------------------------------------\n\n");
        List<String> keysSorted = new ArrayList<>(methodsStatic.keySet());
        keysSorted.sort(Comparator.naturalOrder());
        for (String fieldName : keysSorted) {
          builder
              .append(methodRenderer.onRender(methodsStatic.get(fieldName)))
              .append('\n')
              .append('\n');
        }
        builder.append('\n');
      }

      if (!methods.isEmpty()) {
        builder.append("------------------------------------\n");
        builder.append("------------- METHODS --------------\n");
        builder.append("------------------------------------\n\n");
        List<String> keysSorted = new ArrayList<>(methods.keySet());
        keysSorted.sort(Comparator.naturalOrder());
        for (String fieldName : keysSorted) {
          builder.append(methodRenderer.onRender(methods.get(fieldName))).append('\n').append('\n');
        }
        builder.append('\n');
      }

      if (candleClass.hasConstructors()) {
        builder.append("------------------------------------\n");
        builder.append("----------- CONSTRUCTOR ------------\n");
        builder.append("------------------------------------\n\n");

        CandleExecutableCluster<CandleConstructor> cluster = candleClass.getConstructors();
        builder.append(constructorRenderer.onRender(cluster));
        builder.append('\n');
      }

      return builder.toString();
    };
  }

  @Override
  public CandleRenderer<CandleAlias> getAliasRenderer() {
    return candleAlias -> "--- @class " + candleAlias.getLuaName();
  }

  private static List<String> paginate(String s, int lineLength) {

    List<String> lines = new ArrayList<>();
    StringBuilder current = new StringBuilder();

    for (String word : s.split(" ")) {
      if ((current + " " + word).length() <= lineLength) {
        current.append(" ").append(word);
        continue;
      }
      lines.add(current.toString());
      current = new StringBuilder();
    }
    if (!current.isEmpty()) {
      lines.add(current.toString());
    }

    return lines;
  }
}
