package com.asledgehammer.candle;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public abstract class CandleExecutable<E extends Executable, C extends CandleExecutable<E, C>>
    extends CandleElement<C> {

  private final List<CandleParameter> parameters = new ArrayList<>();
  private final boolean bPublic;
  private final boolean bStatic;
  E executable;

  public CandleExecutable(@NotNull E executable) {
    super(executable.getName());

    this.executable = executable;

    int modifiers = executable.getModifiers();
    this.bPublic = Modifier.isPublic(modifiers);
    this.bStatic = Modifier.isStatic(modifiers);
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    Parameter[] jParameters = executable.getParameters();

    if (jParameters.length != 0) {
      for (Parameter jParameter : jParameters) {
        parameters.add(new CandleParameter(jParameter));
      }
      for (CandleParameter candleParameter : parameters) {
        candleParameter.walk(graph);
      }
    }
  }

  public List<CandleParameter> getParameters() {
    return parameters;
  }

  public boolean hasParameters() {
    return !this.parameters.isEmpty();
  }

  public int size() {
    return parameters.size();
  }

  public boolean isPublic() {
    return bPublic;
  }

  public boolean isStatic() {
    return bStatic;
  }

  public E getExecutable() {
    return executable;
  }
}
