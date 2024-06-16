package com.asledgehammer.candle;

import com.asledgehammer.rosetta.RosettaClass;
import com.asledgehammer.rosetta.RosettaConstructor;
import com.asledgehammer.rosetta.RosettaParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  @NotNull
  public RosettaConstructor getDocs() {
    if(this.docs == null) {
      this.docs = new RosettaConstructor(null, genDocs());
    }
    return this.docs;
  }

  public boolean hasDocs() {
    return this.docs != null;
  }

  public boolean isDocsValid() {
    if(this.docs == null) return false;

    // Parameters
    if(this.hasParameters()) {
      for(CandleParameter parameter : this.getParameters()) {
        if(!parameter.isDocsValid()) {
          return false;
        }
      }
    }

    return true;
  }

  public Map<String, Object> genDocs() {

    System.out.println("Constructor.genDocs(): " + getLuaName());

    Map<String, Object> mapConstructor = new HashMap<>();

    // MODIFIERS
    List<String> listModifiers = new ArrayList<>();
    if (this.isPublic()) listModifiers.add("public");
    else if (this.isProtected()) listModifiers.add("protected");
    else if (this.isPrivate()) listModifiers.add("private");
    if (this.isStatic()) listModifiers.add("static");
    if (this.isFinal()) listModifiers.add("final");
    mapConstructor.put("modifiers", listModifiers);

    // PARAMETERS
    if (this.hasParameters()) {
      List<Object> listParameters = new ArrayList<>();
      for (CandleParameter parameter : this.getParameters()) {
        listParameters.add(parameter.getDocs().toJSON());
      }
      mapConstructor.put("parameters", listParameters);
    }

    return mapConstructor;
  }
}
