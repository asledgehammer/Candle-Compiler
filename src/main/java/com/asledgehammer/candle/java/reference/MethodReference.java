package com.asledgehammer.candle.java.reference;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

@SuppressWarnings("unused")
public class MethodReference implements BoundReference {

  private final ParameterReference[] parameterReferences;
  private final TypeReference[] bounds;
  private final ClassReference classReference;
  private final ReturnReference returnReference;
  private final Method method;

  MethodReference(@NotNull ClassReference classReference, @NotNull Method method) {
    this.classReference = classReference;
    this.method = method;

    TypeVariable<?>[] vars = this.method.getTypeParameters();
    this.bounds = new TypeReference[vars.length];
    for (int i = 0; i < vars.length; i++) {
      this.bounds[i] = TypeReference.wrap(vars[i]);
    }

    // Compile parameter(s).
    Parameter[] p = method.getParameters();
    int length = p.length;
    parameterReferences = new ParameterReference[length];
    if (p.length != 0) {
      Type[] gp = method.getGenericParameterTypes();
      for (int i = 0; i < length; i++) {
        Parameter parameter = p[i];
        Type genericType = gp[i];
        parameterReferences[i] = new ParameterReference(this, parameter, genericType);
      }
    }

    // Compile return.
    Class<?> rt = method.getReturnType();
    Type grt = method.getGenericReturnType();
    this.returnReference = new ReturnReference(this, rt, grt);
  }

  @Override
  public TypeReference[] getBounds() {
    return this.bounds;
  }

  @NotNull
  public ParameterReference[] getParameterReferences() {
    return parameterReferences;
  }

  @NotNull
  public ReturnReference getReturnReference() {
    return returnReference;
  }

  @NotNull
  public ClassReference getClassReference() {
    return classReference;
  }

  @NotNull
  public Method getMethod() {
    return method;
  }
}
