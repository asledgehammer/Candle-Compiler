package com.asledgehammer.candle.java.reference;

import java.lang.reflect.Type;

@SuppressWarnings("unused")
public class ReturnReference {

  private final MethodReference methodReference;
  private final Class<?> type;
  private final Type genericType;

  private final TypeReference resolvedType;

  ReturnReference(MethodReference methodReference, Class<?> type, Type genericType) {
    this.methodReference = methodReference;
    this.type = type;
    this.genericType = genericType;

    ClassReference classReference = methodReference.getClassReference();
    Class<?> deCl = methodReference.getMethod().getDeclaringClass();
    this.resolvedType = classReference.resolveType(genericType, deCl);
  }

  public TypeReference getResolvedType() {
    return resolvedType;
  }

  public MethodReference getMethodReference() {
    return methodReference;
  }

  public Type getGenericType() {
    return genericType;
  }

  public Class<?> getType() {
    return type;
  }
}
