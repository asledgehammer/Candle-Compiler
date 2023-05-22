package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class YamlMethodCluster {

  final List<YamlMethod> methods = new ArrayList<>();
  final String name;

  YamlMethodCluster(String name) {
    this.name = name;
  }

  public void add(YamlMethod method) {
    if (methods.contains(method)) return;
    methods.add(method);
  }

  @Nullable
  public YamlMethod getWithParameters(Class<?>... clazzes) {
    for (YamlMethod method : methods) {
      YamlParameter[] parameters = method.parameters;
      if (clazzes.length == parameters.length) {
        if (clazzes.length == 0) return method;

        boolean invalid = false;
        for (int i = 0; i < parameters.length; i++) {
          YamlParameter parameter = parameters[i];
          if (!parameter.type.equals(clazzes[i].getSimpleName())) {
            invalid = true;
            break;
          }
        }
        if (invalid) {
          continue;
        }
        return method;
      }
    }

    return null;
  }

  @Override
  public String toString() {
    return "YamlMethodCluster{" + "methods=" + methods + ", name='" + name + '\'' + '}';
  }
}
