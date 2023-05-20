package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class YamlField extends YamlEntity {

    @NotNull final String name;
    @Nullable final String notes;
    @NotNull final String returnType;

    YamlField(@NotNull Map<String, Object> raw) {
        super(raw);

        this.name = readString("name", true);
        this.notes = readString("notes");
        this.returnType = readString("returnType");
    }
}
