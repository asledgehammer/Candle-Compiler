package com.asledgehammer.candle.java.reference;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

@SuppressWarnings("unused")
public class CandleReference {

  public static void clearCache() {
    TypeReference.clearCache();
    ClassReference.clearCache();
  }

  @NotNull
  public static ClassReference wrap(@NotNull Class<?> clazz) {
    return ClassReference.wrap(clazz);
  }

  @NotNull
  public static TypeReference wrap(@NotNull TypeVariable<?> typeVariable) {
    return TypeReference.wrap(typeVariable);
  }

  @NotNull
  public static TypeReference wrap(@NotNull Type type) {
    return TypeReference.wrap(type);
  }

  @NotNull
  public static FieldReference wrap(@NotNull Field field) {
    ClassReference classReference = wrap(field.getDeclaringClass());
    return classReference.getFieldReference(field);
  }

  @NotNull
  public static MethodReference wrap(@NotNull Method method) {
    ClassReference classReference = wrap(method.getDeclaringClass());
    return classReference.getMethodReference(method);
  }
}
