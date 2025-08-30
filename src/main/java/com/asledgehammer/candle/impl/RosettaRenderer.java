package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        mapPackage.put(clazz.getDocs().getName(), clazz.getDocs().toJSON());

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

            File file = new File(dirPackage, clazz.getDocs().getName() + ".json");
            System.out.println("RosettaRenderer: Writing: " + file.getPath() + "..");
            CandleGraph.write(file, json);
        }
    }

    public void saveYAML(@NotNull CandleGraph graph, @NotNull Path dir) {
        DumpSettings settings = DumpSettings.builder()
                .setDefaultFlowStyle(FlowStyle.BLOCK)
                .build();
        Dump yaml = new Dump(settings);


        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create directory: " + dir);
            }
        }

        for (CandleClass clazz : graph.classesSorted) {
            String packageName = clazz.getClazz().getPackageName();

            Path dirPackage = dir;
            for (String pathElement : packageName.split("\\.")) {
                dirPackage = dirPackage.resolve(pathElement);
            }

            if (!Files.exists(dirPackage)) {
                try {
                    Files.createDirectories(dirPackage);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot create directory: " + dir);
                }
            }

            String yml = yaml.dumpToString(classToMap(clazz));

            File file = dirPackage.resolve(clazz.getDocs().getName() + ".yml").toFile();
            System.out.println("RosettaRenderer: Writing: " + file.getPath() + "..");
            CandleGraph.write(file, yml);
        }
    }
}
