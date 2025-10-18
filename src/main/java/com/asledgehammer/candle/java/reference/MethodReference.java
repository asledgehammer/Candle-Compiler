package com.asledgehammer.candle.java.reference;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class MethodReference extends ExecutableReference<Method> implements BoundReference {

  private final ReturnReference returnReference;

  MethodReference(@NotNull ClassReference classReference, @NotNull Method method) {
    super(classReference, method);

    // Compile return.
    this.returnReference =
        new ReturnReference(this, method.getReturnType(), method.getGenericReturnType());
  }

  @NotNull
  public ReturnReference getReturnReference() {
    return returnReference;
  }
}
