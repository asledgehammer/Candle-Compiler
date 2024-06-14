package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import com.asledgehammer.rosetta.*;

import java.util.*;

public class RosettaRenderer implements CandleRenderAdapter {

    public static Map<String, Object> jsonFields = new HashMap<>();

    String classNameLegalCurrent = "";

    CandleRenderer<CandleField> fieldRenderer =
            field -> {
                RosettaField yaml = field.getDocs();

                if(yaml == null) {
                    Map<String, Object> mapField = new HashMap<>();

                    // MODIFIERS
                    List<String> listModifiers = new ArrayList<>();
                    if(field.isPublic()) listModifiers.add("public");
                    else if(field.isProtected()) listModifiers.add("protected");
                    else if(field.isPrivate()) listModifiers.add("private");
                    if(field.isStatic()) listModifiers.add("static");
                    if(field.isFinal()) listModifiers.add("final");
                    String[] modifiers = new String[listModifiers.size()];
                    for(int index = 0; index < listModifiers.size(); index++) {
                        modifiers[index] = listModifiers.get(index);
                    }
                    mapField.put("modifiers", modifiers);

                    // TYPE
                    Map<String, Object> mapType = new HashMap<>();
                    mapType.put("basic", field.getBasicType());
                    mapType.put("full", field.getFullType());
                    mapType.put("nullable", true);
                    mapField.put("type", mapType);

                    jsonFields.put(field.getLuaName(), mapField);

                } else {
                    jsonFields.put(field.getLuaName(), yaml.toJSON());
                }

                return "";
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
                        if (pName.equals("true")) {
                            pName = "arg" + argOffset++;
                        }
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
                    for (int index = 1; index < constructors.size(); index++) {
                        CandleConstructor overload = constructors.get(index);
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

                        if (pName.equals("true")) {
                            pName = "arg" + argOffset++;
                        }
                        String pType = parameter.getJavaParameter().getType().getSimpleName();
                        builder.append("--- @param ").append(pName).append(' ').append(pType);

                        if (yaml != null && yaml.hasNotes()) {
                            builder.append(' ').append(yaml.getNotes().replaceAll("\\n", ""));
                        }

                        builder.append('\n');

                        paramBuilder.append(pName).append(", ");
                    }
                    paramBuilder.setLength(paramBuilder.length() - 2);
                }

                builder.append("--- @return ").append(first.getReturnType().getSimpleName());
                if (yamlFirst != null) {
                    RosettaReturns yamlReturn = yamlFirst.getReturns();
                    if (yamlReturn.hasNotes()) {
                        builder.append(' ').append(yamlReturn.getNotes().replaceAll("\\n", ""));
                    }
                }

                builder.append('\n');

                if (cluster.hasOverloads()) {
                    for (int index = 1; index < methods.size(); index++) {
                        CandleMethod overload = methods.get(index);
                        RosettaMethod yaml = overload.getDocs();

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
                        builder.append("): ");

                        builder.append(overload.getReturnType().getSimpleName());

                        if (yaml != null) {
                            RosettaReturns yamlReturn = yaml.getReturns();
                            if (yamlReturn.hasNotes()) {
                                builder.append(' ').append(yamlReturn.getNotes().replaceAll("\\n", ""));
                            }
                        }

                        builder.append('\n');
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

    @Override
    public CandleRenderer<CandleClass> getClassRenderer() {
        return candleClass -> {
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
            builder.append("--- @class ").append(className).append(superClazzName);

            RosettaClass yaml = candleClass.getDocs();

            if (yaml != null && yaml.hasNotes()) {
                builder.append(' ').append(yaml.getNotes().replaceAll("\\n", ""));
            }
            builder.append('\n');
            builder.append("--- @field public class any\n");

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