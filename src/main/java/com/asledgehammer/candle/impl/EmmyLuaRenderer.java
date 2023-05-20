package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EmmyLuaRenderer implements CandleRenderAdapter {

  String classNameLegalCurrent = "";

  CandleRenderer<CandleField> fieldRenderer =
      field ->
          "--- @field "
              + (field.isPublic() ? "public " : "")
              + field.getLuaName()
              + " "
              + field.getClazz().getSimpleName();

  CandleRenderer<CandleExecutableCluster<CandleConstructor>> constructorRenderer =
      cluster -> {
        CandleConstructor first = cluster.getFirst();

        StringBuilder builder = new StringBuilder();
        builder.append("--- @public\n");
        if (first.isStatic()) builder.append("--- @static\n");

        StringBuilder paramBuilder = new StringBuilder();
        if (first.hasParameters()) {
          List<CandleParameter> parameters = first.getParameters();
          for (CandleParameter parameter : parameters) {
            String pName = parameter.getLuaName();
            String pType = parameter.getJavaParameter().getType().getSimpleName();
            builder.append("--- @param ").append(pName).append(' ').append(pType).append('\n');
            paramBuilder.append(pName).append(", ");
          }
          paramBuilder.setLength(paramBuilder.length() - 2);
        }

        builder
            .append("--- @return ")
            .append(first.getExecutable().getDeclaringClass().getSimpleName())
            .append('\n');

        if (cluster.hasOverloads()) {
          List<CandleConstructor> overloads = cluster.getOverloads();
          for (CandleConstructor overload : overloads) {
            builder.append("--- @overload fun(");
            if (overload.hasParameters()) {
              List<CandleParameter> parameters = overload.getParameters();
              for (CandleParameter parameter : parameters) {
                builder
                    .append(parameter.getLuaName())
                    .append(": ")
                    .append(parameter.getJavaParameter().getType().getSimpleName())
                    .append(", ");
              }
              builder.setLength(builder.length() - 2);
            }
            builder.append(")\n");
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
        CandleMethod first = cluster.getFirst();

        StringBuilder builder = new StringBuilder();
        builder.append("--- @public\n");
        if (first.isStatic()) builder.append("--- @static\n");

        StringBuilder paramBuilder = new StringBuilder();
        if (first.hasParameters()) {
          List<CandleParameter> parameters = first.getParameters();
          for (CandleParameter parameter : parameters) {
            String pName = parameter.getLuaName();
            String pType = parameter.getJavaParameter().getType().getSimpleName();
            builder.append("--- @param ").append(pName).append(' ').append(pType).append('\n');
            paramBuilder.append(pName).append(", ");
          }
          paramBuilder.setLength(paramBuilder.length() - 2);
        }

        builder.append("--- @return ").append(first.getReturnType().getSimpleName()).append('\n');

        if (cluster.hasOverloads()) {
          List<CandleMethod> overloads = cluster.getOverloads();
          for (CandleMethod overload : overloads) {
            builder.append("--- @overload fun(");
            if (overload.hasParameters()) {
              List<CandleParameter> parameters = overload.getParameters();
              for (CandleParameter parameter : parameters) {
                builder
                    .append(parameter.getLuaName())
                    .append(": ")
                    .append(parameter.getJavaParameter().getType().getSimpleName())
                    .append(", ");
              }
              builder.setLength(builder.length() - 2);
            }
            builder.append(")\n");
          }
        }

        builder
            .append("function ")
            .append(classNameLegalCurrent)
            .append(first.isStatic() ? '.' : ':')
            .append(cluster.getLuaName())
            .append("(")
            .append(paramBuilder)
            .append(") end");

        String resultCode = builder.toString();
        cluster.setRenderedCode(resultCode);
        return resultCode;
      };

  CandleRenderer<CandleClass> classRenderer =
      candleClass -> {
        Map<String, CandleField> fields = candleClass.getFields();
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

        Class<?> parentClass = candleClass.getClazz().getSuperclass();
        String parentName = parentClass != null ? parentClass.getSimpleName() : "";
        String superClazzName =
            parentClass != null && !parentName.equals("Object") ? ": " + parentName : "";

        StringBuilder builder = new StringBuilder("--- @meta\n\n");
        builder.append("--- @class ").append(className).append(superClazzName).append('\n');

        Class<?> clazz = candleClass.getClazz();
        Class<?>[] interfazes = clazz.getInterfaces();
        for (Class<?> interfaze : interfazes) {
          builder.append("--- @implement ").append(interfaze.getSimpleName()).append('\n');
        }

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
            builder
                .append(methodRenderer.onRender(methods.get(fieldName)))
                .append('\n')
                .append('\n');
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

  CandleRenderer<CandleAlias> aliasRenderer =
      candleAlias -> "--- @class " + candleAlias.getLuaName();

  @Override
  public CandleRenderer<CandleClass> getClassRenderer() {
    return classRenderer;
  }

  @Override
  public CandleRenderer<CandleAlias> getAliasRenderer() {
    return aliasRenderer;
  }

  @Override
  public CandleRenderer<CandleExecutableCluster<CandleMethod>> getMethodRenderer() {
    return methodRenderer;
  }
}
