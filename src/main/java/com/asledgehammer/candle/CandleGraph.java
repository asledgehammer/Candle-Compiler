package com.asledgehammer.candle;

import com.asledgehammer.rosetta.Rosetta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CandleGraph {

  final Map<Class<?>, CandleAlias> aliases = new HashMap<>();
  final Map<Class<?>, CandleClass> classes = new HashMap<>();
  final List<CandleAlias> aliasesSorted = new ArrayList<>();
  final List<CandleClass> classesSorted = new ArrayList<>();
  final CandleClassBag classBag = new CandleClassBag();

  final Rosetta docs = new Rosetta();

  public void walk() {

    try {
      docs.addDirectory(new File("./rosetta/json/"));
    } catch (Exception e) {
      e.printStackTrace();
    }

    aliases.clear();
    classes.clear();
    aliasesSorted.clear();
    classesSorted.clear();

    for (Class<?> clazz : classBag.getClasses()) {
      CandleClass eClass = new CandleClass(clazz);
      classes.put(clazz, eClass);
    }

    List<Class<?>> classKeys = new ArrayList<>(classes.keySet());
    classKeys.sort(CandleClassComparator.INSTANCE);
    for (Class<?> clazz : classKeys) {
      CandleClass candleClass = classes.get(clazz);
      // System.out.println("Candle: Walking: " + candleClass.getLuaName());
      candleClass.walk(this);
      classesSorted.add(candleClass);
    }

    List<Class<?>> aliasKeys = new ArrayList<>(aliases.keySet());
    aliasKeys.sort(CandleClassComparator.INSTANCE);
    for (Class<?> clazz : aliasKeys) {
      if (isClass(clazz)) {
        aliases.remove(clazz);
        continue;
      }

      CandleAlias candleAlias = aliases.get(clazz);
      candleAlias.walk(this);
      aliasesSorted.add(candleAlias);
    }
  }

  public void render(CandleRenderAdapter adapter) {
    for (CandleAlias candleAlias : aliasesSorted) {
      candleAlias.render(adapter.getAliasRenderer());
    }
    for (CandleClass candleClass : classesSorted) {
      System.out.println("Candle: Render Class: " + candleClass.getLuaName());
      candleClass.render(adapter.getClassRenderer());
    }
  }

  public void save(File dir) throws IOException {
    saveAlias(dir);

    for (CandleClass candleClass : classesSorted) {
      System.out.println("Candle: Writing: " + candleClass.getLuaName());
      candleClass.save(dir);
    }
  }

  private void saveAlias(File dir) {
    File file = new File(dir, "__alias.lua");
    System.out.println("Candle: Writing __alias.lua..");
    StringBuilder builder = new StringBuilder();
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

  public Rosetta getDocs() {
    return this.docs;
  }

  public boolean hasDocs() {
    return this.docs != null;
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
