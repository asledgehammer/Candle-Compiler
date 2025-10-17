package com.asledgehammer.candle.java.reference;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@SuppressWarnings("unused")
public class ReturnReference {

  private final MethodReference methodReference;
  private final TypeReference resolvedType;
  private final Class<?> type;
  private final Type genericType;

  ReturnReference(
      @NotNull MethodReference methodReference, @NotNull Class<?> type, @NotNull Type genericType) {
    this.methodReference = methodReference;
    this.type = type;
    this.genericType = genericType;

    ClassReference classReference = methodReference.getClassReference();
    Class<?> deCl = methodReference.getMethod().getDeclaringClass();
    this.resolvedType = classReference.resolveType(genericType, deCl);
  }

  @NotNull
  public TypeReference getResolvedType() {
    return resolvedType;
  }

  @NotNull
  public MethodReference getMethodReference() {
    return methodReference;
  }

  @NotNull
  public Type getGenericType() {
    return genericType;
  }

  @NotNull
  public Class<?> getType() {
    return type;
  }
}
