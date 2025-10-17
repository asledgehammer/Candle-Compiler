package com.asledgehammer.candle.java.reference;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public abstract class TypeReference {

  private static final Map<Type, TypeReference> CACHE = new HashMap<>();

  static final List<String> PRIMITIVE_TYPES;
  static final TypeReference OBJECT_TYPE;
  static final TypeReference[] OBJECT_TYPE_MAP;

  static {
    PRIMITIVE_TYPES = new ArrayList<>();
    PRIMITIVE_TYPES.add("void");
    PRIMITIVE_TYPES.add("boolean");
    PRIMITIVE_TYPES.add("byte");
    PRIMITIVE_TYPES.add("short");
    PRIMITIVE_TYPES.add("char");
    PRIMITIVE_TYPES.add("int");
    PRIMITIVE_TYPES.add("float");
    PRIMITIVE_TYPES.add("double");
    PRIMITIVE_TYPES.add("long");

    OBJECT_TYPE = wrap(Object.class);
    OBJECT_TYPE_MAP = new TypeReference[] {OBJECT_TYPE};
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(compile() = " + compile() + ")";
  }

  @NotNull
  public abstract String getBase();

  @NotNull
  public abstract String compile();

  @NotNull
  public abstract String compile(@NotNull ClassReference reference, @NotNull Class<?> deCl);

  public abstract boolean isGeneric();

  public abstract boolean isWildcard();

  public abstract boolean isPrimitive();

  @NotNull
  public abstract TypeReference[] getBounds();

  @NotNull
  public static TypeReference wrap(@NotNull TypeVariable<?> type) {
    if (CACHE.containsKey(type)) return CACHE.get(type);
    Type[] bounds = type.getBounds();
    TypeReference[] trBounds = new TypeReference[bounds.length];
    for (int i = 0; i < bounds.length; i++) {
      trBounds[i] = wrap(bounds[i]);
    }
    TypeReference reference = new UnionTypeReference(type.getTypeName(), true, trBounds);
    CACHE.put(type, reference);
    return reference;
  }

  @NotNull
  public static TypeReference wrap(@NotNull Type type) {
    if (CACHE.containsKey(type)) return CACHE.get(type);
    TypeReference reference = wrap(type.getTypeName());
    CACHE.put(type, reference);
    return reference;
  }

  @NotNull
  public static TypeReference wrap(@NotNull Class<?> clazz) {
    if (CACHE.containsKey(clazz)) {
      return CACHE.get(clazz);
    }
    TypeReference reference = wrap(clazz.getTypeName());
    CACHE.put(clazz, reference);
    return reference;
  }

  @NotNull
  public static TypeReference wrap(@NotNull String rawType) {
    // No need to iterate.
    if (!rawType.contains("&")) {
      TypeReference reference = new SimpleTypeReference(rawType);
      if (reference.isGeneric()) {
        reference = new UnionTypeReference(reference.getBase(), true, OBJECT_TYPE_MAP);
      }
      return reference;
    }

    String base;
    String sub;
    boolean extendsOrSuper = rawType.contains(" extends ");
    if (extendsOrSuper) {
      base = rawType.substring(0, rawType.indexOf(" extends ") - 1);
      sub = rawType.substring(rawType.indexOf(" extends ") + " extends ".length());
    } else {
      base = rawType.substring(0, rawType.indexOf(" super ") - 1);
      sub = rawType.substring(rawType.indexOf(" super ") + " super ".length());
    }

    List<String> list = getStrings(sub);

    TypeReference[] typeAliases = new TypeReference[list.size()];
    for (int i = 0; i < typeAliases.length; i++) {
      typeAliases[i] = wrap(list.get(i));
    }

    return new UnionTypeReference(base, extendsOrSuper, typeAliases);
  }

  @NotNull
  private static List<String> getStrings(@NotNull String sub) {
    List<String> list = new ArrayList<>();
    int level = 0;
    StringBuilder current = new StringBuilder();
    for (int index = 0; index < sub.length(); index++) {
      char curr = sub.charAt(index);
      if (curr == '<') {
        level++;
      } else if (curr == '>') {
        level--;
      } else if (curr == '&' && level == 0) {
        list.add(current.toString().trim());
        current = new StringBuilder();
        continue;
      }
      current.append(curr);
    }
    if (!current.isEmpty()) {
      list.add(current.toString().trim());
    }
    return list;
  }

  public static void clearCache() {
    CACHE.clear();
  }

  private static class TestType<J, K extends Map<J, String>> extends ArrayList<K> {}

  public static void main(String[] args) {
    TypeReference reference = wrap(TestType.class.getTypeParameters()[1]);
    System.out.println(reference);
  }
}
