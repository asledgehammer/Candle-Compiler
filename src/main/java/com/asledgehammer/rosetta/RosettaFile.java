package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@SuppressWarnings("unchecked")
public class RosettaFile extends RosettaEntity {

    private final Map<String, RosettaPackage> packages = new HashMap<>();
    private final Map<String, RosettaMethodCluster> methods = new HashMap<>();
    private final Rosetta rosetta;

    RosettaFile(@NotNull Rosetta rosetta, @NotNull Map<String, Object> raw) {
        super(raw);

        this.rosetta = rosetta;

        if(!raw.containsKey("version") || !raw.get("version").equals("1.1")) {
            // TODO: this error is kind of useless, there needs to be an indication of what file
            throw new RuntimeException("File is not a Rosetta file or is not a supported version.");
        }

        if (!raw.containsKey("languages")) {
            return;
        }

        Map<String, Object> languages = (Map<String, Object>) raw.get("languages");

        if (!languages.containsKey("java")) {
            return;
        }

        Map<String, Object> java = (Map<String, Object>) languages.get("java");

        /* METHODS */
        if (java.containsKey("methods")) {
            List<Map<String, Object>> rawMethods = (List<Map<String, Object>>) java.get("methods");
            for (Map<String, Object> rawMethod : rawMethods) {
                RosettaMethod method = new RosettaMethod(rawMethod);
                String methodName = method.getName();

                RosettaMethodCluster cluster;
                if (methods.containsKey(methodName)) {
                    cluster = methods.get(methodName);
                } else {
                    cluster = new RosettaMethodCluster(methodName);
                    methods.put(methodName, cluster);
                }
                cluster.add(method);
            }
        }

        /* NAMESPACES */
        if (java.containsKey("packages")) {
            Map<String, Object> rawNamespaces = (Map<String, Object>) java.get("packages");
            for (String name : rawNamespaces.keySet()) {
                Map<String, Object> rawNamespace = (Map<String, Object>) rawNamespaces.get(name);
                RosettaPackage pkg = rosetta.getPackage(name);

                if (pkg == null) {
                    pkg = new RosettaPackage(name, rawNamespace);
                    rosetta.addPackage(pkg);
                } else {
                    pkg.parse(rawNamespace);
                }

                this.packages.put(name, pkg);
            }
        }
    }

    @Nullable
    public RosettaMethodCluster getMethodsByName(@NotNull String name) {
        return this.methods.get(name);
    }

    @Nullable
    public RosettaMethod getMethod(@NotNull String name, Class<?>... parameterTypes) {
        RosettaMethodCluster cluster = getMethodsByName(name);
        if (cluster == null) return null;
        return cluster.getWithParameters(parameterTypes);
    }

    @Nullable
    public RosettaMethod getMethod(@NotNull Method method) {
        String name = method.getName();
        Class<?>[] params = new Class<?>[method.getParameterCount()];
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            params[i] = parameter.getType();
        }
        return getMethod(name, params);
    }

    @NotNull
    public Map<String, RosettaMethodCluster> getMethods() {
        return this.methods;
    }

    @Override
    public String toString() {
        return "RosettaFile{" + "packages=" + packages + '}';
    }

    @NotNull
    public Map<String, RosettaPackage> getPackages() {
        return this.packages;
    }

    @Nullable
    public RosettaPackage getPackage(@NotNull String id) {
        return this.packages.get(id);
    }

    public Map<String, Object> toJSON() {
        Map<String, Object> mapFile = new HashMap<>();

        mapFile.put("$schema", "https://raw.githubusercontent.com/asledgehammer/PZ-Rosetta-Schema/main/rosetta-schema.json");

        // METHODS
        if (!this.methods.isEmpty()) {
            List<Map<String, Object>> listMethods = new ArrayList<>();
            List<String> listMethodNames = new ArrayList<>(this.methods.keySet());
            listMethodNames.sort(Comparator.naturalOrder());
            for (String methodName : listMethodNames) {
                for (RosettaMethod method : this.methods.get(methodName).getMethods()) {
                    listMethods.add(method.toJSON());
                }
            }
            mapFile.put("methods", listMethods);
        }

        // NAMESPACES
        if (!this.packages.isEmpty()) {
            Map<String, Object> mapNamespaces = new HashMap<>();

            List<String> listNamespaceNames = new ArrayList<>(this.packages.keySet());
            listNamespaceNames.sort(Comparator.naturalOrder());

            for (String namespaceName : listNamespaceNames) {
                mapNamespaces.put(namespaceName, this.packages.get(namespaceName).toJSON());
            }
            mapFile.put("namespaces", mapNamespaces);
        }

        return mapFile;
    }
}
