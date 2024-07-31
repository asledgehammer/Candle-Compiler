package com.asledgehammer.candle;

import com.asledgehammer.rosetta.Rosetta;
import com.google.common.reflect.ClassPath;
import zombie.input.JoypadManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.FloatBuffer;
import java.util.*;

public class CandleGraph {

  final Map<Class<?>, CandleAlias> aliases = new HashMap<>();
  public final Map<Class<?>, CandleClass> classes = new HashMap<>();
  public final List<CandleAlias> aliasesSorted = new ArrayList<>();
  public final List<CandleClass> classesSorted = new ArrayList<>();
  public final CandleClassBag classBag = new CandleClassBag();

  final Rosetta docs = new Rosetta();

  int changed = 0;

  public void walk(boolean clear) {

    try {
      docs.addDirectory(new File("./rosetta/json/"));
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (clear) {
      aliases.clear();
      classes.clear();
      aliasesSorted.clear();
      classesSorted.clear();

      for (Class<?> clazz : classBag.getClasses()) {
        CandleClass eClass = new CandleClass(clazz);
        classes.put(clazz, eClass);
      }
    }

    do {
      changed = 0;

      List<Class<?>> classKeys = new ArrayList<>(classes.keySet());
      classKeys.sort(CandleClassComparator.INSTANCE);
      for (Class<?> clazz : classKeys) {
        CandleClass candleClass = classes.get(clazz);
        candleClass.walk(this);
        if (!classesSorted.contains(candleClass)) {
          classesSorted.add(candleClass);
        }
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
        if (!aliasesSorted.contains(candleAlias)) {
          aliasesSorted.add(candleAlias);
        }
      }
    } while (changed != 0);
  }

  public void walkEverything() throws IOException {
    Instrumentation inst = InstrumentHook.getInstrumentation();
    for (Class<?> clazz: inst.getAllLoadedClasses()) {
      if(clazz.getPackage() == null) continue;
      if(clazz.isAnonymousClass()) continue;
      if(clazz.isHidden()) continue;
      addClass(clazz);
    }
  }

  public void walkLegacy() {
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
    // Exceptions
    if (clazz.equals(Map.class) || clazz.equals(List.class)) return;

    if (isClass(clazz)) return;

    if (!this.aliases.containsKey(clazz)) {
      this.aliases.put(clazz, new CandleAlias(clazz));
      this.changed++;
    }
  }

  public void addAlias(CandleAlias candleAlias) {
    Class<?> clazz = candleAlias.getClazz();
    if (isClass(clazz)) return;

    if (!this.aliases.containsKey(clazz)) {
      this.aliases.put(clazz, candleAlias);
      this.changed++;
    }
  }

  public void addClass(Class<?> clazz) {
    if (isAlias(clazz)) {
      aliases.remove(clazz);
    }
    if (!this.classes.containsKey(clazz)) {
      this.classes.put(clazz, new CandleClass(clazz));
      this.changed++;
    }
  }

  public void addClass(CandleClass candleClass) {
    Class<?> clazz = candleClass.getClazz();
    if (isAlias(clazz)) aliases.remove(clazz);
    if (!this.classes.containsKey(clazz)) {
      this.classes.put(clazz, candleClass);
      this.changed++;
    }
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
    if (!isExposedClass(clazz) && !CandleClassBag.isExempt(clazz)) {
      // Maybe this is better.
      addClass(clazz);
      // addAlias(clazz);
    }
  }
}
