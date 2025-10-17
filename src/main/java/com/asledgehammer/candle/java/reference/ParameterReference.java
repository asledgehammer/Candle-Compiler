package com.asledgehammer.candle.java.reference;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

@SuppressWarnings("unused")
public class ParameterReference {

  private final MethodReference methodReference;
  private final Parameter parameter;
  private final Type genericType;
  private final TypeReference resolvedType;

  ParameterReference(MethodReference methodReference, Parameter parameter, Type genericType) {
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
