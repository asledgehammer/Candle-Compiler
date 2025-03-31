package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unchecked")
public class RosettaPackage extends RosettaEntity {

    private final Map<String, RosettaClass> classes = new HashMap<>();
    private final String name;

    RosettaPackage(@NotNull String name, @NotNull Map<String, Object> raw) {
        super(raw);

        this.name = name;
        parse(raw);
    }

    public void parse(@NotNull Map<String, Object> raw) {
        /* CLASSES */
        for (String clazzName : raw.keySet()) {
            if (classes.containsKey(clazzName)) {
                throw new RuntimeException("Duplicate class definition: " + clazzName);
            }
            Map<String, Object> rawClazz = (Map<String, Object>) raw.get(clazzName);
            RosettaClass clazz = new RosettaClass(clazzName, rawClazz);
            // (Formatted Class Name)
            this.classes.put(clazz.getName(), clazz);
        }
    }

    @Override
    public String toString() {
        return "RosettaNamespace{" + "classes=" + classes + '}';
    }

    @NotNull
    public Map<String, RosettaClass> getClasses() {
        return this.classes;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @Nullable
    public RosettaClass getClass(@NotNull Class<?> clazz) {
        String simpleName = clazz.getSimpleName().replaceAll("\\$", ".");
        RosettaClass rClazz = classes.get(simpleName);
        if (rClazz == null) {
            String name = clazz.getName().replaceAll("\\$", ".").replaceFirst(this.name + ".", "");
            rClazz = classes.get(name);
        }
        return rClazz;
    }

    public void printClasses(String prefix) {
        List<String> clazzNames = new ArrayList<>(classes.keySet());
        clazzNames.sort(Comparator.naturalOrder());
        System.out.println(prefix + "Classes: (size: " + clazzNames.size() + ")");
        for (String clazzName : clazzNames) System.out.println(prefix + "\t" + clazzName);
    }

    @Nullable
    public RosettaClass getClass(@NotNull String id) {
        return this.classes.get(id);
    }

    public Map<String, Object> toJSON() {
        Map<String, Object> mapNamespace = new HashMap<>();

        // CLASSES
        if (!this.classes.isEmpty()) {
            List<String> classNames = new ArrayList<>(this.classes.keySet());
            classNames.sort(Comparator.naturalOrder());

            for (String className : classNames) {
                mapNamespace.put(className, this.classes.get(className).toJSON());
            }
        }

        return mapNamespace;
    }
}
