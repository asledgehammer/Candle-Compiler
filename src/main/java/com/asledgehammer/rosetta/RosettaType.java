package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings("unchecked")
public class RosettaType extends RosettaEntity {

  private final String basic;
  private final String full;

  public RosettaType(@NotNull Map<String, Object> raw) {
    super(raw);

    this.basic = readRequiredString("basic");
    this.full = readString("full");
  }

  @NotNull
  public String getBasic() {
    return this.basic;
  }

  @Nullable
  public String getFull() {
    return this.full;
  }

  public boolean hasFull() {
    return this.full != null;
  }
}
