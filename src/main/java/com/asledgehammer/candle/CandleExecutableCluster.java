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
    if (!executable.getLuaName().equals(getLuaName())) {
      throw new IllegalArgumentException(
          "Executable '"
              + executable.getLuaName()
              + "' does not match the first method name: "
              + this.getLuaName());
    }
    executables.add(executable);
  }

//  @NotNull
//  public Map<Integer, List<C>> sort() {
//    Map<Integer, List<C>> groups = new HashMap<>();
//
//    // Place methods into their groups using parameter counts.
//    for (C method : getExecutables()) {
//      int count = method.getParameterCount();
//      List<C> group = groups.computeIfAbsent(count, k -> new ArrayList<>());
//      group.add(method);
//    }
//
//    // Sort executables in their groups.
//    for (List<C> group : groups.values()) {
//      group.sort(CandleExecutableComparator.INSTANCE);
//    }
//
//    return groups;
//  }

  @NotNull
  public List<C> getExecutables() {
    return executables;
  }

  public boolean hasOverloads() {
    return executables.size() > 1;
  }
}
