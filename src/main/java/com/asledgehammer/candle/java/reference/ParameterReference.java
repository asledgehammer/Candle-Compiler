package com.asledgehammer.candle.java.reference;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

@SuppressWarnings("unused")
public class ParameterReference {

  private final MethodReference methodReference;
  private final TypeReference resolvedType;
  private final Parameter parameter;
  private final Type genericType;

  ParameterReference(
      @NotNull MethodReference methodReference,
      @NotNull Parameter parameter,
      @NotNull Type genericType) {
    this.methodReference = methodReference;
    this.parameter = parameter;
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

  public Parameter getParameter() {
    return parameter;
  }

  public Type getGenericType() {
    return genericType;
  }
}
