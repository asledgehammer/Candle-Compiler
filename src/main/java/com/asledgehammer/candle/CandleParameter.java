package com.asledgehammer.candle;

import com.asledgehammer.rosetta.RosettaParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;

public class CandleParameter extends CandleEntity<CandleParameter> {

  private final Parameter parameter;
  RosettaParameter docs;

  CandleParameter(@NotNull Parameter parameter) {
    super(parameter.getType(), parameter.getName());
    this.parameter = parameter;
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    // If not an exposed class, attempt to add as alias.
    graph.evaluate(parameter.getType());
  }

  @Nullable
  public RosettaParameter getDocs() {
    return docs;
  }

  public boolean hasDocs() {
    return this.docs != null;
  }

  @Override
  public String getLuaName() {
    if (docs == null) return super.getLuaName();
    return docs.getName();
  }

  public Parameter getJavaParameter() {
    return this.parameter;
  }

  public boolean isVarArgs() {
    return parameter.isVarArgs();
  }

  public boolean hasNotes() {
    return docs != null && docs.hasNotes();
  }
}
