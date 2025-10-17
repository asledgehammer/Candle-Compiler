package com.asledgehammer.candle.java.reference;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

public class MethodReference implements BoundReference {

  private final ClassReference classReference;
  private final Method method;

  private final ParameterReference[] parameterReferences;
  private final ReturnReference returnReference;
  private final TypeReference[] bounds;

  MethodReference(ClassReference classReference, Method method) {
    this.classReference = classReference;
    this.method = method;

    TypeVariable<?>[] vars = this.method.getTypeParameters();
    this.bounds = new TypeReference[vars.length];
    for (int i = 0; i < vars.length; i++) {
      this.bounds[i] = TypeReference.wrap(vars[i]);
    }
    System.out.println(method + " -> " + Arrays.toString(this.bounds));

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

  public ParameterReference[] getParameterReferences() {
    return parameterReferences;
  }

  public ReturnReference getReturnReference() {
    return returnReference;
  }

  public ClassReference getClassReference() {
    return classReference;
  }

  public Method getMethod() {
    return method;
  }
}
