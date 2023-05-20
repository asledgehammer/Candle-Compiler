package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Parameter;

public class CandleParameter extends CandleEntity<CandleParameter> {

  private final Parameter parameter;

  CandleParameter(@NotNull Parameter parameter) {
    super(parameter.getType(), parameter.getName());
    this.parameter = parameter;
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    // If not an exposed class, attempt to add as alias.
    graph.evaluate(parameter.getType());
  }

  public Parameter getJavaParameter() {
    return this.parameter;
  }

  public boolean isVarArgs() {
    return parameter.isVarArgs();
  }
}
