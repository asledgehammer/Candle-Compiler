package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class KnownTypeRenderer implements CandleRenderAdapter {

    public String file = "";
    String classNameCurrent = "";

    List<String> knownList = new ArrayList<>();

    CandleRenderer<CandleField> fieldRenderer =
            field -> {
                String type = field.getClazz().getSimpleName();
                if (type.equals("String")) {
                    type = "string";
                }
                String known = classNameCurrent + "." + field.getLuaName();
                if (knownList.contains(known)) return "";
                return "\"" + known + "\": \"" + type + "\"";
            };

    CandleRenderer<CandleExecutableCluster<CandleMethod>> methodRenderer =
            cluster -> {

                List<String> types = new ArrayList<>();
                for (CandleMethod me : cluster.getExecutables()) {
                    String rType = me.getReturnType().getSimpleName();
                    if (!types.contains(rType)) {
                        types.add(rType);
                    }
                }

                StringBuilder sTypes = new StringBuilder();
                for (String sType : types) {
                    if (sType.equals("String")) {
                        sType = "string";
                    }
                    sTypes.append(sType).append("|");
                }

                sTypes = new StringBuilder(sTypes.substring(0, sTypes.length() - 1));
                String known = classNameCurrent + "." + cluster.getLuaName() + "()";
                if (knownList.contains(known)) return "";

                return "\"" + known + "\": \"" + sTypes + "\"";
            };

    @Override
    public CandleRenderer<CandleClass> getClassRenderer() {
        return candleClass -> {
            String s = "";
            String i = "    ";
            Map<String, CandleField> fields = candleClass.getStaticFields();
            Map<String, CandleExecutableCluster<CandleMethod>> methodsStatic =
                    candleClass.getStaticMethods();
            Map<String, CandleExecutableCluster<CandleMethod>> methods = candleClass.getMethods();
            classNameCurrent = candleClass.getLuaName();

            List<String> fieldNames = new ArrayList<>(fields.keySet());
            fieldNames.sort(Comparator.naturalOrder());

            List<String> methodNames = new ArrayList<>(methods.keySet());
            methodNames.sort(Comparator.naturalOrder());

            List<String> staticMethodNames = new ArrayList<>(methodsStatic.keySet());
            staticMethodNames.sort(Comparator.naturalOrder());

            s += i + "\"" + classNameCurrent + "\": \"" + classNameCurrent + "\",\n";

            if (candleClass.hasConstructors()) {
                s += i + "\"" + classNameCurrent + ".new()\": \"" + classNameCurrent + "\",\n";
            }

            // Render static fields.
            for (String fieldName : fieldNames) {
                CandleField field = fields.get(fieldName);
                if (!field.isStatic()) continue;
                s += i + fieldRenderer.onRender(field) + ",\n";
            }

            // Render instance fields.
            for (String fieldName : fieldNames) {
                CandleField field = fields.get(fieldName);
                if (field.isStatic()) continue;
                s += i + fieldRenderer.onRender(field) + ",\n";
            }

            // Render instance methods.
            for (String methodName : methodNames) {
                CandleExecutableCluster<CandleMethod> method = methods.get(methodName);
                s += i + methodRenderer.onRender(method) + ",\n";
            }

            // Render static methods.
            for (String methodName : staticMethodNames) {
                CandleExecutableCluster<CandleMethod> method = methodsStatic.get(methodName);
                s += i + methodRenderer.onRender(method) + ",\n";
            }

            file += s;

            return s;
        };
    }

    @Override
    public CandleRenderer<CandleAlias> getAliasRenderer() {
        return candleAlias -> "";
    }
}
