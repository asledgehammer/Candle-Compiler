package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

public class CandleAlias extends CandleEntity<CandleAlias> {
  public CandleAlias(Class<?> clazz) {
    super(clazz);
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {}
}
