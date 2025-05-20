package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@SuppressWarnings({"unchecked", "unused"})
public class RosettaClass extends RosettaEntity {
    private final Map<String, RosettaMethodCluster> methods = new HashMap<>();
    private final Map<String, RosettaField> fields = new HashMap<>();
    private final List<RosettaConstructor> constructors = new ArrayList<>();

    private final String[] modifiers;
    private final String name;
    private final String __extends;
    private final String javaType;
    private final String notes;
    private final boolean deprecated;

    public RosettaClass(@NotNull String name, @NotNull Map<String, Object> raw) {
        super(raw);

        /* CLASS PROPERTIES */
        this.name = RosettaUtils.formatName(name);
        this.__extends = readString("extends");
        this.modifiers = this.readModifiers();
        this.deprecated = readBoolean("deprecated") != null;
        this.javaType = readRequiredString("javaType");
        this.notes = readNotes();

        /* FIELDS */
        if (raw.containsKey("fields")) {
            Map<String, Object> rawFields = (Map<String, Object>) raw.get("fields");

            for (String fieldName : rawFields.keySet()) {
                Map<String, Object> rawField = (Map<String, Object>) rawFields.get(fieldName);
                RosettaField field = new RosettaField(fieldName, rawField);
                fields.put(fieldName, field);
            }
        }

        if (raw.containsKey("staticFields")) {
            Map<String, Object> rawFields = (Map<String, Object>) raw.get("staticFields");

            for (String fieldName : rawFields.keySet()) {
                Map<String, Object> rawField = (Map<String, Object>) rawFields.get(fieldName);
                RosettaField field = new RosettaField(fieldName, rawField);
                fields.put(fieldName, field);
            }
        }

        /* METHODS */
        if (raw.containsKey("methods")) {
            List<Map<String, Object>> rawMethods = (List<Map<String, Object>>) raw.get("methods");
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

        if (raw.containsKey("staticMethods")) {
            List<Map<String, Object>> rawMethods = (List<Map<String, Object>>) raw.get("staticMethods");
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

        /* CONSTRUCTORS */
        if (raw.containsKey("constructors")) {
            List<Map<String, Object>> rawConstructors =
                    (List<Map<String, Object>>) raw.get("constructors");
            for (Map<String, Object> rawConstructor : rawConstructors) {
                RosettaConstructor constructor = new RosettaConstructor(this, rawConstructor);
                constructors.add(constructor);
            }
        }
    }

    @Nullable
    public RosettaField getField(@NotNull String name) {
        return this.fields.get(name);
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
    public RosettaConstructor getConstructor(Class<?>... parameterTypes) {

        if (constructors.isEmpty()) return null;

        for (RosettaConstructor next : constructors) {

            if (next.getParameters().size() == parameterTypes.length) {
                boolean invalid = false;
                for (int index = 0; index < parameterTypes.length; index++) {
                    if (!parameterTypes[index]
                            .getSimpleName()
                            .equals(next.getParameters().get(index).type.getBasic())
                            && !parameterTypes[index]
                            .getName()
                            .equals(next.getParameters().get(index).type.getBasic())) {
                        invalid = true;
                        break;
                    }
                }
                if (invalid) continue;
                return next;
            }
        }

        return null;
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
    public List<RosettaConstructor> getConstructors() {
        return this.constructors;
    }

    @NotNull
    public Map<String, RosettaField> getFields() {
        return this.fields;
    }

    @NotNull
    public Map<String, RosettaMethodCluster> getMethods() {
        return this.methods;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getExtends() {
        return this.__extends;
    }

    @NotNull
    public String[] getModifiers() {
        return this.modifiers;
    }

    public boolean hasNotes() {
        return this.notes != null;
    }

    @Nullable
    public String getNotes() {
        return notes;
    }

    public void printClass(String prefix) {
        System.out.println(prefix + this.javaType + " " + this.getName() + ":");
        List<String> keys = new ArrayList<>(fields.keySet());
        keys.sort(Comparator.naturalOrder());

        System.out.println(prefix + "\tFields:");
        for (String key : keys) {
            System.out.println(fields.get(key).asJavaString("\t\t"));
        }

        keys = new ArrayList<>(methods.keySet());
        keys.sort(Comparator.naturalOrder());
        System.out.println(prefix + "\tMethods:");
        for (String key : keys) {
            RosettaMethodCluster cluster = methods.get(key);
            for (RosettaMethod method : cluster.getMethods()) {
                System.out.println(method.asJavaString("\t\t"));
            }
        }

        System.out.println("\tConstructors:");
        for (RosettaConstructor constructor : constructors) {
            System.out.println(constructor.asJavaString("\t\t"));
        }
    }

    @Override
    public String toString() {
        return "RosettaClass{"
                + "methods="
                + methods
                + ", fields="
                + fields
                + ", constructors="
                + constructors
                + ", modifiers="
                + Arrays.toString(modifiers)
                + ", name='"
                + name
                + '\''
                + ", __extends='"
                + __extends
                + '\''
                + ", javaType='"
                + javaType
                + '\''
                + ", notes='"
                + notes
                + '\''
                + ", deprecated="
                + deprecated
                + '}';
    }

    public Map<String, Object> toJSON() {
        Map<String, Object> mapClass = new HashMap<>();

        // MODIFIERS
        if(this.modifiers.length != 0) {
            List<String> listModifiers = new ArrayList<>();
            Collections.addAll(listModifiers, getModifiers());
            mapClass.put("modifiers", listModifiers);
        }

        // FIELDS
        if (!this.fields.isEmpty()) {
            Map<String, Object> mapFields = new HashMap<>();

            List<String> listFieldNames = new ArrayList<>(this.fields.keySet());
            listFieldNames.sort(Comparator.naturalOrder());

            for (String fieldName : listFieldNames) {
                mapFields.put(fieldName, fields.get(fieldName).toJSON());
            }

            mapClass.put("fields", mapFields);
        }

        // CONSTRUCTORS
        if (!this.constructors.isEmpty()) {
            List<Map<String, Object>> listConstructors = new ArrayList<>();
            for (RosettaConstructor cons : this.constructors) {
                listConstructors.add(cons.toJSON());
            }
            mapClass.put("constructors", listConstructors);
        }

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
            mapClass.put("methods", listMethods);
        }

        // JAVATYPE
        mapClass.put("javaType", this.javaType);

        // EXTENDS
        mapClass.put("extends", this.__extends);

        // DEPRECATED
        if (this.deprecated) {
            mapClass.put("deprecated", true);
        }

        // NOTES
        mapClass.put("notes", this.notes);

        return mapClass;
    }
}
