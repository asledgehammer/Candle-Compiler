package com.asledgehammer.candle.java.reference;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TypeReference {

  private static final Map<String, TypeReference> BANK = new HashMap<>();
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

  public abstract String getBase();

  public abstract String compile();

  public abstract String compile(ClassReference reference, Class<?> deCl);

  public abstract boolean isGeneric();

  public abstract boolean isWildcard();

  public abstract boolean isPrimitive();

  public abstract TypeReference[] getBounds();

  public static TypeReference wrap(TypeVariable<?> type) {
    Type[] bounds = type.getBounds();
    TypeReference[] trBounds = new TypeReference[bounds.length];
    for (int i = 0; i < bounds.length; i++) {
      trBounds[i] = wrap(bounds[i]);
    }
    return new UnionTypeReference(type.getTypeName(), true, trBounds);
  }

  public static TypeReference wrap(Type type) {
    return wrap(type.getTypeName());
  }

  public static TypeReference wrap(Class<?> clazz) {
    return wrap(clazz.getTypeName());
  }

  public static TypeReference wrap(String rawType) {
    if (BANK.containsKey(rawType)) {
      return BANK.get(rawType);
    }

    // No need to iterate.
    if (!rawType.contains("&")) {
      TypeReference reference = new SimpleTypeReference(rawType);
      if (reference.isGeneric()) {
        reference = new UnionTypeReference(reference.getBase(), true, OBJECT_TYPE_MAP);
      }

      BANK.put(rawType, reference);
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

    List<TypeReference> types = new ArrayList<>();
    for (String boundType : list) {
      types.add(wrap(boundType));
    }

    return new UnionTypeReference(base, extendsOrSuper, (TypeReference[]) types.toArray());
  }

  public static void clearCache() {
    BANK.clear();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(compile() = " + compile() + ")";
  }

  private static class TestType<K extends String> extends ArrayList<K> {}

  public static void main(String[] args) {
    TypeReference reference = wrap(TestType.class.getTypeParameters()[0]);
    System.out.println(reference);
  }
}
