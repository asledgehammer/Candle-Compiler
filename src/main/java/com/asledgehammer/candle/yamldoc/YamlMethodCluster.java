package com.asledgehammer.candle.yamldoc;

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

  @Override
  public String toString() {
    return "YamlMethodCluster{" +
            "methods=" + methods +
            ", name='" + name + '\'' +
            '}';
  }
}
