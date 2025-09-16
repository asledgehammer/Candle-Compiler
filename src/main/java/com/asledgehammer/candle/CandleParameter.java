package com.asledgehammer.candle;

import com.asledgehammer.rosetta.RosettaParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class CandleParameter extends CandleEntity<CandleParameter> {

  private final Parameter parameter;
  RosettaParameter docs;

  CandleParameter(@NotNull Parameter parameter) {
    super(parameter.getType(), parameter.getName());
    this.parameter = parameter;
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {

    if (Candle.addParameterClasses) {
      for (Class<?> clz :
          CandleUtils.resolveList(CandleUtils.extractClasses(getFullType(), false))) {
        graph.evaluate(clz);
      }
    } else {
      // If not an exposed class, attempt to add as alias.
      graph.evaluate(parameter.getType());
    }
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

  public String getFullType() {
    return CandleUtils.getFullParameterType(this.parameter);
  }

  public String getBasicType() {
    return CandleUtils.asBasicType(this.getFullType());
  }

  public boolean isDocsValid() {
    if (this.docs == null) return false;
    if (this.docs.getType().hasFull()) {
      return this.docs.getType().matches(this.getFullType(), this.getBasicType());
    } else {
      return this.docs.getType().matches(this.getBasicType());
    }
  }

  @NotNull
  public RosettaParameter getDocs() {
    if (this.docs == null) {
      this.docs = new RosettaParameter(this.genDocs());
    }
    return docs;
  }

  public boolean hasDocs() {
    return this.docs != null;
  }

  public Map<String, Object> genDocs() {

    //        System.out.println("Parameter.genDocs(): " + getLuaName());

    Map<String, Object> mapParameter = new HashMap<>();

    // NAME
    mapParameter.put("name", this.getLuaName());

    // TYPE
    Map<String, Object> mapType = new HashMap<>();
    mapType.put("basic", this.getBasicType());
    mapType.put("full", this.getFullType());
    mapParameter.put("type", mapType);

    return mapParameter;
  }

  public boolean isNullable() {
    String type = getBasicType();
    return switch (type) {
      case "void", "boolean", "byte", "short", "int", "float", "double", "long" -> false;
      default -> true;
    };
  }
}
