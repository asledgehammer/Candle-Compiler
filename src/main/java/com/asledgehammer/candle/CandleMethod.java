package com.asledgehammer.candle;

import com.asledgehammer.candle.yamldoc.YamlFile;
import com.asledgehammer.candle.yamldoc.YamlMethod;
import com.asledgehammer.candle.yamldoc.YamlParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import se.krka.kahlua.integration.annotations.LuaMethod;

import java.lang.reflect.Method;
import java.util.List;

public class CandleMethod extends CandleExecutable<Method, CandleMethod> {

  private final Method method;
  private YamlMethod yaml;
  private String luaName;

  public CandleMethod(@NotNull Method method) {
    super(method);
    this.method = method;

    LuaMethod annotation = method.getAnnotation(LuaMethod.class);
    if(annotation != null) {
      this.luaName = annotation.name();
    } else {
      this.luaName = method.getName();
    }
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    super.onWalk(graph);

    String path = method.getDeclaringClass().getName();
    YamlFile yamlFile = graph.getDocs().getFile(path);
    if (yamlFile != null) {
      yaml = yamlFile.getMethod(method);
      if (yaml != null && hasParameters()) {
        YamlParameter[] yamlParameters = yaml.getParameters();
        List<CandleParameter> parameters = getParameters();
        for (int i = 0; i < parameters.size(); i++) {
          parameters.get(i).yaml = yamlParameters[i];
        }
      }
    }

    // If not an exposed class, attempt to add as alias.
    graph.evaluate(getReturnType());
  }

  @Nullable
  public YamlMethod getYaml() {
    return this.yaml;
  }

  @NotNull
  @Override
  public String getLuaName() {
    return this.luaName;
  }

  public Class<?> getReturnType() {
    return this.method.getReturnType();
  }
}
