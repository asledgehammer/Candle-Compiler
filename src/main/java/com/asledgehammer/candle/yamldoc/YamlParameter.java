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

    readString("name", true);
    readString("type", true);
    readString("notes");
  }
}
