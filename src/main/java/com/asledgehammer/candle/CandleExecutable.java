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
  private final boolean bProtected;
  private final boolean bPrivate;
  private final boolean bStatic;
  private final boolean bFinal;
  E executable;

  public CandleExecutable(@NotNull E executable) {
    super(executable.getName());

    this.executable = executable;

    int modifiers = executable.getModifiers();
    this.bPublic = Modifier.isPublic(modifiers);
    this.bProtected = Modifier.isProtected(modifiers);
    this.bPrivate = Modifier.isPrivate(modifiers);
    this.bFinal = Modifier.isFinal(modifiers);
    this.bStatic = Modifier.isStatic(modifiers);
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder(this.getLuaName() + "(");
    if (hasParameters()) {
      for (CandleParameter parameter : parameters) {
        s.append(parameter.getJavaParameter().getType().getSimpleName()).append(", ");
      }
      s = new StringBuilder(s.substring(0, s.length() - 2));
    }
    return s + ")";
  }

  boolean walkedParameters = false;

  @Override
  void onWalk(@NotNull CandleGraph graph) {

    if (!walkedParameters) {
      Parameter[] jParameters = executable.getParameters();
      for (Parameter jParameter : jParameters) {
        parameters.add(new CandleParameter(jParameter));
        if (Candle.addParameterClasses) {
          graph.addClass(jParameter.getType());
        }
      }
      walkedParameters = true;
    }

    if (hasParameters()) {
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

  public boolean isPublic() {
    return this.bPublic;
  }

  public boolean isProtected() {
    return this.bProtected;
  }

  public boolean isPrivate() {
    return this.bPrivate;
  }

  public boolean isStatic() {
    return bStatic;
  }

  public boolean isFinal() {
    return this.bFinal;
  }

  public E getExecutable() {
    return executable;
  }

  public int getParameterCount() {
    return parameters.size();
  }
}
