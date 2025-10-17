package com.asledgehammer.candle.java.reference;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class UnionTypeReference extends TypeReference implements BoundReference {

  private final TypeReference[] bounds;
  private final String base;
  private final boolean extendsOrSuper;
  private final boolean wildcard;
  private final boolean primitive;
  private final boolean generic;

  UnionTypeReference(@NotNull String base, boolean extendsOrSuper, @NotNull TypeReference[] bounds) {
    this.base = base;
    this.extendsOrSuper = extendsOrSuper;
    this.bounds = bounds;
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
  }

  @NotNull
  @Override
  public String compile() {
    StringBuilder builder = new StringBuilder(this.base);
    if (this.extendsOrSuper) {
      builder.append(" extends ");
    } else {
      builder.append(" super ");
    }
    for (int i = 0; i < this.bounds.length; i++) {
      TypeReference reference = this.bounds[i];
      if (i != 0) builder.append(" & ");
      builder.append(reference.compile());
    }
    return builder.toString();
  }

  @NotNull
  @Override
  public String compile(@NotNull ClassReference clazzReference, @NotNull Class<?> deCl) {
    StringBuilder builder = new StringBuilder(this.base);
    if (this.extendsOrSuper) {
      builder.append(" extends ");
    } else {
      builder.append(" super ");
    }
    for (int i = 0; i < this.bounds.length; i++) {
      TypeReference reference = this.bounds[i];
      if (i != 0) builder.append(" & ");
      builder.append(reference.compile(clazzReference, deCl));
    }
    return builder.toString();
  }

  @Override
  public boolean isWildcard() {
    return wildcard;
  }

  @Override
  public boolean isGeneric() {
    return generic;
  }

  @NotNull
  @Override
  public String getBase() {
    return this.base;
  }

  @Override
  public boolean isPrimitive() {
    return primitive;
  }

  @NotNull
  @Override
  public TypeReference[] getBounds() {
    return bounds;
  }

  public boolean isExtendsOrSuper() {
    return extendsOrSuper;
  }
}
