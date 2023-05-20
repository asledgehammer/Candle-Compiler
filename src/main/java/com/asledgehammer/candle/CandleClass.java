package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class CandleClass extends CandleEntity<CandleClass> {

  private final Map<String, CandleField> fields = new HashMap<>();
  private final Map<String, CandleExecutableCluster<CandleMethod>> methodsStatic = new HashMap<>();
  private final Map<String, CandleExecutableCluster<CandleMethod>> methods = new HashMap<>();
  private CandleExecutableCluster<CandleConstructor> constructors;

  public CandleClass(@NotNull Class<?> clazz) {
    super(clazz);
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    walkFields(graph);
    walkMethods(graph);
    walkConstructors(graph);
  }

  private void walkConstructors(CandleGraph graph) {

    Class<?> clazz = getClazz();
    if (clazz.isEnum()) return;

    Constructor[] jConstructors = getClazz().getDeclaredConstructors();

    for (Constructor jConstructor : jConstructors) {
      int modifiers = jConstructor.getModifiers();

      // Make sure that we only add public constructors.
      if (!Modifier.isPublic(modifiers)) continue;

      if (constructors == null) {
        constructors = new CandleExecutableCluster<>(new CandleConstructor(jConstructor));
      } else {
        constructors.add(new CandleConstructor(jConstructor));
      }
    }

    if (hasConstructors()) constructors.walk(graph);
  }

  /**
   * Walks the fields and only handles fields that are public and static. (Only visible fields in
   * Kahlua)
   *
   * @param graph
   */
  private void walkFields(@NotNull CandleGraph graph) {
    Class<?> clazz = getClazz();
    List<Field> fieldz = new ArrayList<>(Arrays.stream(clazz.getDeclaredFields()).toList());

    // Add declared fields in any implementations.
    for (Class<?> interfaze : clazz.getInterfaces()) {
      fieldz.addAll(Arrays.stream(interfaze.getDeclaredFields()).toList());
    }

    for (Field field : fieldz) {

      // If not an exposed class, attempt to add as alias.
      graph.evaluate(field.getType());

      int modifiers = field.getModifiers();
      if (!Modifier.isPublic(modifiers)) continue;
      else if (!Modifier.isStatic(modifiers)) continue;
      CandleField emmyField = new CandleField(field);
      fields.put(emmyField.getLuaName().toLowerCase(), emmyField);
    }

    List<String> keysSorted = new ArrayList<>(fields.keySet());
    keysSorted.sort(Comparator.naturalOrder());
    for (String fieldName : keysSorted) {
      CandleField candleField = fields.get(fieldName);
      //      System.out.println(
      //          "Candle: Walking Class: FIELD    public static "
      //              + candleField.getClazz().getSimpleName()
      //              + " "
      //              + candleField.getLuaName());
      candleField.walk(graph);
    }
  }

  private void walkMethods(@NotNull CandleGraph graph) {
    Class<?> clazz = getClazz();
    List<Method> methodz = new ArrayList<>(Arrays.stream(clazz.getDeclaredMethods()).toList());

    // Add declared fields in any implementations.
    for (Class<?> interfaze : clazz.getInterfaces()) {
      methodz.addAll(Arrays.stream(interfaze.getDeclaredMethods()).toList());
    }

    for (Method method : methodz) {

      CandleMethod candleMethod = new CandleMethod(method);

      // (Only digest public methods)
      if (!candleMethod.isPublic()) continue;

      String nameLower = candleMethod.getLuaName();
      boolean created = false;

      // Attempt to grab the cluster. If it doesn't exist, create one.
      CandleExecutableCluster<CandleMethod> cluster;
      if (candleMethod.isStatic()) {
        cluster = methodsStatic.get(nameLower);
        if (cluster == null) {
          cluster = new CandleExecutableCluster<>(candleMethod);
          methodsStatic.put(nameLower, cluster);
          created = true;
        }
      } else {
        cluster = methods.get(nameLower);
        if (cluster == null) {
          cluster = new CandleExecutableCluster<>(candleMethod);
          methods.put(nameLower, cluster);
          created = true;
        }
      }

      // If a cluster already exists for the method, simply add it.
      if (!created) {
        cluster.add(candleMethod);
      }
    }

    // Walk through both groups of method clusters.
    walkMethodClusters(graph, this.methodsStatic);
    walkMethodClusters(graph, this.methods);
  }

  private void walkMethodClusters(
      @NotNull CandleGraph graph,
      @NotNull Map<String, CandleExecutableCluster<CandleMethod>> clusters) {
    List<String> keysSorted = new ArrayList<>(clusters.keySet());
    keysSorted.sort(Comparator.naturalOrder());

    for (String key : keysSorted) {
      CandleExecutableCluster<CandleMethod> cluster = clusters.get(key);
      //      System.out.println(
      //          "Candle: Walking Class: METHOD    public "
      //              + (cluster.getFirst().isStatic() ? "static " : " ")
      //              + cluster.getFirst().getReturnType().getSimpleName()
      //              + " "
      //              + cluster.getLuaName()
      //              + "()");
      cluster.walk(graph);
    }
  }

  public void save(@NotNull File dir) {
    File dirPackage = new File(dir, getClazz().getPackageName().replace("\\.", "/"));
    if (!dirPackage.exists()) dirPackage.mkdirs();
    File file = new File(dirPackage, getClazz().getSimpleName() + ".lua");
    CandleGraph.write(file, getRenderedCode());
  }

  @NotNull
  public Map<String, CandleField> getFields() {
    return fields;
  }

  @NotNull
  public Map<String, CandleExecutableCluster<CandleMethod>> getStaticMethods() {
    return methodsStatic;
  }

  @NotNull
  public Map<String, CandleExecutableCluster<CandleMethod>> getMethods() {
    return methods;
  }

  public CandleExecutableCluster<CandleConstructor> getConstructors() {
    return constructors;
  }

  public boolean hasConstructors() {
    return constructors != null;
  }
}
