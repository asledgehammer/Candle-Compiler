package com.asledgehammer.rosetta;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public class RosettaParameter extends RosettaEntity {

    @NotNull String name;
    @NotNull RosettaType type;
    @Nullable String notes;

    RosettaParameter(Map<String, Object> raw) {
        super(raw);

        this.name = RosettaUtils.formatName(readRequiredString("name"));
        if (!raw.containsKey("type")) {
            throw new RuntimeException("The returns does not have a defined type.");
        }
        this.type = new RosettaType((Map<String, Object>) raw.get("type"));
        this.notes = readNotes();
    }

    @Override
    public String toString() {
        return "YamlParameter{"
                + "name='"
                + name
                + '\''
                + ", type='"
                + type
                + '\''
                + ", notes='"
                + notes
                + '\''
                + '}';
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public RosettaType getType() {
        return this.type;
    }

    @Nullable
    public String getNotes() {
        return this.notes;
    }

    public boolean hasNotes() {
        return this.notes != null;
    }

    public Map<String, Object> toJSON() {
        Map<String, Object> mapParameter = new HashMap<>();
        mapParameter.put("name", this.name);
        mapParameter.put("type", this.type.toJSON());
        mapParameter.put("notes", this.notes);
        return mapParameter;
    }
}
