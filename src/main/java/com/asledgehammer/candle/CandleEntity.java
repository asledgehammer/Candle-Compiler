package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

abstract class CandleEntity<E extends CandleEntity<E>> extends CandleElement<E> {

  private final Class<?> clazz;

  CandleEntity(@NotNull Class<?> clazz) {
    super(clazz.getSimpleName());
    this.clazz = clazz;
  }

  CandleEntity(@NotNull Class<?> clazz, String luaName) {
    super(luaName);
    this.clazz = clazz;
  }

  public Class<?> getClazz() {
    return clazz;
  }
}
