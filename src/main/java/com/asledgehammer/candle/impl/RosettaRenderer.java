package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
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
            Map<String, CandleField> fields = clazz.getStaticFields();
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

    /**
     * Builds a map Rosetta representation of a class.
     * @param clazz The class to represent.
     * @return A Rosetta-formatted map describing the class.
     */
    private @NotNull Map<String, Object> classToMap(@NotNull CandleClass clazz) {
        String packageName = clazz.getClazz().getPackageName();

        // we are using a LinkedHashMap to preserve order of insertion
        // order of insertion doesn't matter for the maps that only store one element anyway
        Map<String, Object> mapFile = new LinkedHashMap<>();
        Map<String, Object> mapLanguages = new HashMap<>();
        Map<String, Object> mapJava = new HashMap<>();
        Map<String, Object> mapPackages = new HashMap<>();
        Map<String, Object> mapPackage = new HashMap<>();
        mapFile.put("version", "1.1");
        mapFile.put("languages", mapLanguages);
        mapLanguages.put("java", mapJava);
        mapJava.put("packages", mapPackages);
        mapPackages.put(packageName, mapPackage);
        mapPackage.put(clazz.getLuaName(), clazz.getDocs().toJSON());

        return mapFile;
    }

    /**
     * Builds a map Rosetta representation of the global namespace.
     * @param graph The graph to search for globals in.
     * @return A Rosetta-formatted map describing globals.
     */
    private @NotNull Map<String, Object> globalsToMap(@NotNull CandleGraph graph) {
        CandleClass candleGlobalObject = graph.classes.get(LuaManager.GlobalObject.class);
        Map<String, CandleExecutableCluster<CandleMethod>> methods =
                new HashMap<>(candleGlobalObject.getStaticMethods());
        methods.putAll(candleGlobalObject.getMethods());

        Map<String, Object> mapFile = new HashMap<>();

        List<Map<String, Object>> listMethods = new ArrayList<>();

        for (CandleExecutableCluster<CandleMethod> cluster : methods.values()) {
            for (CandleMethod method : cluster.getExecutables()) {
                listMethods.add(method.getDocs().toJSON());
            }
        }

        mapFile.put("methods", listMethods);

        return mapFile;
    }

    public void saveJSON(CandleGraph graph, File dir) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Cannot create directory: " + dir.getPath());
        }

        for (CandleClass clazz : graph.classesSorted) {
            String packageName = clazz.getClazz().getPackageName();

            File dirPackage = new File(dir, packageName);
            if (!dirPackage.exists() && !dirPackage.mkdirs()) {
                throw new RuntimeException("Cannot create directory: " + dir.getPath());
            }

            String json = gson.toJson(classToMap(clazz));

            File file = new File(dirPackage, clazz.getLuaName() + ".json");
            System.out.println("RosettaRenderer: Writing: " + file.getPath() + "..");
            CandleGraph.write(file, json);
        }

        String json = gson.toJson(globalsToMap(graph));

        File file = new File(dir, "global.json");
        System.out.println("RosettaRenderer: Writing: " + file.getPath() + "..");
        CandleGraph.write(file, json);
    }

    public void saveYAML(@NotNull CandleGraph graph, @NotNull File dir) {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);

        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Cannot create directory: " + dir.getPath());
        }

        for (CandleClass clazz : graph.classesSorted) {
            String packageName = clazz.getClazz().getPackageName();

            File dirPackage = new File(dir, packageName);
            if (!dirPackage.exists() && !dirPackage.mkdirs()) {
                throw new RuntimeException("Cannot create directory: " + dir.getPath());
            }

            String yml = yaml.dump(classToMap(clazz));

            File file = new File(dirPackage, clazz.getLuaName() + ".yml");
            System.out.println("RosettaRenderer: Writing: " + file.getPath() + "..");
            CandleGraph.write(file, yml);
        }

        String yml = yaml.dump(globalsToMap(graph));

        File file = new File(dir, "global.yml");
        System.out.println("RosettaRenderer: Writing: " + file.getPath() + "..");
        CandleGraph.write(file, yml);
    }
}
