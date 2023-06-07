package com.asledgehammer.candle;

import com.asledgehammer.rosetta.RosettaClass;
import com.asledgehammer.rosetta.RosettaConstructor;
import com.asledgehammer.rosetta.RosettaParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.List;

public class CandleConstructor extends CandleExecutable<Constructor<?>, CandleConstructor> {

  private final CandleClass candleClass;
  @Nullable RosettaConstructor docs;

  public CandleConstructor(@NotNull CandleClass candleClass, @NotNull Constructor<?> executable) {
    super(executable);

    this.candleClass = candleClass;
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    super.onWalk(graph);

    RosettaClass yamlFile = candleClass.getDocs();

    if (yamlFile != null) {
      Class<?>[] cParams;
      if (hasParameters()) {
        List<CandleParameter> parameters = getParameters();
        cParams = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
          cParams[i] = parameters.get(i).getJavaParameter().getType();
        }
      } else {
        cParams = new Class[0];
      }

      docs = yamlFile.getConstructor(cParams);
      if (docs != null && hasParameters()) {
        List<RosettaParameter> yamlParameters = docs.getParameters();
        List<CandleParameter> parameters = getParameters();
        for (int i = 0; i < parameters.size(); i++) {
          parameters.get(i).docs = yamlParameters.get(i);
        }
      }
    }
  }

  @Override
  public String getLuaName() {
    return "new";
  }

  @Nullable
  public RosettaConstructor getDocs() {
    return this.docs;
  }

  public boolean hasDocs() {
    return this.docs != null;
  }
}
