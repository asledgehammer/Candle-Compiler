package com.asledgehammer.candle.java.reference;

import java.lang.reflect.*;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ConstructorReference extends ExecutableReference<Constructor<?>>
    implements BoundReference {

  ConstructorReference(
      @NotNull ClassReference classReference, @NotNull Constructor<?> constructor) {
    super(classReference, constructor);
  }
}
