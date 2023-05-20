package com.asledgehammer.candle.yamldoc;

import java.util.Arrays;
import java.util.Map;

public class YamlMethod extends YamlExecutable {

  final String name;
  final String[] modifiers;
  final String returnType;
  final String returnNotes;
  final String notes;

  YamlMethod(Map<String, Object> raw) {
    super(raw);

    this.name = readString("name", true);
    this.returnType = readString("returnType");
    this.modifiers = readModifiers();
    this.notes = readString("notes");
    this.returnNotes = readString("returnNotes");
  }

  @Override
  public String toString() {
    return "YamlMethod{" +
            "name='" + name + '\'' +
            ", modifiers=" + Arrays.toString(modifiers) +
            ", parameters=" + Arrays.toString(parameters) +
            ", returnType='" + returnType + '\'' +
            ", returnNotes='" + returnNotes + '\'' +
            ", notes='" + notes + '\'' +
            ", deprecated=" + deprecated +
            '}';
  }
}
