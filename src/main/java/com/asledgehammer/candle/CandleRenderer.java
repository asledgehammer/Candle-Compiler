package com.asledgehammer.candle;

public interface CandleRenderer<E extends CandleElement<E>> {
  String onRender(E entity);
}
