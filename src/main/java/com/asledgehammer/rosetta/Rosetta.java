package com.asledgehammer.rosetta;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.Lua.LuaManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@SuppressWarnings({"unchecked", "unused"})
public class Rosetta {

  private static final Gson gson = new Gson();

  private final Map<String, RosettaFile> files = new HashMap<>();
  private final Map<String, RosettaNamespace> namespaces = new HashMap<>();

  public void addDirectory(@NotNull File dir) throws IOException {
    if (!dir.exists()) {
      throw new FileNotFoundException("Directory doesn't exist: " + dir.getPath());
    }

    final List<File> yamlFiles = getFilesFromDir(dir);
    for (File file : yamlFiles) {
      System.out.println("Reading file: " + file.getPath() + "..");
      try (FileReader reader = new FileReader(file)) {
        final RosettaFile rFile =
            new RosettaFile(this, (Map<String, Object>) gson.fromJson(reader, Map.class));
        files.put(file.getPath(), rFile);
      }
    }
  }

  @NotNull
  private List<File> getFilesFromDir(@NotNull File dir) {
    if (!dir.exists()) return new ArrayList<>();

    final List<File> list = new ArrayList<>();

    final File[] files = dir.listFiles();
    if (files == null) return list;

    for (File next : files) {
      if (next.isDirectory() && !next.getName().equals("..")) {
        list.addAll(getFilesFromDir(next));
      }
      if (next.getName().toLowerCase().endsWith(".json")) {
        list.add(next);
      }
    }

    return list;
  }

  @Nullable
  public RosettaNamespace getNamespace(@NotNull Package pkg) {
    return namespaces.get(pkg.getName());
  }

  @Nullable
  public RosettaNamespace getNamespace(@NotNull String id) {
    return namespaces.get(id);
  }

  public void addNamespace(@NotNull RosettaNamespace namespace) {
    this.namespaces.put(namespace.getName(), namespace);
  }

  @Nullable
  public RosettaClass getClass(@NotNull Class<?> clazz) {
    final String namespaceName = clazz.getPackageName();
    final RosettaNamespace namespace = getNamespace(namespaceName);
    if (namespace == null) return null;
    return namespace.getClass(clazz);
  }

  public void printNamespaces(@NotNull String prefix) {
    final List<String> namespaceNames = new ArrayList<>(namespaces.keySet());
    namespaceNames.sort(Comparator.naturalOrder());
    System.out.println(prefix + "Namespace(s) Loaded: (size: " + namespaceNames.size() + ")");
    for (String name : namespaceNames) System.out.println(prefix + "\t" + name);
  }

  @Override
  public String toString() {
    return "Rosetta{" + "files=" + files + '}';
  }

  @NotNull
  public Map<String, RosettaFile> getFiles() {
    return this.files;
  }

  @NotNull
  public Map<String, RosettaNamespace> getNamespaces() {
    return this.namespaces;
  }

  public static void main(String[] yargs) throws IOException {
    final Rosetta rosetta = new Rosetta();
    rosetta.addDirectory(new File("./rosetta/json/"));
    rosetta.printNamespaces("");

    RosettaClass clazz = rosetta.getClass(LuaManager.GlobalObject.class);
    clazz.printClass("");
  }
}
