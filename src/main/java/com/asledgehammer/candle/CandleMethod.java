package com.asledgehammer.candle;

import com.asledgehammer.candle.yamldoc.YamlFile;
import com.asledgehammer.candle.yamldoc.YamlMethod;
import com.asledgehammer.candle.yamldoc.YamlParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

public class CandleMethod extends CandleExecutable<Method, CandleMethod> {

  private final Method method;
  private YamlMethod yaml;

  public CandleMethod(@NotNull Method method) {
    super(method);
    this.method = method;
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

  public Class<?> getReturnType() {
    return this.method.getReturnType();
  }
}
