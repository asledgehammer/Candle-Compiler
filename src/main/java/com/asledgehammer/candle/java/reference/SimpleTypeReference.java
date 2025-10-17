package com.asledgehammer.candle.java.reference;

import java.util.ArrayList;
import java.util.List;

public class SimpleTypeReference extends TypeReference {

  static final TypeReference[] OBJECT_BOUNDS;

  static {
    OBJECT_BOUNDS = new TypeReference[] {TypeReference.wrap(Object.class)};
  }

  private final List<TypeReference> subTypes;
  private final String base;
  private final boolean wildcard;
  private final boolean primitive;
  private final boolean generic;
  private final TypeReference[] bounds;

  SimpleTypeReference(String raw) {
    if (raw.contains("<")) {
      this.base = raw.substring(0, raw.indexOf('<'));
      List<String> subTypesStr = getGenericTypes(raw);
      subTypes = new ArrayList<>();
      for (String subTypeStr : subTypesStr) {
        subTypes.add(TypeReference.wrap(subTypeStr));
      }
    } else {
      String base = raw.trim();
      if (base.contains(" extends ")) {
        this.base = base.split(" extends ")[0];
      } else if (base.contains(" super ")) {
        this.base = base.split(" super ")[0];
      } else {
        this.base = base;
      }
      this.subTypes = null;
    }

    this.wildcard = this.base.equals("?");
    this.primitive = PRIMITIVE_TYPES.contains(this.base);
    boolean generic = this.wildcard;
    if (!generic && !this.primitive) {
      // Attempt to resolve the path. if it doesn't exist then it's considered generic.
      try {
        Class.forName(this.base, false, ClassLoader.getSystemClassLoader());
      } catch (Exception e) {
        generic = true;
      }
    }
    this.generic = generic;
    this.bounds = this.generic ? OBJECT_BOUNDS : new TypeReference[] {this};
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

  @Override
  public boolean isWildcard() {
    return wildcard;
  }

  @Override
  public boolean isGeneric() {
    return generic;
  }

  @Override
  public String getBase() {
    return this.base;
  }

  public boolean isPrimitive() {
    return primitive;
  }

  @Override
  public TypeReference[] getBounds() {
    return bounds;
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
}
