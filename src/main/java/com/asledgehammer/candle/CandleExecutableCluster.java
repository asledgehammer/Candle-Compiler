package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CandleExecutableCluster<C extends CandleExecutable>
    extends CandleElement<CandleExecutableCluster<C>> {

  private final List<C> overloads = new ArrayList<>();
  private C first;

  public CandleExecutableCluster(@NotNull C first) {
    super(first.getLuaName());
    this.first = first;
  }

  @Override
  public void onWalk(@NotNull CandleGraph graph) {
    // We add the first to the overloads and then use it to sort based on param count. Pop from the
    // bottom of the stack To reassign the first executable. All others are sorted overloads.
    ArrayList<C> all = new ArrayList<>();
    all.add(first);
    all.addAll(overloads);
    for (C c : all) {
      c.walk(graph);
    }
    all.sort(Comparator.comparingInt(o -> o.getParameters().size()));
    first = all.get(0);
    if (all.size() > 1) {
      overloads.clear();
      for (int index = 1; index < all.size(); index++) {

        C next = all.get(index);

        if (executableEquals(next.getExecutable(), first.getExecutable())) continue;

        boolean skip = false;
        if (!overloads.isEmpty()) {
          for (C oNext : overloads) {
            if (executableEquals(next.getExecutable(), oNext.getExecutable())) {
              skip = true;
              break;
            }
          }
        }

        if (!skip) overloads.add(all.get(index));
      }
    }
  }

  public void add(C executable) {
    if (!executable.getLuaName().equals(this.first.getLuaName())) {
      throw new IllegalArgumentException(
          "Executable '"
              + executable.getLuaName()
              + "' does not match the first method name: "
              + this.getLuaName());
    }

    if (!overloads.contains(executable)) overloads.add(executable);
  }

  public C getFirst() {
    return first;
  }

  public int size() {
    return overloads.size() + 1;
  }

  public List<C> getOverloads() {
    return overloads;
  }

  public boolean hasOverloads() {
    return !overloads.isEmpty();
  }

  public static boolean executableEquals(Executable a, Executable b) {

    // Check names first.
    if (!a.getName().equals(b.getName())) {
      return false;
    }

    // Check parameter-count.
    if (a.getParameterCount() != b.getParameterCount()) {
      return false;
    }

    // Check parameter-types.
    Parameter[] aps = a.getParameters();
    Parameter[] bps = b.getParameters();
    for (int index = 0; index < aps.length; index++) {
      Parameter ap = aps[index];
      Parameter bp = bps[index];
      if (!ap.getClass().equals(bp.getClass())) {
        return false;
      }
    }

    return true;
  }
}
