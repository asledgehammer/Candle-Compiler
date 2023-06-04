package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings("unchecked")
public class RosettaField extends RosettaEntity {

  private final String name;
  private final String[] modifiers;
  private final RosettaReturns returns;
  private final String notes;

  public RosettaField(@NotNull String name, @NotNull Map<String, Object> raw) {
    super(raw);

    this.name = name;
    this.modifiers = this.readModifiers();
    this.notes = readString("notes");

    /* RETURNS */
    if (!raw.containsKey("returns")) {
      throw new RuntimeException("Method does not have returns definition: " + this.name);
    }
    this.returns = new RosettaReturns((Map<String, Object>) raw.get("returns"));
  }

  @NotNull
  public RosettaReturns getReturns() {
    return returns;
  }

  @NotNull
  public String[] getModifiers() {
    return modifiers;
  }

  @NotNull
  public String getName() {
    return this.name;
  }

  @Nullable
  public String getNotes() {
    return this.notes;
  }

  public boolean hasNotes() {
    return this.notes != null;
  }
}
