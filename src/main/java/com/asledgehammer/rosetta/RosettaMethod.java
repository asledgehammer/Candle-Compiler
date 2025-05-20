package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unchecked")
public class RosettaMethod extends RosettaEntity {

    private final List<RosettaParameter> parameters = new ArrayList<>();
    private final RosettaReturn returns;
    private final String name;
    private final String notes;
    private final String[] modifiers;
    private final boolean deprecated;

    public RosettaMethod(@NotNull Map<String, Object> raw) {
        super(raw);

        /* PROPERTIES */
        this.name = RosettaUtils.formatName(readRequiredString("name"));
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

        /* RETURN */
        if (!raw.containsKey("return")) {
            throw new RuntimeException("Method does not have return definition: " + this.name);
        }
        this.returns = new RosettaReturn((Map<String, Object>) raw.get("return"));
    }

    @NotNull
    public String asJavaString(@NotNull String prefix) {
        StringBuilder stringBuilder = new StringBuilder(prefix);
        String[] modifiers = this.getModifiers();
        if (modifiers.length != 0) {
            for (String modifier : this.getModifiers()) {
                stringBuilder.append(modifier).append(' ');
            }
        }
        stringBuilder
                .append(this.getReturn().getType().getBasic())
                .append(' ')
                .append(this.getName())
                .append('(');
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

        // NAME
        mapMethod.put("name", this.getName());

        // MODIFIERS
        if(this.modifiers.length != 0) {
            List<String> listModifiers = new ArrayList<>();
            Collections.addAll(listModifiers, getModifiers());
            mapMethod.put("modifiers", listModifiers);
        }

        // PARAMETERS
        if (!this.parameters.isEmpty()) {
            List<Map<String, Object>> mapParameters = new ArrayList<>();
            for (RosettaParameter parameter : this.parameters) {
                mapParameters.add(parameter.toJSON());
            }
            mapMethod.put("parameters", mapParameters);
        }

        // RETURNS
        mapMethod.put("return", this.returns.toJSON());

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

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public RosettaReturn getReturn() {
        return this.returns;
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

    public String[] getModifiers() {
        return this.modifiers;
    }
}
