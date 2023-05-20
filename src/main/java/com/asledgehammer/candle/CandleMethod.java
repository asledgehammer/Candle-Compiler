package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class CandleMethod extends CandleExecutable<Method, CandleMethod> {

  private final Method method;

  public CandleMethod(@NotNull Method method) {
    super(method);
    this.method = method;
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    super.onWalk(graph);

    // If not an exposed class, attempt to add as alias.
    graph.evaluate(getReturnType());
  }

  public Class<?> getReturnType() {
    return this.method.getReturnType();
  }
}
