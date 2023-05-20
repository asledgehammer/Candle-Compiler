package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class YamlFile extends YamlEntity {

  @NotNull final Map<String, YamlField> fields;
  @NotNull final Map<String, YamlMethodCluster> methods;
  @NotNull final List<YamlConstructor> constructors;
  @NotNull final String[] modifiers;
  @NotNull final String type;
  @NotNull final String name;
  @NotNull final String pkg;
  @NotNull final String path;
  @Nullable final String notes;

  YamlFile(@NotNull File file) {

    super(getMapFromFile(file));

    this.pkg = readString("package", true);
    this.name = readString("name", true);
    this.path = pkg + "." + name;
    this.type = readString("type", true);
    this.notes = readString("notes");

    this.modifiers = readModifiers();
    this.methods = readMethods();
    this.fields = readFields();
    this.constructors = readConstructors();
  }

  @NotNull
  private Map<String, YamlField> readFields() {
    if (!raw.containsKey("fields")) return new HashMap<>();

    Map<String, YamlField> fields = new HashMap<>();

    List<Map<String, Object>> list = (List<Map<String, Object>>) raw.get("fields");
    for (Map<String, Object> rawField : list) {
      YamlField field = new YamlField(rawField);
      fields.put(field.name, field);
    }

    return fields;
  }

  @NotNull
  private Map<String, YamlMethodCluster> readMethods() {
    if (!raw.containsKey("methods")) return new HashMap<>();

    Map<String, YamlMethodCluster> methods = new HashMap<>();

    List<Map<String, Object>> list = (List<Map<String, Object>>) raw.get("methods");
    for (Map<String, Object> rawMethod : list) {
      YamlMethod method = new YamlMethod(rawMethod);
      YamlMethodCluster cluster;
      if (methods.containsKey(method.name)) {
        cluster = methods.get(method.name);
      } else {
        cluster = new YamlMethodCluster(method.name);
        methods.put(method.name, cluster);
      }
      cluster.add(method);
    }

    return methods;
  }

  @NotNull
  private List<YamlConstructor> readConstructors() {
    if (!raw.containsKey("constructors")) return new ArrayList<>();
    List<YamlConstructor> constructors = new ArrayList<>();
    List<Map<String, Object>> list = (List<Map<String, Object>>) raw.get("constructors");
    for (Map<String, Object> rawConstructor : list) {
      YamlConstructor constructor = new YamlConstructor(rawConstructor, this.name);
      constructors.add(constructor);
    }
    return constructors;
  }

  @Nullable
  public YamlField getField(@NotNull String name) {
    return this.fields.get(name);
  }

  @Nullable
  public YamlMethodCluster getMethodsByName(@NotNull String name) {
    return this.methods.get(name);
  }

  @Nullable
  public YamlMethod getMethod(String name, Class<?>... parameterTypes) {
    YamlMethodCluster cluster = getMethodsByName(name);
    if (cluster == null) return null;
    return cluster.getWithParameters(parameterTypes);
  }

  @NotNull
  public List<YamlConstructor> getConstructors() {
    return this.constructors;
  }

  @NotNull
  public Map<String, YamlField> getFields() {
    return this.fields;
  }

  @NotNull
  public Map<String, YamlMethodCluster> getMethods() {
    return this.methods;
  }

  @NotNull
  public String getName() {
    return this.name;
  }

  @NotNull
  String getPackage() {
    return this.pkg;
  }

  @NotNull
  public String getPath() {
    return this.path;
  }
}
