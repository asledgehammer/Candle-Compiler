package com.asledgehammer.candle;

import com.asledgehammer.rosetta.*;
import org.jetbrains.annotations.NotNull;
import se.krka.kahlua.integration.annotations.LuaMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        if (yamlParameters.size() != parameters.size()) {
          //                    System.out.println("yaml-size: " + yamlParameters.size() + ",
          // param-size: " + parameters.size());
          this.docs = null;
          RosettaMethod mDocs = getDocs();
          yamlParameters = mDocs.getParameters();
        }

        for (int i = 0; i < parameters.size(); i++) {
          parameters.get(i).docs = yamlParameters.get(i);
        }
      }
    }

    if (Candle.addReturnClasses) {
      graph.addClass(getReturnType());
    }

    // If not an exposed class, attempt to add as alias.
    graph.evaluate(getReturnType());
  }

  @NotNull
  @Override
  public String getLuaName() {
    return this.luaName;
  }

  public Class<?> getReturnType() {
    return this.method.getReturnType();
  }

  public String getFullReturnType() {
    return CandleUtils.getFullReturnType(this.method);
  }

  public String getBasicReturnType() {
    return CandleUtils.asBasicType(this.getFullReturnType());
  }

  public boolean isExposed() {
    return this.exposed;
  }

  public boolean isDocsValid() {
    if (this.docs == null) return false;

    // PARAMETERS
    if (this.hasParameters()) {
      for (CandleParameter parameter : this.getParameters()) {
        if (!parameter.isDocsValid()) {
          return false;
        }
      }
    }

    // RETURNS
    RosettaReturns returns = this.docs.getReturns();
    RosettaType rType = returns.getType();
    if (rType.hasFull()) {
      return rType.matches(this.getFullReturnType(), this.getBasicReturnType());
    } else {
      return rType.matches(this.getBasicReturnType());
    }
  }

  @NotNull
  public RosettaMethod getDocs() {
    if (this.docs == null) {
      this.docs = new RosettaMethod(genDocs());
    }

    return this.docs;
  }

  public boolean hasDocs() {
    return this.docs != null;
  }

  public Map<String, Object> genDocs() {

    //        System.out.println("Method.genDocs(): " + getLuaName());

    Map<String, Object> mapMethod = new HashMap<>();

    // NAME
    mapMethod.put("name", this.getLuaName());

    // MODIFIERS
    List<String> listModifiers = new ArrayList<>();
    if (this.isPublic()) listModifiers.add("public");
    else if (this.isProtected()) listModifiers.add("protected");
    else if (this.isPrivate()) listModifiers.add("private");
    if (this.isStatic()) listModifiers.add("static");
    if (this.isFinal()) listModifiers.add("final");
    mapMethod.put("modifiers", listModifiers);

    // PARAMETERS
    if (this.hasParameters()) {
      List<Object> listParameters = new ArrayList<>();
      for (CandleParameter parameter : this.getParameters()) {
        listParameters.add(parameter.getDocs().toJSON());
      }
      mapMethod.put("parameters", listParameters);
    }

    // RETURNS
    Map<String, Object> mapReturns = new HashMap<>();

    // RETURNS TYPE
    Map<String, Object> mapReturnsType = new HashMap<>();
    String fullType = this.getFullReturnType();
    String basicType = CandleUtils.asBasicType(fullType);
    mapReturnsType.put("full", fullType);
    mapReturnsType.put("basic", basicType);
    mapReturns.put("type", mapReturnsType);

    mapMethod.put("returns", mapReturns);

    return mapMethod;
  }
}
