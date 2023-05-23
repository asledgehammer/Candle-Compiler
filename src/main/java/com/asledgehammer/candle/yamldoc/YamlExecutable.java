package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class YamlExecutable extends YamlEntity {

  @NotNull final YamlParameter[] parameters;
  @Nullable final String notes;

  YamlExecutable(Map<String, Object> raw) {
    super(raw);

    this.parameters = readParameters();
    this.notes = readString("notes");
  }

  @NotNull
  YamlParameter[] readParameters() {
    if (!raw.containsKey("parameters")) return new YamlParameter[0];

    List<Map<String, Object>> list = (List<Map<String, Object>>) raw.get("parameters");
    YamlParameter[] parameters = new YamlParameter[list.size()];
    for (int i = 0; i < list.size(); i++) {
      parameters[i] = new YamlParameter(list.get(i));
    }
    return parameters;
  }

  @NotNull
  public YamlParameter[] getParameters() {
    return parameters;
  }

  @Nullable
  public String getNotes() {
    return notes;
  }

  public boolean hasNotes() {
    return this.notes != null;
  }
}
