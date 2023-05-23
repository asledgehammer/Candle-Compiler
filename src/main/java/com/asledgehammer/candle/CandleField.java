package com.asledgehammer.candle;

import com.asledgehammer.candle.yamldoc.YamlField;
import com.asledgehammer.candle.yamldoc.YamlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class CandleField extends CandleEntity<CandleField> {

  private final boolean bPublic;
  private final boolean bStatic;

  @NotNull private final String name;

  @NotNull private final Field field;

  @Nullable private YamlField yaml;

  CandleField(@NotNull Field field) {
    super(field.getType(), field.getName());

    int modifiers = field.getModifiers();
    this.bStatic = Modifier.isStatic(modifiers);
    this.bPublic = Modifier.isPublic(modifiers);
    this.name = field.getName();
    this.field = field;
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    String path = field.getDeclaringClass().getName();
    YamlFile yamlFile = graph.getDocs().getFile(path);
    if (yamlFile != null) {
      yaml = yamlFile.getField(field.getName());
    }
  }

  public String getName() {
    return name;
  }

  public boolean isPublic() {
    return bPublic;
  }

  public boolean isStatic() {
    return bStatic;
  }

  public YamlField getYaml() {
    return this.yaml;
  }

  public boolean hasYaml() {
    return this.yaml != null;
  }
}
