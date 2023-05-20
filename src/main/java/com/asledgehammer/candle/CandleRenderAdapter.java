package com.asledgehammer.candle;

public interface CandleRenderAdapter {
    CandleRenderer<CandleClass> getClassRenderer();
    CandleRenderer<CandleAlias> getAliasRenderer();
    CandleRenderer<CandleExecutableCluster<CandleMethod>> getMethodRenderer();
}
