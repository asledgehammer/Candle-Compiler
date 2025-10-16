package com.asledgehammer.candle.java.reference;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

public class MethodReference {

  private final ClassReference classReference;
  private final Method method;

  private final ParameterReference[] parameterReferences;
  private final ReturnReference returnReference;

  MethodReference(ClassReference classReference, Method method) {
    this.classReference = classReference;
    this.method = method;

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
