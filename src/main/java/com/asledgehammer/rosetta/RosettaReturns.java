package com.asledgehammer.rosetta;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public class RosettaReturns extends RosettaEntity {

  @NotNull RosettaType type;
  @Nullable String notes;

  RosettaReturns(Map<String, Object> raw) {
    super(raw);

    if (!raw.containsKey("type")) {
      throw new RuntimeException("The returns does not have a defined type.");
    }
    this.type = new RosettaType((Map<String, Object>) raw.get("type"));
    this.notes = readNotes();
  }

  @Override
  public String toString() {
    return "YamlReturn{" + "type='" + type + '\'' + ", notes='" + notes + '\'' + '}';
  }

  @NotNull
  public RosettaType getType() {
    return this.type;
  }

  @Nullable
  public String getNotes() {
    return notes;
  }

  public boolean hasNotes() {
    return notes != null;
  }

  public Map<String, Object> toJSON() {
    Map<String, Object> mapReturns = new HashMap<>();
    mapReturns.put("type", this.type.toJSON());
    mapReturns.put("notes", this.notes);
    return mapReturns;
  }
}
