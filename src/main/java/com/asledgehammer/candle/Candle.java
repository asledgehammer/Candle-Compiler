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

  void walk(boolean clear) {
    this.graph.walk(clear);
  }

  void render(CandleRenderAdapter adapter) {
    this.graph.render(adapter);
  }

  void save(File dir, String extension) throws IOException {
    saveJavaAPI(dir);
    saveGlobalAPI(dir, extension);
    this.graph.save(dir, extension);
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

  public static void main(String[] yargs) throws IOException {
    //    mainPy(yargs);
        mainLua(yargs);
//    mainTypescript(yargs);
  }

  private static void mainRosetta(String[] yargs) throws IOException {
    String path = "./dist/";
    if (yargs.length != 0) path = yargs[0];
    Path rosettaPath = Path.of("rosetta", "yml");
    if (yargs.length > 1) {
      rosettaPath = Path.of(yargs[1]);
    }

    File dir = new File(path);
    if (!dir.exists() && !dir.mkdirs()) throw new IOException("Failed to mkdirs: " + path);

    Candle candle = new Candle(rosettaPath);
    candle.walk(true);

    // Export to Rosetta
    RosettaRenderer renderer = new RosettaRenderer();
    candle.render(renderer);
    renderer.saveJSON(candle.graph, new File("./dist2/json/"));
    renderer.saveYAML(candle.graph, Path.of("./dist2/yml/"));
  }

  private static void mainLua(String[] yargs) throws IOException {
    // any more arguments and this should probably be replaced with 'proper' handling
    String path = "./dist/";
    if (yargs.length != 0) path = yargs[0];
    Path rosettaPath = Path.of("rosetta", "json");
    if (yargs.length > 1) {
      rosettaPath = Path.of(yargs[1]);
    }

    File dir = new File(path);
    if (!dir.exists() && !dir.mkdirs()) throw new IOException("Failed to mkdirs: " + path);

    Candle candle = new Candle(rosettaPath);
    candle.graph.walkLegacy();

    // Export to Lua
    candle.render(new EmmyLuaRenderer());
    candle.save(dir, "lua");
  }

  private static void mainTypescript(String[] yargs) throws IOException {
    // any more arguments and this should probably be replaced with 'proper' handling
    String path = "./dist/";
    if (yargs.length != 0) path = yargs[0];
    Path rosettaPath = Path.of("rosetta", "yml");
    if (yargs.length > 1) {
      rosettaPath = Path.of(yargs[1]);
    }

    File dir = new File(path);
    if (!dir.exists() && !dir.mkdirs()) throw new IOException("Failed to mkdirs: " + path);

    Candle candle = new Candle(rosettaPath);
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

    File dirJava = new File(dir, "java");
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

    final File fileReference = new File(dir, "java.reference.partial.d.ts");
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
        sb.append("Exports.").append(name).append(" = loadstring(\"return _G['").append(name).append("']\")();\n");
      }
    sb.append("-- [PARTIAL:STOP]\n\n");
    sb.append("return Exports;\n");

    File fileClassAPI = new File(dir, "java.api.partial.lua");
    CandleGraph.write(fileClassAPI, sb.toString());

    sb = new StringBuilder();

    sb.append("// [PARTIAL:START]\n");
    for (CandleClass clazz : candle.graph.classesSorted) {
      String name = clazz.getLuaName();
      sb.append("/** @customConstructor ").append(name).append(".new */\n");
      sb.append("export class " + name + " extends " + clazz.getClazz().getPackage().getName());
    }
  }

  private static void mainPy(String[] yargs) throws IOException {
    addSubClasses = true;
    addSuperClasses = true;
    addParameterClasses = true;
    addReturnClasses = true;

    ClassLoadParser parser = new ClassLoadParser();
    parser.parse("classes.txt");

    String path = "./dist/";
    if (yargs.length != 0) path = yargs[0];

    File dir = new File(path);
    if (!dir.exists() && !dir.mkdirs()) throw new IOException("Failed to mkdirs: " + path);

    Candle candle = new Candle(null);
    for (Class<?> clazz : parser.classes) {
      candle.graph.addClass(clazz);
    }

    // PythonBag.addClasses(candle.graph);

    candle.walk(false);

    // Export to Python
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
