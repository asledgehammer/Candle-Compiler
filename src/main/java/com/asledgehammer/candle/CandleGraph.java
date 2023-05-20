package com.asledgehammer.candle;

import zombie.iso.sprite.IsoSprite;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CandleGraph {

  CandleClassBag classBag = new CandleClassBag();

  Map<Class<?>, CandleAlias> aliases = new HashMap<>();
  Map<Class<?>, CandleClass> classes = new HashMap<>();
  List<CandleAlias> aliasesSorted = new ArrayList<>();
  List<CandleClass> classesSorted = new ArrayList<>();

  public CandleGraph() {}

  public void walk() {

    aliases.clear();
    classes.clear();
    aliasesSorted.clear();
    classesSorted.clear();

    for (Class<?> clazz : classBag.getClasses()) {
      CandleClass eClass = new CandleClass(clazz);
      classes.put(clazz, eClass);
    }

    List<Class<?>> classKeys = new ArrayList<>(classes.keySet());
    classKeys.sort(ClazzComparator.INSTANCE);
    for (Class<?> clazz : classKeys) {
      CandleClass candleClass = classes.get(clazz);
      System.out.println("Candle: Walking: " + candleClass.getLuaName());
      candleClass.walk(this);
      classesSorted.add(candleClass);
    }

    List<Class<?>> aliasKeys = new ArrayList<>(aliases.keySet());
    aliasKeys.sort(ClazzComparator.INSTANCE);
    for (Class<?> clazz : aliasKeys) {
      if (isClass(clazz)) {
        aliases.remove(clazz);
        continue;
      }

      CandleAlias candleAlias = aliases.get(clazz);
      //      System.out.println("Candle: Walking Alias: " + candleAlias.getLuaName());
      candleAlias.walk(this);
      aliasesSorted.add(candleAlias);
    }

    System.out.println(
        "IsoSprite: isClass = "
            + isClass(IsoSprite.class)
            + ", isAlias = "
            + isAlias(IsoSprite.class));
  }

  public void render(CandleRenderAdapter adapter) {
    for (CandleAlias candleAlias : aliasesSorted) {
      //      System.out.println("Candle: Rendering Alias: " + candleAlias.getLuaName());
      candleAlias.render(adapter.getAliasRenderer());
    }
    for (CandleClass candleClass : classesSorted) {
      //      System.out.println("Candle: Render Class: " + candleClass.getLuaName());
      candleClass.render(adapter.getClassRenderer());
    }
  }

  public void save(File dir) {
    saveAlias(dir);

    for (CandleClass candleClass : classesSorted) {
      System.out.println("Candle: Writing: " + candleClass.getLuaName());
      candleClass.save(dir);
    }
  }

  private void saveAlias(File dir) {
    File file = new File(dir, "__alias.lua");
    //    System.out.println("Candle: Saving __alias.lua..");
    StringBuilder builder = new StringBuilder("""

    ---------- ALIAS ----------

    """);
    for (CandleAlias candleAlias : aliasesSorted) {
      if (isClass(candleAlias.getClazz())) continue;
      String renderedCode = candleAlias.getRenderedCode();
      if (builder.indexOf(renderedCode) != -1) continue;

      builder.append(candleAlias.getRenderedCode()).append('\n');
    }

    write(file, builder.toString());
  }

  public void addAlias(Class<?> clazz) {
    if (isClass(clazz)) return;
    this.aliases.put(clazz, new CandleAlias(clazz));
  }

  public void addAlias(CandleAlias candleAlias) {
    Class<?> clazz = candleAlias.getClazz();
    if (isClass(clazz)) return;
    this.aliases.put(clazz, candleAlias);
  }

  public void addClass(Class<?> clazz) {
    if (isAlias(clazz)) aliases.remove(clazz);
    this.classes.put(clazz, new CandleClass(clazz));
  }

  public void addClass(CandleClass candleClass) {
    Class<?> clazz = candleClass.getClazz();
    if (isAlias(clazz)) aliases.remove(clazz);
    this.classes.put(clazz, candleClass);
  }

  public boolean isAlias(Class<?> clazz) {
    return this.aliases.containsKey(clazz);
  }

  public boolean isClass(Class<?> clazz) {
    return this.classes.containsKey(clazz);
  }

  public boolean isExposedClass(Class<?> clazz) {
    return this.classBag.getClasses().contains(clazz);
  }

  public static void write(File file, String content) {
    try {
      FileWriter writer = new FileWriter(file);
      writer.write(content);
      writer.flush();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void evaluate(Class<?> clazz) {
    while (clazz.isArray()) clazz = clazz.getComponentType();
    if (!isExposedClass(clazz) && !CandleClassBag.isExempt(clazz)) addAlias(clazz);
  }
}

class ClazzComparator implements Comparator<Class<?>> {

  static ClazzComparator INSTANCE = new ClazzComparator();

  @Override
  public int compare(Class<?> o1, Class<?> o2) {
    int compare = o1.getSimpleName().compareTo(o2.getSimpleName());
    if (compare == 0) return o1.getName().compareTo(o2.getName());
    return compare;
  }
}
