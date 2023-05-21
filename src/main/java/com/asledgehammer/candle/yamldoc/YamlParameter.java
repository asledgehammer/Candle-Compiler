package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class YamlParameter extends YamlEntity {

  @NotNull String name;
  @NotNull String type;
  @Nullable String notes;

  YamlParameter(Map<String, Object> raw) {
    super(raw);

    this.name = readString("name", true);
    this.type = readString("type", true);
    this.notes = readString("notes");
  }

  @Override
  public String toString() {
    return "YamlParameter{" +
            "name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", notes='" + notes + '\'' +
            '}';
  }

  @NotNull
  public String getName() {
    return this.name;
  }

  @NotNull
  public String getType() {
    return type;
  }

  @Nullable
  public String getNotes() {
    return notes;
  }
}
