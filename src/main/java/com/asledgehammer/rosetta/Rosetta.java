package com.asledgehammer.rosetta;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import zombie.Lua.LuaManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@SuppressWarnings({"unchecked", "unused"})
public class Rosetta {

  private static final Gson gson = new Gson();
  private static final Load yaml = new Load(LoadSettings.builder().build());

  private final Map<String, RosettaFile> files = new HashMap<>();
  private final Map<String, RosettaPackage> packages = new HashMap<>();

  public void addDirectory(@NotNull File dir) throws IOException {
    if (!dir.exists()) {
      throw new FileNotFoundException("Directory doesn't exist: " + dir.getPath());
    }

    final List<File> yamlFiles = getFilesFromDir(dir);
    for (File file : yamlFiles) {
      System.out.println("Reading file: " + file.getPath() + "..");
      try (FileReader reader = new FileReader(file)) {
        final String extension = getFileExtension(file.getName().toLowerCase());
        final RosettaFile rFile = switch (extension) {
              case "json" -> new RosettaFile(this, (Map<String, Object>) gson.fromJson(reader, Map.class));
              case "yml" -> new RosettaFile(this, (Map<String, Object>) yaml.loadFromReader(reader));
              default -> throw new UnsupportedOperationException("Cannot parse file type " + extension);
          };
        files.put(file.getPath(), rFile);
      }
    }
  }

  /**
   * Gets the file extension from a file name.
   * @param fileName Name of the file.
   * @return The file extension. An empty string is returned if the file name does not have one.
   */
  private @NotNull String getFileExtension(@NotNull final String fileName) {
    final int extensionPos = (fileName.lastIndexOf("."));
    if (extensionPos == -1) {
      return "";
    }

    return fileName.substring(extensionPos + 1);
  }

  @NotNull
  private List<File> getFilesFromDir(@NotNull File dir) {
    if (!dir.exists()) return new ArrayList<>();

    final List<File> list = new ArrayList<>();

    final File[] files = dir.listFiles();
    if (files == null) return list;

    for (File next : files) {
      if (next.isDirectory()) {
        if (!next.getName().equals("..")) {
          list.addAll(getFilesFromDir(next));
        }
        continue;
      }

      final String extension = getFileExtension(next.getName().toLowerCase());

      if (extension.equals("json") || extension.equals("yml")) {
        list.add(next);
      }
    }

    return list;
  }

  @Nullable
  public RosettaPackage getPackage(@NotNull Package pkg) {
    return packages.get(pkg.getName());
  }

  @Nullable
  public RosettaPackage getPackage(@NotNull String id) {
    return packages.get(id);
  }

  public void addPackage(@NotNull RosettaPackage pkg) {
    this.packages.put(pkg.getName(), pkg);
  }

  @Nullable
  public RosettaClass getClass(@NotNull Class<?> clazz) {
    final String namespaceName = clazz.getPackageName();
    final RosettaPackage pkg = getPackage(namespaceName);
    if (pkg == null) return null;
    return pkg.getClass(clazz);
  }

  public void printNamespaces(@NotNull String prefix) {
    final List<String> packageNames = new ArrayList<>(packages.keySet());
    packageNames.sort(Comparator.naturalOrder());
    System.out.println(prefix + "Namespace(s) Loaded: (size: " + packageNames.size() + ")");
    for (String name : packageNames) System.out.println(prefix + "\t" + name);
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
  public Map<String, RosettaPackage> getPackages() {
    return this.packages;
  }

  public static void main(String[] yargs) throws IOException {
    final Rosetta rosetta = new Rosetta();
    rosetta.addDirectory(new File("./rosetta/json/"));
    rosetta.printNamespaces("");

    RosettaClass clazz = rosetta.getClass(LuaManager.GlobalObject.class);
    clazz.printClass("");
  }
}
