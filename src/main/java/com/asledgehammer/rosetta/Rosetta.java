package com.asledgehammer.rosetta;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import zombie.Lua.LuaManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "unused"})
public class Rosetta {

  public static final boolean DEBUG = true;

  private static final Gson gson = new Gson();
  private static final Yaml yaml;

  static {
    LoaderOptions options = new LoaderOptions();
    yaml = new Yaml(options);
  }

  private final Map<String, RosettaFile> files = new HashMap<>();
  private final Map<String, RosettaPackage> packages = new HashMap<>();

  public void addDirectory(@NotNull Path dir) throws IOException {
    if (!Files.exists(dir) || !Files.isDirectory(dir)) {
      throw new FileNotFoundException("Directory doesn't exist: " + dir);
    }

    final List<Path> yamlFiles = getFilesFromDir(dir);
    for (Path file : yamlFiles) {

      // (Ignore project metafiles)
      if (file.toString().startsWith(dir + "\\.")) {
        continue;
      }

      if (DEBUG) {
        System.out.println("Reading file: " + file + "..");
      }
      try (BufferedReader reader = Files.newBufferedReader(file)) {
        final String extension = getFileExtension(file.getFileName().toString().toLowerCase());

        final RosettaFile rFile =
            switch (extension) {
              case "json" ->
                  new RosettaFile(this, (Map<String, Object>) gson.fromJson(reader, Map.class));
              case "yml" ->
                  new RosettaFile(this, yaml.load(reader));
              default ->
                  throw new UnsupportedOperationException("Cannot parse file type " + extension);
            };
        files.put(file.toString(), rFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Gets the file extension from a file name.
   *
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
  private List<Path> getFilesFromDir(@NotNull Path dir) {
    if (!Files.exists(dir)) return new ArrayList<>();

    final List<Path> list = new ArrayList<>();

    try (Stream<Path> files = Files.list(dir)) {
      for (Path next : files.toList()) {
        if (Files.isDirectory(next)) {
          if (!next.getFileName().toString().equals("..")) {
            list.addAll(getFilesFromDir(next));
          }
          continue;
        }

        final String extension = getFileExtension(next.getFileName().toString().toLowerCase());

        if (extension.equals("json") || extension.equals("yml")) {
          list.add(next);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
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
    rosetta.addDirectory(Path.of("./rosetta/json/"));
    rosetta.printNamespaces("");

    RosettaClass clazz = rosetta.getClass(LuaManager.GlobalObject.class);
    clazz.printClass("");
  }
}
