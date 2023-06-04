package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Deprecated
public class YamlConstructor extends YamlExecutable {

    final String name;
    final String[] modifiers;
    final String returnType;

    YamlConstructor(@NotNull Map<String, Object> raw, String name) {
        super(raw);

        this.name = name;
        this.returnType = readString("returnType");
        this.modifiers = readModifiers();
    }
}
