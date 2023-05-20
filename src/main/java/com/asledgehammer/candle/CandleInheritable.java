package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

public abstract class CandleInheritable<E extends CandleEntity<E>> extends CandleEntity<E> {
  CandleInheritable<E> parent;

  CandleInheritable(@NotNull Class<?> clazz) {
    super(clazz);
  }

  public CandleInheritable<E> getParent() {
    return parent;
  }

  public void setParent(@NotNull CandleInheritable<E> parent) {
    this.parent = parent;
  }
}
