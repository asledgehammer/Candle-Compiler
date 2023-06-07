package com.asledgehammer.candle;

import com.asledgehammer.rosetta.RosettaClass;
import com.asledgehammer.rosetta.RosettaField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class CandleField extends CandleEntity<CandleField> {

  private final boolean bPublic;
  private final boolean bStatic;

  @NotNull private final String name;

  @NotNull private final Field field;
  private final CandleClass candleClass;

  @Nullable private RosettaField docs;

  CandleField(@NotNull CandleClass candleClass, @NotNull Field field) {
    super(field.getType(), field.getName());

    this.candleClass = candleClass;

    int modifiers = field.getModifiers();
    this.bStatic = Modifier.isStatic(modifiers);
    this.bPublic = Modifier.isPublic(modifiers);
    this.name = field.getName();
    this.field = field;
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    RosettaClass yamlFile = candleClass.getDocs();
    if (yamlFile != null) {
      docs = yamlFile.getField(field.getName());
    }
  }

  public String getName() {
    return name;
  }

  public boolean isPublic() {
    return bPublic;
  }

  public boolean isStatic() {
    return bStatic;
  }

  public RosettaField getDocs() {
    return this.docs;
  }

  public boolean hasDocs() {
    return this.docs != null;
  }
}
