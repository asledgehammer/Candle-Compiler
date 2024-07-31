package com.asledgehammer.candle;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClassLoadParser {

  public final List<Class<?>> classes = new ArrayList<>();

  public ClassLoadParser() {}

  public void parse(String path) throws IOException {
    FileReader fr = new FileReader(path);
    BufferedReader br = new BufferedReader(fr);

    List<String> clazzes = new ArrayList<>();
    String line;
    while ((line = br.readLine()) != null) {
      String classPath = line.split("load] ")[1].split(" source: ")[0].replaceAll("/", ".").trim();
      if (classPath.contains(".$") || classPath.contains("0x") || classPath.contains("..")) {
        continue;
      }
      clazzes.add(classPath);
    }

    br.close();

    int loaded = 0;
    clazzes.sort(Comparator.naturalOrder());
    for (String cPath : clazzes) {

      if (!cPath.equals("java.lang.OutOfMemoryError")) {
        System.out.println("Class: " + cPath);
      }

      try {
        Class<?> clazz = Class.forName(cPath, false, ClassLoader.getSystemClassLoader());
        this.classes.add(clazz);
        loaded++;
      } catch (ClassNotFoundException e) {
        e.printStackTrace(System.out);
      }
    }

    System.out.println("Loaded: " + loaded + " / " + clazzes.size());
  }

  public static void main(String[] yargs) throws IOException {
    new ClassLoadParser().parse("classes.txt");
  }
}
