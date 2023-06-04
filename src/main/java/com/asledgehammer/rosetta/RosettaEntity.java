package com.asledgehammer.rosetta;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public class RosettaEntity {

  @NotNull private final Map<String, Object> raw;

  RosettaEntity(@NotNull Map<String, Object> raw) {
    this.raw = raw;
  }

  @NotNull
  String[] readModifiers() {
    if (!raw.containsKey("modifiers")) return new String[0];

    List<Object> list = (List<Object>) raw.get("modifiers");
    String[] ss = new String[list.size()];
    for (int index = 0; index < list.size(); index++) {
      ss[index] = list.get(index).toString();
    }

    return ss;
  }

  @Nullable
  String readString(@NotNull String id) {
    if (!raw.containsKey(id)) return null;
    return raw.get(id).toString();
  }

  @NotNull
  String readRequiredString(@NotNull String id) {
    if (!raw.containsKey(id)) throw new RuntimeException("The string '" + id + "' is not defined.");
    return raw.get(id).toString();
  }

  @Nullable
  Boolean readBoolean(@NotNull String id) {
    if (!raw.containsKey(id)) return null;
    return (boolean) raw.get(id);
  }

  protected boolean readRequiredBoolean(@NotNull String id) {
    if (!raw.containsKey(id))
      throw new RuntimeException("The boolean '" + id + "' is not defined.");
    return (boolean) raw.get(id);
  }
}
