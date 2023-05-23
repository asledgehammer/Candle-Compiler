package com.asledgehammer.candle;

import com.asledgehammer.candle.yamldoc.YamlConstructor;
import com.asledgehammer.candle.yamldoc.YamlFile;
import com.asledgehammer.candle.yamldoc.YamlParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.List;

public class CandleConstructor extends CandleExecutable<Constructor<?>, CandleConstructor> {

  @Nullable YamlConstructor yaml;

  public CandleConstructor(@NotNull Constructor<?> executable) {
    super(executable);
  }

  @Override
  void onWalk(@NotNull CandleGraph graph) {
    super.onWalk(graph);

    String path = executable.getDeclaringClass().getName();
    YamlFile yamlFile = graph.getDocs().getFile(path);

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

      yaml = yamlFile.getConstructor(cParams);
      if (yaml != null && hasParameters()) {
        YamlParameter[] yamlParameters = yaml.getParameters();
        List<CandleParameter> parameters = getParameters();
        for (int i = 0; i < parameters.size(); i++) {
          parameters.get(i).yaml = yamlParameters[i];
        }
      }
    }
  }

  @Override
  public String getLuaName() {
    return "new";
  }

  public YamlConstructor getYaml() {
    return this.yaml;
  }

  public boolean hasYaml() {
    return this.yaml != null;
  }
}
