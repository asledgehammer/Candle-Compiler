package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CandleExecutableCluster<C extends CandleExecutable<?, ?>>
    extends CandleElement<CandleExecutableCluster<C>> {

  private final List<C> executables = new ArrayList<>();

  public CandleExecutableCluster(@NotNull String name) {
    super(name);
  }

  @Override
  public void onWalk(@NotNull CandleGraph graph) {
    for (C c : executables) c.walk(graph);
    executables.sort(CandleExecutableComparator.INSTANCE);
  }

  public void add(C executable) {
    executables.add(executable);
  }

  @NotNull
  public List<C> getExecutables() {
    return executables;
  }

  public boolean hasOverloads() {
    return executables.size() > 1;
  }
}
