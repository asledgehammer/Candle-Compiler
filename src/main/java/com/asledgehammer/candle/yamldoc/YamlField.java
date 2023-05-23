package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class YamlField extends YamlEntity {

  @NotNull final String name;
  @Nullable final String notes;
  @NotNull final YamlReturn _return;

  YamlField(@NotNull Map<String, Object> raw) {
    super(raw);

    this.name = readString("name", true);
    this.notes = readString("notes");
    this._return = new YamlReturn((Map<String, Object>) raw.get("return"));
  }

  @NotNull
  public YamlReturn getReturn() {
    return _return;
  }

  @Nullable
  public String getNotes() {
    return this.notes;
  }

  public boolean hasNotes() {
    return this.notes != null;
  }
}
