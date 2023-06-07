package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings("unused")
public class RosettaType extends RosettaEntity {

  private final String rawBasic;
  private final String basic;
  private final String full;

  public RosettaType(@NotNull Map<String, Object> raw) {
    super(raw);
    String basic = readRequiredString("basic");
    this.rawBasic = basic;

    if(basic.contains(".")) {
      String[] split = basic.split("\\.");
      this.basic = split[split.length - 1];
    } else {
      this.basic = basic;
    }

    this.full = readString("full");
  }

  @Override
  public String toString() {
    return "RosettaType{" + "basic='" + basic + '\'' + ", full='" + full + '\'' + '}';
  }

  @NotNull
  public String getBasic() {
    return this.basic;
  }

  @Nullable
  public String getFull() {
    return this.full;
  }

  @NotNull
  public String getRawBasic() {
    return this.rawBasic;
  }

  public boolean hasFull() {
    return this.full != null;
  }
}
