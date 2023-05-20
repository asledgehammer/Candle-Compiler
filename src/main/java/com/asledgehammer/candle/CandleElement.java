package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

public abstract class CandleElement<E extends CandleElement<E>> {

  private final String luaName;
  private boolean walked = false;
  private boolean rendered = false;
  private String renderedCode = null;

  public CandleElement(@NotNull String luaName) {
    this.luaName = luaName;
  }

  public void walk(@NotNull CandleGraph graph) {
    if (this.walked) return;
    this.onWalk(graph);
    this.walked = true;
  }

  @SuppressWarnings("unchecked")
  public void render(@NotNull CandleRenderer<E> renderer) {
    if (this.rendered) return;
    this.renderedCode = renderer.onRender((E) this);
    this.rendered = true;
  }

  public String getRenderedCode() {
    return renderedCode;
  }

  public void setRenderedCode(String code) {
    this.renderedCode = code;
  }

  public boolean isWalked() {
    return this.walked;
  }

  public boolean isRendered() {
    return this.rendered;
  }

  public String getLuaName() {
    return luaName;
  }

  abstract void onWalk(@NotNull CandleGraph graph);

}
