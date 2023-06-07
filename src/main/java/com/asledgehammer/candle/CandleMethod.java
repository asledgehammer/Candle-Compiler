package com.asledgehammer.candle;

import com.asledgehammer.rosetta.RosettaClass;
import com.asledgehammer.rosetta.RosettaMethod;
import com.asledgehammer.rosetta.RosettaParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import se.krka.kahlua.integration.annotations.LuaMethod;

import java.lang.reflect.Method;
import java.util.List;

public class CandleMethod extends CandleExecutable<Method, CandleMethod> {

  private final Method method;
  private final CandleClass candleClass;
  private RosettaMethod docs;
  private final String luaName;
  private final boolean exposed;

  public CandleMethod(@NotNull CandleClass candleClass, @NotNull Method method) {
    super(method);

    this.candleClass = candleClass;
    this.method = method;

    LuaMethod annotation = method.getAnnotation(LuaMethod.class);
    this.exposed = annotation != null;
    if (exposed) {
      this.luaName = annotation.name();
    } else {
      this.luaName = method.getName();
    }
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    super.onWalk(graph);

    RosettaClass yamlFile = candleClass.getDocs();
    if (yamlFile != null) {
      docs = yamlFile.getMethod(method);
      if (docs != null && hasParameters()) {
        List<RosettaParameter> yamlParameters = docs.getParameters();
        List<CandleParameter> parameters = getParameters();
        for (int i = 0; i < parameters.size(); i++) {
          parameters.get(i).docs = yamlParameters.get(i);
        }
      }
    }

    // If not an exposed class, attempt to add as alias.
    graph.evaluate(getReturnType());
  }

  @Nullable
  public RosettaMethod getDocs() {
    return this.docs;
  }

  public boolean hasDocs() {
    return this.docs != null;
  }

  @NotNull
  @Override
  public String getLuaName() {
    return this.luaName;
  }

  public Class<?> getReturnType() {
    return this.method.getReturnType();
  }

  public boolean isExposed() {
    return this.exposed;
  }
}
