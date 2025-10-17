package com.asledgehammer.candle.java.reference;

import java.lang.reflect.Field;

public class FieldReference {

  private final ClassReference classReference;
  private final Field field;
  private final TypeReference type;

  FieldReference(ClassReference classReference, Field field) {
    this.classReference = classReference;
    this.field = field;
    this.type = classReference.resolveType(field.getGenericType(), field.getDeclaringClass());
  }

  public ClassReference getClassReference() {
    return classReference;
  }

  public Field getField() {
    return field;
  }
}
