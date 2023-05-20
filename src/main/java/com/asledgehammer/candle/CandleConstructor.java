package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

public class CandleConstructor extends CandleExecutable<Constructor, CandleConstructor> {

  public CandleConstructor(@NotNull Constructor executable) {
    super(executable);
  }

  @Override
  public String getLuaName() {
    return "new";
  }
}
