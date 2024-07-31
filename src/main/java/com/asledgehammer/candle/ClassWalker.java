package com.asledgehammer.candle;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClassWalker {

  public final List<String> clazzes = new ArrayList<>();

  public ClassWalker() {}

  public void walk(String packagePrefix, String path) throws IOException {
    path = path.replaceAll("/", "\\\\");
    File fDir = new File(path);
    if (!fDir.exists()) {
      throw new FileNotFoundException(fDir.getPath());
    }

    walkRecursively(packagePrefix, path, fDir, clazzes);
  }

  private void walkRecursively(
      String packagePrefix, String rootDir, File dir, List<String> clazzes) {
    File[] entries = dir.listFiles();
    if (entries != null) {
      for (File entry : entries) {
        if (entry.isDirectory()) {
          walkRecursively(packagePrefix, rootDir, entry, clazzes);
        } else if (entry.isFile() && entry.getName().toLowerCase().endsWith(".class")) {

          String path = entry.getPath();
          if (!isValid(path)) {
            continue;
          }

          String pruned =
              packagePrefix
                  + "."
                  + entry
                      .getPath()
                      .replace(rootDir, "")
                      .replaceAll("\\\\", ".")
                      .replaceAll("\\$", ".");

          if (!clazzes.contains(pruned)) {
            clazzes.add(pruned);
          }
        }
      }
    }
  }

  private boolean isValid(String s) {

    if (s.contains("$")) {
      for (int index = 0; index < 100; index++) {
        if (s.contains("$" + index)) return false;
      }
    }

    return true;
  }

  public void save(File file) throws IOException {
    FileWriter fw = new FileWriter(file);
    BufferedWriter bw = new BufferedWriter(fw);

    fw.write("static final Class<?>[] classes = new Class<?>[] {\n");

    for (String s : clazzes) {
      fw.write("    " + s + ",\n");
    }

    fw.write("};\n");

    fw.flush();
    fw.close();
  }

  public static void main(String[] yargs) throws IOException {
    ClassWalker walker = new ClassWalker();
    walker.walk("astar", "D:/SteamLibrary/steamapps/common/ProjectZomboid/astar/");
    walker.walk("com", "D:/SteamLibrary/steamapps/common/ProjectZomboid/com/");
    walker.walk("de", "D:/SteamLibrary/steamapps/common/ProjectZomboid/de/");
    walker.walk("fmod", "D:/SteamLibrary/steamapps/common/ProjectZomboid/fmod/");
    walker.walk("javax", "D:/SteamLibrary/steamapps/common/ProjectZomboid/javax/");
    walker.walk("N3D", "D:/SteamLibrary/steamapps/common/ProjectZomboid/N3D/");
    walker.walk("org", "D:/SteamLibrary/steamapps/common/ProjectZomboid/org/");
    walker.walk("se", "D:/SteamLibrary/steamapps/common/ProjectZomboid/se/");
    walker.walk("zombie", "D:/SteamLibrary/steamapps/common/ProjectZomboid/zombie - copy/");
    walker.save(new File("pz_classes.txt"));
  }
}
