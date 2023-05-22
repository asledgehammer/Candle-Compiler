package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

public class YamlMethod extends YamlExecutable {

  final String name;
  final String[] modifiers;
  final String notes;
  final YamlReturn _return;

  YamlMethod(Map<String, Object> raw) {
    super(raw);

    this.name = readString("name", true);
    this.modifiers = readModifiers();
    this.notes = readString("notes");
    this._return = new YamlReturn((Map<String, Object>) raw.get("return"));
  }

  @Override
  public String toString() {
    return "YamlMethod{"
        + "name='"
        + name
        + '\''
        + ", modifiers="
        + Arrays.toString(modifiers)
        + ", notes='"
        + notes
        + '\''
        + ", _return="
        + _return
        + ", parameters="
        + Arrays.toString(parameters)
        + '}';
  }

  @NotNull
  public YamlReturn getReturn() {
    return _return;
  }
}
