package com.asledgehammer.candle;

public interface CandleRenderAdapter {
  CandleRenderer<CandleClass> getClassRenderer();

  CandleRenderer<CandleAlias> getAliasRenderer();
}
