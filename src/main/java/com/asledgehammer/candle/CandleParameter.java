package com.asledgehammer.candle;

import com.asledgehammer.candle.yamldoc.YamlParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;

public class CandleParameter extends CandleEntity<CandleParameter> {

  private final Parameter parameter;
  YamlParameter yaml;

    CandleParameter(@NotNull Parameter parameter) {
    super(parameter.getType(), parameter.getName());
    this.parameter = parameter;
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    // If not an exposed class, attempt to add as alias.
    graph.evaluate(parameter.getType());
  }

  @Nullable
  public YamlParameter getYaml() {
    return yaml;
  }

  @Override
  public String getLuaName() {
      if(yaml == null) return super.getLuaName();
    return yaml.getName();
  }

  public Parameter getJavaParameter() {
    return this.parameter;
  }

  public boolean isVarArgs() {
    return parameter.isVarArgs();
  }
}
