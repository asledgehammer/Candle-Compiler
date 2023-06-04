package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Deprecated
public class YamlEntity {

  @NotNull final Map<String, Object> raw;
  final boolean deprecated;

  YamlEntity(@NotNull Map<String, Object> raw) {
    this.raw = raw;
    Boolean deprecated = readBoolean("deprecated");
    if (deprecated != null && deprecated) this.deprecated = true;
    else this.deprecated = false;
  }

  @NotNull
  String[] readModifiers() {
    // MODIFIERS
    if (!raw.containsKey("modifiers")) return new String[0];

    List<Object> list = (List<Object>) raw.get("modifiers");
    String[] ss = new String[list.size()];
    for (int index = 0; index < list.size(); index++) {
      ss[index] = list.get(index).toString();
    }

    return ss;
  }

  @Nullable
  String readString(String id) {
    return readString(id, false);
  }

  @Nullable
  String readString(String id, boolean required) {
    if (!raw.containsKey(id)) {
      if (required) throw new RuntimeException("The string '" + id + "' is not defined.");
      return null;
    }
    return Jsoup.parse(raw.get(id).toString()).text();
  }

  @Nullable
  Boolean readBoolean(String id) {
    return readBoolean(id, false);
  }

  @Nullable
  Boolean readBoolean(String id, boolean required) {
    if (!raw.containsKey(id)) {
      if (required) throw new RuntimeException("The boolean '" + id + "' is not defined.");
      return null;
    }
    return (boolean) raw.get(id);
  }

  @NotNull
  static Map<String, Object> getMapFromFile(File file) {
    Map<String, Object> raw = null;
    FileReader reader = null;
    try {
      reader = new FileReader(file);
      raw = YamlDocs.yaml.load(reader);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (reader != null) {
      try {
        reader.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    assert raw != null;
    return raw;
  }
}
