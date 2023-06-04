package com.asledgehammer.candle.yamldoc;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class YamlDocs {

  static Yaml yaml = new Yaml();

  Map<String, YamlFile> definitions = new HashMap<>();

  public void addDirectory(File dir) throws FileNotFoundException {
    if (!dir.exists()) {
      throw new FileNotFoundException("Directory doesn't exist: " + dir.getPath());
    }

    List<File> yamlFiles = getFilesFromDir(dir);
    for (File file : yamlFiles) {
      YamlFile definition = new YamlFile(file);
      definitions.put(definition.path, definition);
    }
  }

  private List<File> getFilesFromDir(File dir) {
    if (!dir.exists()) return new ArrayList<>();

    List<File> list = new ArrayList<>();

    File[] files = dir.listFiles();
    if (files == null) return list;

    for (File next : files) {
      if (next.isDirectory() && !next.getName().equals("..")) {
        list.addAll(getFilesFromDir(next));
      }

      if (next.getName().toLowerCase().endsWith(".yml")) {
        list.add(next);
      }
    }

    return list;
  }

  public YamlFile getFile(String path) {
    return definitions.get(path.replaceAll("\\$", "."));
  }

  public static void main(String[] args) throws FileNotFoundException {
    YamlDocs yamlDocs = new YamlDocs();
    yamlDocs.addDirectory(new File("./docs"));
  }
}
