package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@SuppressWarnings("unchecked")
public class RosettaFile extends RosettaEntity {

    private final Map<String, RosettaNamespace> namespaces = new HashMap<>();
    private final Map<String, RosettaMethodCluster> methods = new HashMap<>();
    private final Rosetta rosetta;

    RosettaFile(@NotNull Rosetta rosetta, @NotNull Map<String, Object> raw) {
        super(raw);

        this.rosetta = rosetta;

        /* METHODS */
        if (raw.containsKey("methods")) {
            List<Map<String, Object>> rawMethods = (List<Map<String, Object>>) raw.get("methods");
            for (Map<String, Object> rawMethod : rawMethods) {
                RosettaMethod method = new RosettaMethod(rawMethod);
                String methodName = method.getName();
                if (methodName.equals("triggerEvent")) {
                    System.out.println(method.asJavaString("### "));
                }
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
        if (raw.containsKey("namespaces")) {
            Map<String, Object> rawNamespaces = (Map<String, Object>) raw.get("namespaces");
            for (String name : rawNamespaces.keySet()) {
                Map<String, Object> rawNamespace = (Map<String, Object>) rawNamespaces.get(name);
                RosettaNamespace namespace = rosetta.getNamespace(name);

                if (namespace == null) {
                    namespace = new RosettaNamespace(name, rawNamespace);
                    rosetta.addNamespace(namespace);
                } else {
                    namespace.parse(rawNamespace);
                }

                this.namespaces.put(name, namespace);
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
        return "RosettaFile{" + "namespaces=" + namespaces + '}';
    }

    @NotNull
    public Map<String, RosettaNamespace> getNamespaces() {
        return this.namespaces;
    }

    @Nullable
    public RosettaNamespace getNamespace(@NotNull String id) {
        return this.namespaces.get(id);
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
        if (!this.namespaces.isEmpty()) {
            Map<String, Object> mapNamespaces = new HashMap<>();

            List<String> listNamespaceNames = new ArrayList<>(this.namespaces.keySet());
            listNamespaceNames.sort(Comparator.naturalOrder());

            for (String namespaceName : listNamespaceNames) {
                mapNamespaces.put(namespaceName, this.namespaces.get(namespaceName).toJSON());
            }
            mapFile.put("namespaces", mapNamespaces);
        }

        return mapFile;
    }
}
