package com.asledgehammer.candle;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SubArrayList<S extends String> extends ArrayList<S> {}

public final class CandleUtils {

  private CandleUtils() {
    throw new RuntimeException("Cannot instantiate CandleUtils. (Utility class)");
  }

  public static List<Class<?>> getAllClassTypes(Class<?> clazz) {
    TypeVariable<? extends Class<?>>[] parameters = clazz.getTypeParameters();
    List<Class<?>> clazzes = new ArrayList<>();
    clazzes.add(clazz);

    for (TypeVariable<? extends Class<?>> parameter : parameters) {
      Type[] bounds = parameter.getBounds();
      if (bounds.length != 0) {
        clazzes.addAll(getAllClassTypes((Class<?>) bounds[0]));
      }
    }

    return clazzes;
  }

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
        String extendz = getFullType(bounds[0]);
        if (!extendz.equals("java.lang.Object")) {
          p += " extends " + getFullType(bounds[0]);
        }
      }

      s.append(p).append(", ");
    }
    s = new StringBuilder(s.substring(0, s.length() - 2) + '>');
    return s.toString();
  }

  public static String getFullReturnType(Method method) {
    return getFullType(method.getGenericReturnType());
  }

  public static String getFullParameterType(Parameter parameter) {
    return getFullType(parameter.getParameterizedType());
  }

  public static String getFullType(Field field) {
    return getFullType(field.getGenericType());
  }

  public static String getFullType(Type type) {
    return type.getTypeName();
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
//    System.out.println("fullType: " + fullType);
    String genArgsString = fullType.substring(indexCarrot + 1, fullType.length() - 1);
    List<String> genArgs = commaSplit(genArgsString);
    StringBuilder s = new StringBuilder(mainType + '<');
    for (String genArg : genArgs) {
      // Recursively feed the generic arguments for possible nested generics.
      s.append(recurseStrip(genArg)).append(", ");
    }
    s = new StringBuilder(s.substring(0, s.length() - 2) + '>');
    return s.toString();
  }

  public static List<String> extractClasses(String fullType, boolean allowDuplicates) {
    List<String> clazzes = new ArrayList<>();
//    System.out.println(fullType);
    recurseExtract(fullType, clazzes, allowDuplicates);
    return clazzes;
  }

  private static void recurseExtract(
      String fullType, List<String> clazzes, boolean allowDuplicates) {

    if (fullType.contains("<")) {
      int firstIndex = fullType.indexOf('<');
      int lastIndex = fullType.lastIndexOf('>');
      if(lastIndex == -1) {
        lastIndex = fullType.length() - 1;
      }

      String r = fullType.substring(0, firstIndex);
      if (allowDuplicates || !clazzes.contains(r)) {
        clazzes.add(r);
      }

      String subTypes = fullType.substring(firstIndex + 1, lastIndex);
      if (subTypes.contains(",")) {
        String[] split = subTypes.split(",");
        for (String s : split) {
          s = s.trim();
//          System.out.println(s);
          recurseExtract(s, clazzes, allowDuplicates);
        }
      } else {
        recurseExtract(subTypes, clazzes, allowDuplicates);
      }
    } else {
      if (allowDuplicates || !clazzes.contains(fullType)) {
        clazzes.add(fullType);
      }
    }
  }

  public static List<Class<?>> resolveList(List<String> list) {

    List<Class<?>> clazzes = new ArrayList<>();

    for (String s : list) {
      try {
        clazzes.add(Class.forName(s, false, null));
      } catch (ClassNotFoundException e) {
      }
    }

    return clazzes;
  }

  public static List<String> commaSplit(String s) {

    int inside = 0;
    List<String> args = new ArrayList<>();
    String arg = "";

    for (int index = 0; index < s.length(); index++) {
      char c = s.charAt(index);
      if (c == '<') {
        inside++;
      } else if (c == '>') {
        inside--;
      } else if (c == ',' && inside == 0) {
        args.add(arg.trim());
        arg = "";
        continue;
      }
      arg += c;
    }

    if (!arg.isEmpty()) {
      args.add(arg);
    }

    return args;
  }

  private int aInt;
  private Integer aBoxedInt;
  private Map aNoGenMap;
  private Map<String, Object> aGenMap;
  private Map<String, ArrayList<String>> aNestedGenMap;

  public static void main(String[] args) throws Exception {
    Field field_aInt = CandleUtils.class.getDeclaredField("aInt");
    Field field_aBoxedInt = CandleUtils.class.getDeclaredField("aBoxedInt");
    Field field_aNoGenMap = CandleUtils.class.getDeclaredField("aNoGenMap");
    Field field_aGenMap = CandleUtils.class.getDeclaredField("aGenMap");
    Field field_aNestedGenMap = CandleUtils.class.getDeclaredField("aNestedGenMap");

    String fieldType_aInt = getFullType(field_aInt);
    String fieldType_aBoxedInt = getFullType(field_aBoxedInt);
    String fieldType_aNoGenMap = getFullType(field_aNoGenMap);
    String fieldType_aGenMap = getFullType(field_aGenMap);
    String fieldType_aNestedGenMap = getFullType(field_aNestedGenMap);

    //        System.out.println("aInt: ");
    //        System.out.println("Full Type:\t" + fieldType_aInt);
    //        System.out.println("Basic Type:\t" + asBasicType(fieldType_aInt));
    //        System.out.println();
    //        System.out.println("aBoxedInt: ");
    //        System.out.println("Full Type:\t" + fieldType_aBoxedInt);
    //        System.out.println("Basic Type:\t" + asBasicType(fieldType_aBoxedInt));
    //        System.out.println();
    //        System.out.println("aNoGenMap: ");
    //        System.out.println("Full Type:\t" + fieldType_aNoGenMap);
    //        System.out.println("Basic Type:\t" + asBasicType(fieldType_aNoGenMap));
    //        System.out.println();
    //        System.out.println("aGenMap: ");
    //        System.out.println("Full Type:\t" + fieldType_aGenMap);
    //        System.out.println("Basic Type:\t" + asBasicType(fieldType_aGenMap));
    //        System.out.println();
    System.out.println("aNestedGenMap: ");
    System.out.println("Full Type:\t" + fieldType_aNestedGenMap);
    System.out.println("Basic Type:\t" + asBasicType(fieldType_aNestedGenMap));

    //        System.out.println(getFullClassType(Map.class));
    //        System.out.println(getFullClassType(SubArrayList.class));
    //        System.out.println(asBasicType(getFullClassType(SubArrayList.class)));

    //        System.out.println(SubArrayList.class.getGenericSuperclass());

    List<String> clazzPaths = extractClasses(fieldType_aNestedGenMap, false);
    List<Class<?>> clazzes = resolveList(clazzPaths);

    System.out.println(clazzes);
  }
}
