package com.asledgehammer.candle;

import com.asledgehammer.candle.impl.EmmyLuaRenderer;
import zombie.Lua.LuaManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

class Candle {

  final CandleGraph graph = new CandleGraph();

  void walk() {
    this.graph.walk();
  }

  void render(CandleRenderAdapter adapter) {
    this.graph.render(adapter);
  }

  void save(File dir) {
    saveJavaAPI(dir);
    saveGlobalAPI(dir);
    this.graph.save(dir);
  }

  void saveGlobalAPI(File dir) {
    CandleClass candleGlobalObject = this.graph.classes.get(LuaManager.GlobalObject.class);
    Map<String, CandleExecutableCluster<CandleMethod>> methods =
        candleGlobalObject.getStaticMethods();
    List<String> keysSorted = new ArrayList<>(methods.keySet());
    keysSorted.sort(Comparator.naturalOrder());
    StringBuilder builder = new StringBuilder();
    for (String methodName : keysSorted) {
      CandleExecutableCluster<CandleMethod> cluster = methods.get(methodName);
      builder
          .append("\n")
          .append(cluster.getRenderedCode().replaceAll("GlobalObject.", ""))
          .append("\n");
    }
    CandleGraph.write(new File(dir, "__global.lua"), builder.toString());
  }

  public void saveJavaAPI(File dir) {
    String fileContents =
        """
        --- @alias byte number
        --- @alias short number
        --- @alias int number
        --- @alias char string
        --- @alias float number
        --- @alias double number
        --- @alias long number
        --- @alias void nil
        --- @alias Unknown Object
        --- @alias Object any
        --- @alias Void void
        --- @alias Boolean boolean
        --- @alias Short short
        --- @alias Integer int
        --- @alias Float float
        --- @alias Double double
        --- @alias Long long
        --- @alias BigInt number
        --- @alias Character string
        --- @alias String string
        --- @alias KahluaTable table
        """;

    CandleGraph.write(new File(dir, "__java.lua"), fileContents);
  }

  public static void main(String[] yargs) throws IOException {

    String path = "./dist/";
    if (yargs.length != 0) {
      path = yargs[1];
    }

    File dir = new File(path);
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("Failed to mkdirs: " + path);
    }

    Candle candle = new Candle();
    candle.walk();
    candle.render(new EmmyLuaRenderer());
    candle.save(dir);
  }
}
