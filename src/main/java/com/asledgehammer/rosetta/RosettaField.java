package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

@SuppressWarnings("unchecked")
public class RosettaField extends RosettaEntity {

  private final String name;
  private final String[] modifiers;
  private final RosettaType type;
  private final String notes;
  private final boolean deprecated;

  public RosettaField(@NotNull String name, @NotNull Map<String, Object> raw) {
    super(raw);

    this.name = name;
    this.modifiers = this.readModifiers();
    this.notes = readNotes();
    this.deprecated = readBoolean("deprecated") != null;

    /* RETURNS */
    if (!raw.containsKey("type")) {
      throw new RuntimeException("Field does not have type definition: " + this.name);
    }
    this.type = new RosettaType((Map<String, Object>) raw.get("type"));
  }

  @NotNull
  public String asJavaString(String prefix) {
    StringBuilder stringBuilder = new StringBuilder(prefix);
    String[] modifiers = this.getModifiers();
    if (modifiers.length != 0) {
      for (String modifier : this.getModifiers()) {
        stringBuilder.append(modifier).append(' ');
      }
    }
    stringBuilder.append(this.getType().getBasic()).append(' ').append(getName());
    return stringBuilder.toString();
  }

  @NotNull
  public RosettaType getType() {
    return this.type;
  }

  @NotNull
  public String[] getModifiers() {
    return this.modifiers;
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

  public boolean isDeprecated() {
    return this.deprecated;
  }
}
