package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;
import java.util.*;

import static com.asledgehammer.candle.impl.PythonTypingsRenderer.ILLEGAL;

public class PythonTypingsRenderer {

  public static List<String> ILLEGAL;

  public static final Map<String, Class<?>> DISCOVERED_CLASSES = new HashMap<>();

  static {
    ILLEGAL =
        new ArrayList<>(
            Arrays.stream(
                    new String[] {
                      "False",
                      "def",
                      "if",
                      "raise",
                      "None",
                      "del",
                      "import",
                      "return",
                      "True",
                      "elif",
                      "in",
                      "try",
                      "and",
                      "else",
                      "is",
                      "while",
                      "as",
                      "except",
                      "lambda",
                      "with",
                      "assert",
                      "finally",
                      "nonlocal",
                      "yield",
                      "break",
                      "for",
                      "not",
                      "class",
                      "from",
                      "or",
                      "continue",
                      "global",
                      "pass"
                    })
                .toList());
  }

  Map<Package, PythonFile> mapFiles = new HashMap<>();

  static File dir;

  public void render(CandleGraph graph) {

    dir = new File("python/typings/");
    if (!dir.exists()) dir.mkdirs();
    List<Class<?>> clazzes = new ArrayList<>(graph.classes.keySet());
    clazzes.sort(Comparator.comparing(Class::getSimpleName));
    for (Class<?> clazz : clazzes) {
      if (clazz.getDeclaringClass() == null) {
        renderClass(graph, graph.classes.get(clazz));
      }
    }

    for (Package pkg : mapFiles.keySet()) {
      mapFiles.get(pkg).write();
    }
  }

  public void renderClass(CandleGraph graph, CandleClass clazz) {
    Package pkg = clazz.getClazz().getPackage();

    if (pkg == null) return;

    PythonFile file;
    if (mapFiles.containsKey(pkg)) {
      file = mapFiles.get(pkg);
    } else {
      file = new PythonFile(pkg);
      mapFiles.put(pkg, file);
    }

    file.lines.addAll(file.digestClass(graph, clazz, 0));
  }
}

