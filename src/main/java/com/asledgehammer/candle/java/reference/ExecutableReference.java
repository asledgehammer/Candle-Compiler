package com.asledgehammer.candle.java.reference;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;

/**
 * @param <E> Either {@link Constructor Constructor} or {@link Method Method}.
 */
public abstract class ExecutableReference<E extends Executable> {

  protected final ParameterReference<?>[] parameterReferences;
  protected final E executable;
  private final TypeReference[] bounds;
  private final ClassReference classReference;

  ExecutableReference(@NotNull ClassReference classReference, @NotNull E executable) {
    this.classReference = classReference;
    this.executable = executable;

    // Compile generic variable(s).
    TypeVariable<?>[] vars = this.executable.getTypeParameters();
    this.bounds = new TypeReference[vars.length];
    for (int i = 0; i < vars.length; i++) {
      this.bounds[i] = TypeReference.wrap(vars[i]);
    }

    // Compile parameter(s).
    Parameter[] p = executable.getParameters();
    int length = p.length;
    parameterReferences = new ParameterReference[length];
    if (p.length != 0) {
      Type[] gp = executable.getGenericParameterTypes();
      for (int i = 0; i < length; i++) {
        Parameter parameter = p[i];
        Type genericType = gp[i];
        parameterReferences[i] = new ParameterReference<>(this, parameter, genericType);
      }
    }
  }

  @NotNull
  public ParameterReference<?>[] getParameterReferences() {
    return this.parameterReferences;
  }

  @NotNull
  public TypeReference[] getBounds() {
    return this.bounds;
  }

  @NotNull
  public E getExecutable() {
    return this.executable;
  }

  @NotNull
  public ClassReference getClassReference() {
    return this.classReference;
  }
}
