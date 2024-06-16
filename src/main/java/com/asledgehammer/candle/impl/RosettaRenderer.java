package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import com.asledgehammer.rosetta.Rosetta;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import zombie.Lua.LuaManager;

import java.io.File;
import java.util.*;

public class RosettaRenderer implements CandleRenderAdapter {

    CandleRenderer<CandleField> fieldRenderer =
            field -> {
                field.getDocs();
                return "";
            };

    CandleRenderer<CandleExecutableCluster<CandleConstructor>> constructorRenderer =
            cluster -> {
                for (CandleConstructor constructor : cluster.getExecutables()) {
                    constructor.getDocs();
                }
                return "";
            };

    CandleRenderer<CandleExecutableCluster<CandleMethod>> methodRenderer =
            cluster -> {
                for (CandleMethod method : cluster.getExecutables()) {
                    method.getDocs();
                }
                return "";
            };

    @Override
    public CandleRenderer<CandleClass> getClassRenderer() {
        return clazz -> {
            Map<String, CandleField> fields = clazz.getFields();
            Map<String, CandleExecutableCluster<CandleMethod>> methodsStatic = clazz.getStaticMethods();
            Map<String, CandleExecutableCluster<CandleMethod>> methods = clazz.getMethods();

            clazz.getDocs();

            if (!fields.isEmpty()) {
                List<String> keysSorted = new ArrayList<>(fields.keySet());
                keysSorted.sort(Comparator.naturalOrder());
                for (String fieldName : keysSorted) {
                    fieldRenderer.onRender(fields.get(fieldName));
                }
            }

            if (!methodsStatic.isEmpty()) {
                List<String> keysSorted = new ArrayList<>(methodsStatic.keySet());
                keysSorted.sort(Comparator.naturalOrder());
                for (String fieldName : keysSorted) {
                    methodRenderer.onRender(methodsStatic.get(fieldName));
                }
            }

            if (!methods.isEmpty()) {
                List<String> keysSorted = new ArrayList<>(methods.keySet());
                keysSorted.sort(Comparator.naturalOrder());
                for (String fieldName : keysSorted) {
                    methodRenderer.onRender(methods.get(fieldName));
                }
            }

            if (clazz.hasConstructors()) {
                CandleExecutableCluster<CandleConstructor> cluster = clazz.getConstructors();
                constructorRenderer.onRender(cluster);
            }

            return "";
        };
    }

    @Override
    public CandleRenderer<CandleAlias> getAliasRenderer() {
        return candleAlias -> "";
    }

    public void saveJSON(CandleGraph graph, File dir) {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Cannot create directory: " + dir.getPath());
        }

        for (CandleClass clazz : graph.classesSorted) {

            String namespace = clazz.getClazz().getPackageName().replaceAll("\\.", "-");
            String namespace2 = namespace.replaceAll("\\.", "-");

            File dirNamespace = new File(dir, namespace2);
            if (!dirNamespace.exists() && !dirNamespace.mkdirs()) {
                throw new RuntimeException("Cannot create directory: " + dir.getPath());
            }

            Map<String, Object> mapFile = new HashMap<>();
            mapFile.put("$schema", "https://raw.githubusercontent.com/asledgehammer/PZ-Rosetta-Schema/main/rosetta-schema.json");
            Map<String, Object> mapNamespaces = new HashMap<>();
            Map<String, Object> mapNamespace = new HashMap<>();
            mapNamespace.put(clazz.getLuaName(), clazz.getDocs().toJSON());
            mapNamespaces.put(namespace, mapNamespace);
            mapFile.put("namespaces", mapNamespaces);

            String json = gson.toJson(mapFile);

            File file = new File(dirNamespace, clazz.getLuaName() + ".json");
            System.out.println("RosettaRenderer: Writing: " + file.getPath() + "..");
            CandleGraph.write(file, json);
        }

        CandleClass candleGlobalObject = graph.classes.get(LuaManager.GlobalObject.class);
        Map<String, CandleExecutableCluster<CandleMethod>> methods =
                new HashMap<>(candleGlobalObject.getStaticMethods());
        methods.putAll(candleGlobalObject.getMethods());

        Map<String, Object> mapFile = new HashMap<>();
        mapFile.put("$schema", "https://raw.githubusercontent.com/asledgehammer/PZ-Rosetta-Schema/main/rosetta-schema.json");

        List<Map<String, Object>> listMethods = new ArrayList<>();

        int count = 0;
        for (CandleExecutableCluster<CandleMethod> cluster : methods.values()) {
            for (CandleMethod method : cluster.getExecutables()) {
                listMethods.add(method.getDocs().toJSON());
                count++;
            }
        }

        System.out.println("Count: " + count);

        mapFile.put("methods", listMethods);

        String json = gson.toJson(mapFile);

        File file = new File(dir, "global.json");
        System.out.println("RosettaRenderer: Writing: " + file.getPath() + "..");
        CandleGraph.write(file, json);
    }
}
