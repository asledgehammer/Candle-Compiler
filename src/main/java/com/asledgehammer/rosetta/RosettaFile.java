package com.asledgehammer.rosetta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class RosettaFile extends RosettaEntity {

  private final Map<String, RosettaNamespace> namespaces = new HashMap<>();
  private final Rosetta rosetta;

  RosettaFile(@NotNull Rosetta rosetta, @NotNull Map<String, Object> raw) {
    super(raw);

    this.rosetta = rosetta;

    /* NAMESPACES */
    if (raw.containsKey("namespaces")) {
      Map<String, Object> rawNamespaces = (Map<String, Object>) raw.get("namespaces");
      for (String name : rawNamespaces.keySet()) {
        Map<String, Object> rawNamespace = (Map<String, Object>) rawNamespaces.get(name);
        RosettaNamespace namespace = rosetta.getNamespace(name);

        if (namespace == null) {
          namespace = new RosettaNamespace(name, rawNamespace);
          rosetta.addNamespace(namespace);
        } else {
          namespace.parse(rawNamespace);
        }

        this.namespaces.put(name, namespace);
      }
    }
  }

  @Override
  public String toString() {
    return "RosettaFile{" + "namespaces=" + namespaces + '}';
  }

  @NotNull
  public Map<String, RosettaNamespace> getNamespaces() {
    return this.namespaces;
  }

  @Nullable
  public RosettaNamespace getNamespace(@NotNull String id) {
    return this.namespaces.get(id);
  }
}
