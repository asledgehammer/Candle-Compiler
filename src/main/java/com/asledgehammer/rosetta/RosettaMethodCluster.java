package com.asledgehammer.rosetta;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RosettaMethodCluster {

  private final List<RosettaMethod> methods = new ArrayList<>();
  private final String name;

  RosettaMethodCluster(@NotNull String name) {
    this.name = name;
  }

  public void add(@NotNull RosettaMethod method) {
    if (methods.contains(method)) return;
    methods.add(method);
  }

  @Nullable
  public RosettaMethod getWithParameters(Class<?>... clazzes) {
    for (RosettaMethod method : methods) {
      List<RosettaParameter> parameters = method.getParameters();
      if (clazzes.length == parameters.size()) {
        if (clazzes.length == 0) return method;

        boolean invalid = false;
        for (int i = 0; i < parameters.size(); i++) {
          RosettaParameter parameter = parameters.get(i);
          String basic = parameter.type.getBasic();
          Class<?> clazz = clazzes[i];
          if (!basic.equals(clazz.getSimpleName()) && !basic.equals(clazz.getName())) {
            invalid = true;
            break;
          }
        }
        if (invalid) continue;
        return method;
      }
    }

    return null;
  }

  @Override
  public String toString() {
    return "RosettaMethodCluster{" + "methods=" + methods + ", name='" + name + '\'' + '}';
  }

  public List<RosettaMethod> getMethods() {
    return this.methods;
  }

  public String getName() {
    return this.name;
  }
}
