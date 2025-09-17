package com.asledgehammer.candle;

import com.asledgehammer.candle.impl.*;
import zombie.Lua.LuaManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

class Candle {

  public static boolean addReturnClasses = false;
  final CandleGraph graph;

  public static boolean addSubClasses = false;
  public static boolean addSuperClasses = false;
  public static boolean addParameterClasses = false;

  private static final int MODE_NONE = 0;
  private static final int MODE_LUA = 1;
  private static final int MODE_TYPESCRIPT = 2;
  private static final int MODE_PYTHON = 3;
  private static final int MODE_ROSETTA = 4;

  private static final int ROSETTA_FORMAT_NONE = 0;
  private static final int ROSETTA_FORMAT_JSON = 1;
  private static final int ROSETTA_FORMAT_YAML = 2;

  void walk(boolean clear) {
    this.graph.walk(clear);
  }

  void render(CandleRenderAdapter adapter) {
    this.graph.render(adapter);
  }

  void save(File dir) throws IOException {
    saveJavaAPI(dir);
    saveGlobalAPI(dir, "lua");
    this.graph.save(dir, "lua");
  }

  void saveGlobalAPI(File dir, String extension) {
    CandleClass candleGlobalObject = this.graph.classes.get(LuaManager.GlobalObject.class);
    Map<String, CandleExecutableCluster<CandleMethod>> methods =
        new HashMap<>(candleGlobalObject.getStaticMethods());
    methods.putAll(candleGlobalObject.getMethods());

    List<String> keysSorted = new ArrayList<>(methods.keySet());
    keysSorted.sort(Comparator.naturalOrder());
    StringBuilder builder = new StringBuilder();
    for (String methodName : keysSorted) {
      CandleExecutableCluster<CandleMethod> cluster = methods.get(methodName);
      String renderedCode = cluster.getRenderedCode();
      if (renderedCode != null) {
        builder
            .append("\n")
            .append(
                renderedCode
                    .replaceAll("GlobalObject.", "")
                    .replaceAll("--- @public\n", "")
                    .replaceAll("--- @static\n", ""))
            .append("\n");
      }
    }
    System.out.println("Candle: Writing __global." + extension + " ..");
    CandleGraph.write(new File(dir, "__global." + extension), "--- @meta _\n" + builder);
  }

  public void saveJavaAPI(File dir) {
    String fileContents =
        """
        --- @meta _

        --- @alias short number
        --- @alias Unknown any
        --- @alias Short short
        """;

    System.out.println("Candle: Writing __java.lua ..");
    CandleGraph.write(new File(dir, "__java.lua"), fileContents);
  }

  public static void printHelpMessage() {

    String s =
"""
  Candle-Compiler (By asledgehammer, 2025)

  Arguments:

    -h, --help :: Displays this message. ::

    -l, --language [LANGUAGE] :: Sets the language option. ::

      - LANGUAGE: 'lua', 'typescript', 'python', or 'rosetta'.

    -o, --output [PATH] :: Sets the output directory. ::
      - PATH: The path to the output directory. DEFAULT: "dist/"

    -r, --rosetta [PATH] [MODE] :: Sets the rosetta configuration. ::
      - PATH: The path to the copy of Rosetta documentation. DEFAULT: "rosetta/"
      - MODE: Either 'yaml' or 'json' formats. (Only used when exporting to Rosetta files)

  Contributors:
    - JabDoesThings (https://github.com/JabDoesThings)
    - Albion (https://github.com/demiurgeQuantified)

""";

    System.out.println(s);
  }

  public static void main(String[] yargs) {

    File outputDir = new File("dist/");
    File rosettaDir = new File("rosetta/");
    int mode = 0;
    int rosettaFormat = ROSETTA_FORMAT_NONE;

    try {
      for (int index = 0; index < yargs.length; ) {
        String arg1 = yargs[index];

        switch (arg1.toLowerCase()) {
          case "--help", "-h":
            {
              printHelpMessage();
              System.exit(0);
            }
          case "--language", "-l":
            {
              String arg2 = yargs[index + 1];
              switch (arg2.toLowerCase()) {
                case "lua":
                  {
                    mode = MODE_LUA;
                    break;
                  }
                case "typescript":
                  {
                    mode = MODE_TYPESCRIPT;
                    break;
                  }
                case "python":
                  {
                    mode = MODE_PYTHON;
                    break;
                  }
                case "rosetta":
                  {
                    mode = MODE_ROSETTA;
                    break;
                  }
                default:
                  {
                    throw new IllegalArgumentException("Unknown language: " + arg2.toLowerCase());
                  }
              }
              index += 2;
              break;
            }
          case "--output", "-o":
            {
              String arg2 = yargs[index + 1];
              outputDir = new File(arg2);
              if (outputDir.exists() && !outputDir.isDirectory()) {
                throw new IllegalArgumentException(
                    "The Output directory argument isn't a valid directory: " + arg2);
              }
              index += 2;
              break;
            }
          case "--rosetta", "-r":
            {
              String arg2 = yargs[index + 1];

              rosettaDir = new File(arg2);
              if (rosettaDir.exists() && !rosettaDir.isDirectory()) {
                throw new IllegalArgumentException(
                    "The Rosetta directory argument isn't a valid directory: " + arg2);
              }

              String arg3 = yargs[index + 2];

              switch (arg3.toLowerCase()) {
                case "yaml", "yml":
                  {
                    rosettaFormat = ROSETTA_FORMAT_YAML;
                    break;
                  }
                case "json":
                  {
                    rosettaFormat = ROSETTA_FORMAT_JSON;
                    break;
                  }
                default:
                  {
                    throw new IllegalArgumentException(
                        "The Rosetta format isn't known or valid: " + arg3.toLowerCase());
                  }
              }

              index += 3;
              break;
            }
        }
      }

      if (mode == MODE_NONE) {
        throw new RuntimeException("No output language is set.");
      }

      if (rosettaFormat == ROSETTA_FORMAT_NONE) {
        throw new RuntimeException("No Rosetta mode is set.");
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
      printHelpMessage();
    }

    try {
      switch (mode) {
        case MODE_LUA:
          {
            mainLua(outputDir, rosettaDir);
            break;
          }
        case MODE_TYPESCRIPT:
          {
            mainTypescript(outputDir, rosettaDir);
            break;
          }
        case MODE_PYTHON:
          {
            mainPy(outputDir, rosettaDir);
            break;
          }
        case MODE_ROSETTA:
          {
            mainRosetta(outputDir, rosettaDir, rosettaFormat);
          }
      }
    } catch (Exception e) {
      System.err.println("Candle-Compiler failed to run.");
      e.printStackTrace(System.err);
    }
  }

  private static void mainRosetta(File output, File rosettaDir, int rosettaMode)
      throws IOException {

    if (!output.exists() && !output.mkdirs())
      throw new IOException("Failed to mkdirs: " + output.getPath());

    Candle candle = new Candle(rosettaDir.toPath());
    candle.walk(true);

    // Export to Rosetta
    RosettaRenderer renderer = new RosettaRenderer();
    candle.render(renderer);

    if (rosettaMode == ROSETTA_FORMAT_JSON) {
      renderer.saveJSON(candle.graph, new File(output, "json"));
    } else if (rosettaMode == ROSETTA_FORMAT_YAML) {
      renderer.saveYAML(candle.graph, new File(output, "yml").toPath());
    } else {
      throw new IllegalArgumentException("No rosetta mode selected.");
    }
  }

  private static void mainLua(File output, File rosettaDir) throws IOException {
    if (!output.exists() && !output.mkdirs())
      throw new IOException("Failed to mkdirs: " + output.getPath());

    Candle candle = new Candle(rosettaDir.toPath());
    candle.graph.walkLegacy();

    // Export to Lua
    candle.render(new EmmyLuaRenderer());
    candle.save(output);
  }

  private static void mainTypescript(File output, File rosettaDir) throws IOException {
    if (!output.exists() && !output.mkdirs())
      throw new IOException("Failed to mkdirs: " + output.getPath());

    Candle candle = new Candle(rosettaDir.toPath());
    candle.graph.walkLegacy();

    candle.render(new TypescriptRenderer());

    // We compile all classes into a package-scope list.
    final Map<Package, List<CandleClass>> classMap = new HashMap<>();
    for (CandleClass clazz : candle.graph.classesSorted) {
      Package pkg = clazz.getClazz().getPackage();
      classMap.computeIfAbsent(pkg, k -> new ArrayList<>()).add(clazz);
    }

    final List<Package> packagesSorted = new ArrayList<>(classMap.keySet());
    packagesSorted.sort(Comparator.comparing(Package::getName));

    final List<String> listReferences = new ArrayList<>();

    File dirJava = new File(output, "java");
    if (!dirJava.exists() && !dirJava.mkdirs()) {
      throw new IOException("Failed to mkdirs: " + dirJava.getPath());
    }

    for (Package pkg : packagesSorted) {
      StringBuilder sb = new StringBuilder();

      List<CandleClass> classes = classMap.get(pkg);
      for (final CandleClass clazz : classes) {
        if (!sb.isEmpty()) {
          sb.append("\n\n");
        }
        sb.append(clazz.getRenderedCode());
      }

      final String fileCode =
          TypescriptRenderer.wrapAsTSFile(
              TypescriptRenderer.wrapAsTSNamespace(pkg.getName(), sb.toString()));

      final String fileName = pkg.getName().replaceAll("\\.", "_") + ".d.ts";
      listReferences.add(fileName);
      final File file = new File(dirJava, fileName);
      System.out.println("Writing file: " + file.getPath());
      CandleGraph.write(file, fileCode);
    }

    final File fileReference = new File(output, "java.reference.partial.d.ts");
    System.out.println("Writing reference file: " + fileReference.getPath());

    StringBuilder sb = new StringBuilder();
    for (String fileName : listReferences) {
      sb.append("/// <reference path=\"java/").append(fileName).append("\" />\n");
    }

    CandleGraph.write(fileReference, sb.toString());

    // Write Lua interface.
    sb = new StringBuilder();

    sb.append("local Exports = {};\n\n");
    sb.append("-- [PARTIAL:START]\n");
    for (CandleClass clazz : candle.graph.classesSorted) {
      String name = clazz.getLuaName();
      sb.append("Exports.")
          .append(name)
          .append(" = loadstring(\"return _G['")
          .append(name)
          .append("']\")();\n");
    }
    sb.append("-- [PARTIAL:STOP]\n\n");
    sb.append("return Exports;\n");

    File fileClassAPI = new File(output, "java.api.partial.lua");
    CandleGraph.write(fileClassAPI, sb.toString());

    sb = new StringBuilder();

    sb.append("// [PARTIAL:START]\n");
    for (CandleClass clazz : candle.graph.classesSorted) {
      String name = clazz.getLuaName();
      sb.append("/** @customConstructor ").append(name).append(".new */\n");
      sb.append("export class ")
          .append(name)
          .append(" extends ")
          .append(clazz.getClazz().getPackage().getName());
    }
  }

  private static void mainPy(File outputDir, File rosettaDir) throws IOException {
    addSubClasses = true;
    addSuperClasses = true;
    addParameterClasses = true;
    addReturnClasses = true;

    ClassLoadParser parser = new ClassLoadParser();
    parser.parse("classes.txt");

    if (!outputDir.exists() && !outputDir.mkdirs())
      throw new IOException("Failed to mkdirs: " + outputDir.getPath());

    Candle candle = new Candle(rosettaDir.toPath());
    for (Class<?> clazz : parser.classes) {
      candle.graph.addClass(clazz);
    }

    candle.walk(false);

    // Export to Python
    PythonTypingsRenderer.DIR_OUTPUT = outputDir;
    PythonTypingsRenderer renderer = new PythonTypingsRenderer();
    renderer.render(candle.graph);
  }

  public Candle(Path rosettaPath) {
    graph = new CandleGraph(rosettaPath);
  }

  public Candle() {
    this(null);
  }
}
