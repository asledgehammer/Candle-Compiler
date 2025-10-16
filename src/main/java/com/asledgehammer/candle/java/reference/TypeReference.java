package com.asledgehammer.candle.java.reference;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeReference {

  private static final Map<String, TypeReference> BANK = new HashMap<>();

  private final List<TypeReference> subTypes;
  private final String base;
  private final boolean wildcard;

  private final TypeReference lower;
  private final TypeReference upper;

  private TypeReference(String raw) {
    if (raw.contains("<")) {
      String base = raw.substring(0, raw.indexOf('<'));
      if (base.contains(" extends ")) {
        String[] split = base.split(" extends ");
        this.base = split[0];
        this.upper = this;
        this.lower = TypeReference.wrap(split[1]);
      } else if (base.contains(" super ")) {
        String[] split = base.split(" super ");
        this.base = split[0];
        this.upper = TypeReference.wrap(split[1]);
        this.lower = this;
      } else {
        this.base = base;
        this.lower = this;
        this.upper = this;
      }
      List<String> subTypesStr = getGenericTypes(raw);
      subTypes = new ArrayList<>();
      for (String subTypeStr : subTypesStr) {
        subTypes.add(new TypeReference(subTypeStr));
      }
    } else {
      String base = raw.trim();
      if (base.contains(" extends ")) {
        String[] split = base.split(" extends ");
        this.base = split[0];
        this.lower = this;
        this.upper = TypeReference.wrap(split[1]);
      } else if (base.contains(" super ")) {
        String[] split = base.split(" super ");
        this.base = split[0];
        this.lower = TypeReference.wrap(split[1]);
        this.upper = this;
      } else {
        this.base = base;
        this.lower = this;
        this.upper = this;
      }
      this.subTypes = null;
    }
    this.wildcard = this.base.equals("?");
//    System.out.println("TypeReference(" + base + ") -> " + subTypes);
  }

  public String compile() {
    String compiled = this.base;
    if (subTypes != null) {
      StringBuilder subTypeStr = new StringBuilder();
      for (TypeReference subType : subTypes) {
        if (subTypeStr.isEmpty()) {
          subTypeStr = new StringBuilder(subType.compile());
        } else {
          subTypeStr.append(", ").append(subType.compile());
        }
      }
      compiled += '<' + subTypeStr.toString() + '>';
    }
    return compiled;
  }

  public String compile(ClassReference reference, Class<?> deCl) {
    String compiled = reference.resolveType(this, deCl).compile();
    if (subTypes != null) {
      StringBuilder subTypeStr = new StringBuilder();
      for (TypeReference subType : subTypes) {
        if (subTypeStr.isEmpty()) {
          subTypeStr = new StringBuilder(subType.compile(reference, deCl));
        } else {
          subTypeStr.append(", ").append(subType.compile(reference, deCl));
        }
      }
      compiled += '<' + subTypeStr.toString() + '>';
    }
    return compiled;
  }

  public TypeReference getLower() {
    return lower;
  }

  public TypeReference getUpper() {
    return upper;
  }

  public boolean isWildcard() {
    return wildcard;
  }

  public static List<String> getGenericTypes(String raw) {
    int level = 0;
    final List<String> vars = new ArrayList<>();
    StringBuilder var = new StringBuilder();
    for (int x = 0; x < raw.length(); x++) {
      char curr = raw.charAt(x);
      if (curr == '<') {
        level++;
        if (level > 1) {
          var.append(curr);
        }
      } else if (curr == '>') {
        level--;
        if (level > 0) {
          var.append(curr);
        } else {
          break;
        }
      } else if (curr == ',' && level == 1) {
        vars.add(var.toString().trim());
        var = new StringBuilder();
      } else if (level != 0) {
        var.append(curr);
      }
    }

    if (!var.isEmpty()) {
      vars.add(var.toString().trim());
    }

    return vars;
  }

  public String getBase() {
    return this.base;
  }

  @Override
  public String toString() {
    return "TypeReference(" + compile() + ")";
  }

  public static TypeReference wrap(TypeVariable<?> type) {
    return wrap(type.getTypeName());
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

    TypeReference reference = new TypeReference(rawType);
    BANK.put(rawType, reference);

    return reference;
  }

  public static void clearCache() {
    BANK.clear();
  }

  public static void main(String[] args) {
    new TypeReference("List<Map<String, Integer>>");
  }
}
