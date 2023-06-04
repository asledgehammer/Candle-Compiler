package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class RosettaMethod extends RosettaEntity {

  private final List<RosettaParameter> parameters = new ArrayList<>();
  private final RosettaReturns returns;
  private final String name;
  private final String notes;

  RosettaMethod(@NotNull Map<String, Object> raw) {
    super(raw);

    /* PROPERTIES */
    this.name = readRequiredString("name");
    this.notes = readString("notes");

    /* PARAMETERS */
    if (raw.containsKey("parameters")) {
      List<Map<String, Object>> rawParameters = (List<Map<String, Object>>) raw.get("parameters");
      for (Map<String, Object> rawParameter : rawParameters) {
        RosettaParameter parameter = new RosettaParameter(rawParameter);
        parameters.add(parameter);
      }
    }

    /* RETURNS */
    if (!raw.containsKey("returns")) {
      throw new RuntimeException("Method does not have returns definition: " + this.name);
    }
    this.returns = new RosettaReturns((Map<String, Object>) raw.get("returns"));
  }

  @NotNull
  public List<RosettaParameter> getParameters() {
    return this.parameters;
  }

  public boolean hasParameters() {
    return !this.parameters.isEmpty();
  }

  @NotNull
  public String getName() {
    return this.name;
  }

  @NotNull
  public RosettaReturns getReturns() {
    return this.returns;
  }

  @Nullable
  public String getNotes() {
    return this.notes;
  }

  public boolean hasNotes() {
    return this.notes != null;
  }
}
