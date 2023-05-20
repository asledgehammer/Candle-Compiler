package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public abstract class YamlExecutable extends YamlEntity {

  @NotNull final YamlParameter[] parameters;

  YamlExecutable(Map<String, Object> raw) {
    super(raw);

    this.parameters = readParameters();
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
}
