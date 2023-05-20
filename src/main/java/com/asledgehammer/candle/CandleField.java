package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class CandleField extends CandleEntity<CandleField> {

  private final boolean bPublic;
  private final boolean bStatic;
  private final String name;

  CandleField(@NotNull Field field) {
    super(field.getType(), field.getName());

    int modifiers = field.getModifiers();
    this.bStatic = Modifier.isStatic(modifiers);
    this.bPublic = Modifier.isPublic(modifiers);
    this.name = field.getName();
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {}

  public String getName() {
    return name;
  }

  public boolean isPublic() {
    return bPublic;
  }

  public boolean isStatic() {
    return bStatic;
  }
}
