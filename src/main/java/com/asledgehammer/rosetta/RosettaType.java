package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class RosettaType extends RosettaEntity {

    private final String rawBasic;
    private final String basic;
    private final String full;
    private final boolean nullable;

    public RosettaType(@NotNull Map<String, Object> raw) {
        super(raw);
        String basic = readRequiredString("basic");
        this.rawBasic = basic;

        if (basic.contains(".")) {
            String[] split = basic.split("\\.");
            this.basic = split[split.length - 1];
        } else {
            this.basic = basic;
        }

        Boolean nullable = readBoolean("nullable");
        if (nullable != null) {
            this.nullable = nullable;
        } else {
            this.nullable = isNullPossible(this.basic);
        }
        this.full = readString("full");
    }

    @Override
    public String toString() {
        return "RosettaType{" + "basic='" + basic + '\'' + ", full='" + full + '\'' + '}';
    }

    @NotNull
    public String getBasic() {
        return this.basic;
    }

    @Nullable
    public String getFull() {
        return this.full;
    }

    @NotNull
    public String getRawBasic() {
        return this.rawBasic;
    }

    public boolean hasFull() {
        return this.full != null;
    }

    public Map<String, Object> toJSON() {
        Map<String, Object> mapType = new HashMap<>();
        mapType.put("basic", this.basic);
        mapType.put("full", this.full);
        mapType.put("nullable", this.nullable);
        return mapType;
    }

    public static boolean isNullPossible(String basicType) {
        return switch (basicType) {
            case "boolean", "byte", "short", "int", "float", "double", "long", "char", "null", "void" -> false;
            default -> true;
        };
    }
}
