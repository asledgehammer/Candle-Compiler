package com.asledgehammer.candle.impl;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

import static com.asledgehammer.candle.CandleUtils.commaSplit;

public class PythonUtils {

  public static String getFullClassType(Class<?> clazz) {
    TypeVariable<? extends Class<?>>[] parameters = clazz.getTypeParameters();
    if (parameters.length == 0) {
      return clazz.getTypeName();
    }

    // Build generic arguments(s).
    StringBuilder s = new StringBuilder(clazz.getSimpleName() + '<');
    for (TypeVariable<? extends Class<?>> parameter : parameters) {

      // Type name.
      String p = parameter.getName();

      // extends
      Type[] bounds = parameter.getBounds();
      if (bounds.length != 0) {
        String extendz = bounds[0].getTypeName();
        if (!extendz.equals("java.lang.Object")) {
          p += " extends " + bounds[0].getTypeName();
        }
      }

      s.append(p).append(", ");
    }
    s = new StringBuilder(s.substring(0, s.length() - 2) + '>');
    return s.toString();
  }

  public static String asBasicType(String fullType) {
    return recurseStrip(fullType).replaceAll("\\$", ".");
  }

  private static String recurseStrip(String fullType) {

    fullType = fullType.trim();

    // Easy parsing. No recursion needed.
    if (!fullType.contains("<")) {
      if (fullType.contains(" extends ")) {
        String[] split = fullType.split(" extends ");
        String paramName = split[0].trim();
        String path = split[1].trim();
        split = path.split("\\.");
        return paramName + " extends " + split[split.length - 1];
      } else {
        if (fullType.contains(".")) {
          String[] split = fullType.split("\\.");
          return split[split.length - 1];
        }
      }
      return fullType;
    }

    // Split the main type from the generic arguments.
    int indexCarrot = fullType.indexOf('<');
    String mainType = fullType.substring(0, indexCarrot);
    if (mainType.contains(".")) {
      if (mainType.contains(" extends ")) {
        String[] split = mainType.split(" extends ");
        String paramName = split[0].trim();
        String path = split[1].trim();
        split = path.split("\\.");
        mainType = paramName + " extends " + split[split.length - 1];
      } else {
        if (mainType.contains(".")) {
          String[] split = mainType.split("\\.");
          mainType = split[split.length - 1];
        }
      }
    }

    // Build generic arguments.
    System.out.println("fullType: " + fullType);
    String genArgsString = fullType.substring(indexCarrot + 1, fullType.length() - 1);
    List<String> genArgs = commaSplit(genArgsString);
    StringBuilder s = new StringBuilder(mainType + '[');
    for (String genArg : genArgs) {
      genArg = adapt(genArg);
      // Recursively feed the generic arguments for possible nested generics.
      s.append(recurseStrip(genArg)).append(", ");
    }
    s = new StringBuilder(s.substring(0, s.length() - 2) + ']');
    return s.toString();
  }

  public static String adapt(String o) {
    return switch (o) {
      case "String" -> "str";
      case "Object" -> "object";
      case "byte", "short", "int" -> "int";
      case "long" -> "long";
      case "float", "double" -> "float";
      default ->
          o.replaceAll("\\? extends ", "")
              .replaceAll("\\? super ", "")
              .replaceAll("<\\?>", "")
              .replaceAll("\\[]", "");
    };
  }

}
