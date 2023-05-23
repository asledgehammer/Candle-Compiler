package com.asledgehammer.candle.yamldoc;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class YamlReturn extends YamlEntity {

  @NotNull String type;
  @Nullable String generic;
  @Nullable String notes;

  YamlReturn(Map<String, Object> raw) {
    super(raw);

    this.type = readString("type", true);
    this.generic = readString("generic");
    this.notes = readString("notes");
  }

  @Override
  public String toString() {
    return "YamlReturn{"
        + "type='"
        + type
        + '\''
        + ", generic='"
        + generic
        + '\''
        + ", notes='"
        + notes
        + '\''
        + '}';
  }

  @NotNull
  public String getType() {
    return type;
  }

  @Nullable
  public String getGeneric() {
    return generic;
  }

  @Nullable
  public String getNotes() {
    return notes;
  }

  public boolean hasNotes() {
    return notes != null;
  }
}
