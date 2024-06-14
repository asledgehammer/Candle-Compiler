package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "unused"})
public class RosettaConstructor extends RosettaEntity {

  private final List<RosettaParameter> parameters = new ArrayList<>();
  private final String notes;
  private final RosettaClass clazz;
  private final boolean deprecated;
  private final String[] modifiers;

  RosettaConstructor(@NotNull RosettaClass clazz, @NotNull Map<String, Object> raw) {
    super(raw);

    this.clazz = clazz;

    /* PROPERTIES */
    this.notes = readNotes();
    this.deprecated = readBoolean("deprecated") != null;
    this.modifiers = readModifiers();

    /* PARAMETERS */
    if (raw.containsKey("parameters")) {
      List<Map<String, Object>> rawParameters = (List<Map<String, Object>>) raw.get("parameters");
      for (Map<String, Object> rawParameter : rawParameters) {
        RosettaParameter parameter = new RosettaParameter(rawParameter);
        parameters.add(parameter);
      }
    }
  }

  @Override
  public String toString() {
    return "RosettaConstructor{" + "parameters=" + parameters + ", notes='" + notes + '\'' + '}';
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
    stringBuilder.append(clazz.getName()).append('(');
    List<RosettaParameter> parameters = this.getParameters();
    if (!parameters.isEmpty()) {
      for (RosettaParameter parameter : parameters) {
        stringBuilder
            .append(parameter.getType().getBasic())
            .append(' ')
            .append(parameter.getName())
            .append(", ");
      }
      stringBuilder = new StringBuilder(stringBuilder.substring(0, stringBuilder.length() - 2));
    }
    stringBuilder.append(')');
    return stringBuilder.toString();
  }

  @NotNull
  public Map<String, Object> toJSON() {
    Map<String, Object> mapMethod = new HashMap<>();

    // PARAMETERS
    if (!this.parameters.isEmpty()) {
      List<Map<String, Object>> mapParameters = new ArrayList<>();
      for (RosettaParameter parameter : this.parameters) {
        mapParameters.add(parameter.toJSON());
      }
      mapMethod.put("parameters", mapParameters);
    }

    // NOTES
    mapMethod.put("notes", this.notes);

    return mapMethod;
  }

  @NotNull
  public List<RosettaParameter> getParameters() {
    return this.parameters;
  }

  public boolean hasParameters() {
    return !this.parameters.isEmpty();
  }

  @Nullable
  public String getNotes() {
    return this.notes;
  }

  public boolean hasNotes() {
    return this.notes != null;
  }

  @NotNull
  public RosettaClass getClazz() {
    return this.clazz;
  }

  public boolean isDeprecated() {
    return this.deprecated;
  }

  public String[] getModifiers() {
    return this.modifiers;
  }
}
