package com.asledgehammer.candle.java.reference;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

import static com.asledgehammer.candle.java.reference.SimpleTypeReference.OBJECT_BOUNDS;

public class ClassReference {

  private static final Map<Class<?>, ClassReference> CACHE = new HashMap<>();

  private final Map<Class<?>, Map<String, TypeReference>> assignedSuperVariables = new HashMap<>();

  private final Map<String, Type> upperBounds = new HashMap<>();

  private final Class<?> clazz;
  private final ClassReference superClazzReference;
  private final ClassReference[] superInterfazeReferences;
  private final MethodReference[] methodReferences;
  private final Map<Method, MethodReference> methodReferenceMap = new HashMap<>();
  private final TypeReference[] genericTypes;
  private final Map<String, TypeReference> genericTypesMap = new HashMap<>();

  public MethodReference getMethodReference(Method method) {
    return methodReferenceMap.get(method);
  }

  private static boolean isClassPath(String path) {
    try {
      Class.forName(path);
      return true;
    } catch (ClassNotFoundException ignored) {
    }
    return false;
  }

  private ClassReference(Class<?> clazz) {
    this.clazz = clazz;

    TypeVariable<?>[] vars = clazz.getTypeParameters();
    this.genericTypes = new TypeReference[vars.length];
    for (int i = 0; i < vars.length; i++) {
      TypeVariable<?> var = vars[i];
      Type[] bounds = var.getBounds();
      TypeReference[] boundsRef = new TypeReference[bounds.length];
      for (int j = 0; j < bounds.length; j++) {
        boundsRef[j] = TypeReference.wrap(bounds[j]);
      }
      String typeName = var.getTypeName();
      this.genericTypes[i] = new UnionTypeReference(typeName, true, boundsRef);
      this.genericTypesMap.put(var.getTypeName(), this.genericTypes[i]);
    }

    Type superClazz = clazz.getGenericSuperclass();

    if (superClazz != null) {
      Class<?> superClazzClazz = clazz.getSuperclass();
      assignedSuperVariables.put(superClazzClazz, createTypeMap(superClazzClazz, superClazz));
      superClazzReference = wrap(superClazzClazz);
    } else {
      superClazzReference = null;
    }

    Class<?>[] interfazeClazzes = clazz.getInterfaces();
    superInterfazeReferences = new ClassReference[interfazeClazzes.length];
    if (interfazeClazzes.length != 0) {
      Type[] interfazes = clazz.getGenericInterfaces();
      for (int i = 0; i < interfazes.length; i++) {
        Class<?> interfazeClazz = interfazeClazzes[i];
        Type interfaze = interfazes[i];
        assignedSuperVariables.put(interfazeClazz, createTypeMap(interfazeClazz, interfaze));
        superInterfazeReferences[i] = wrap(interfazeClazz);
      }
    }

    Method[] methods = clazz.getMethods();
    int length = methods.length;
    this.methodReferences = new MethodReference[length];
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      MethodReference methodReference = new MethodReference(this, method);
      this.methodReferences[i] = methodReference;
      methodReferenceMap.put(method, methodReference);
    }
  }

  public Stack<ClassReference> resolveChain(Class<?> baseClazz) {
    return resolveChain(baseClazz, new Stack<>());
  }

  public Stack<ClassReference> resolveChain(Class<?> baseClazz, Stack<ClassReference> stack) {

    // Found it.
    if (this.clazz == baseClazz) {
      stack.push(this);
      return stack;
    }

    // Try super-class route first. (if exists)
    if (this.superClazzReference != null) {
      stack.push(this);
      this.superClazzReference.resolveChain(baseClazz, stack);
      if (stack.peek().clazz != baseClazz) {
        stack.pop();
      }
    }

    // Try interface(s) next. (if exists)
    for (ClassReference interfazeReference : this.superInterfazeReferences) {
      stack.push(this);
      interfazeReference.resolveChain(baseClazz, stack);
      if (stack.peek().clazz == baseClazz) {
        break;
      }
      stack.pop();
    }

    return stack;
  }

  public TypeReference resolveReturnType(Method method) {
    Class<?> methodClazz = method.getDeclaringClass();

    Type genericReturnType = method.getGenericReturnType();
    String fullReturnType = genericReturnType.getTypeName();

    // Here we resolve the hierarchy as a stack to traverse backwards.
    List<ClassReference> stackRef = new ArrayList<>(resolveChain(methodClazz));

    ClassReference sup = stackRef.remove(stackRef.size() - 1);
    TypeReference returnType = TypeReference.wrap(fullReturnType);
    //    System.out.println("return init: " + method.getReturnType());
    while (!stackRef.isEmpty()) {
      ClassReference refNext = stackRef.remove(stackRef.size() - 1);
      if (refNext.assignedSuperVariables.containsKey(sup.clazz)) {
        Map<String, TypeReference> vars = refNext.assignedSuperVariables.get(sup.clazz);
        if (vars.containsKey(returnType.getBase())) {
          // The class contains an entry of the var name as generic so transform it to the
          // applied type.
          returnType = vars.get(returnType.getBase());
        }
      }
      sup = refNext;
    }

    if (upperBounds.containsKey(returnType.getBase())) {
      returnType = TypeReference.wrap(upperBounds.get(returnType.getBase()).getTypeName());
    }
    // Not sure what to do here for default.

    return returnType;
  }

  public TypeReference resolveType(String type, Class<?> deCl) {
    return resolveType(TypeReference.wrap(type), deCl);
  }

  public TypeReference resolveType(Type type, Class<?> deCl) {
    return resolveType(TypeReference.wrap(type), deCl);
  }

  public TypeReference resolveType(TypeReference type, Class<?> deCl) {
    String rawType = type.getBase();
    TypeReference resolvedType = type;
    TypeReference[] bounds = resolvedType.getBounds();
    Stack<ClassReference> chain = resolveChain(deCl);

    // Here we resolve the hierarchy as a stack to traverse backwards.
    List<ClassReference> stackRef = new ArrayList<>(chain);

    ClassReference sup = stackRef.remove(stackRef.size() - 1);
    while (!stackRef.isEmpty()) {
      ClassReference refNext = stackRef.remove(stackRef.size() - 1);
      if (refNext.assignedSuperVariables.containsKey(sup.clazz)) {
        Map<String, TypeReference> vars = refNext.assignedSuperVariables.get(sup.clazz);
        if (vars.containsKey(rawType)) {
          // The class contains an entry of the var name as generic so transform it to the
          // applied type.
          resolvedType = vars.get(rawType);
          if (resolvedType.isGeneric()) {
            TypeReference o = refNext.genericTypesMap.get(resolvedType.getBase());
            bounds = o.getBounds();
          }
        }
      }
      sup = refNext;
    }

    System.out.println(
        "Result: "
            + resolvedType
            + ", generic = "
            + resolvedType.isGeneric()
            + ", wildcard = "
            + resolvedType.isWildcard());

    if (!resolvedType.isPrimitive() && resolvedType.isGeneric()) {
      return new UnionTypeReference(resolvedType.getBase(), true, bounds);
    }

    return resolvedType;
  }

  private TypeReference[] resolveBounds(TypeReference type, Class<?> deCl) {
    return resolveBounds(type, deCl, resolveChain(deCl));
  }

  private TypeReference[] resolveBounds(
      TypeReference type, Class<?> deCl, Stack<ClassReference> chain) {
    if (chain == null) {
      chain = resolveChain(deCl);
    }

    String rawType = type.getBase();

    TypeReference resolvedType = type;
    TypeReference[] bounds = OBJECT_BOUNDS;

    // Here we resolve the hierarchy as a stack to traverse backwards.
    List<ClassReference> stackRef = new ArrayList<>(chain);

    ClassReference sup = stackRef.remove(stackRef.size() - 1);
    while (!stackRef.isEmpty()) {
      ClassReference refNext = stackRef.remove(stackRef.size() - 1);
      if (refNext.assignedSuperVariables.containsKey(sup.clazz)) {
        Map<String, TypeReference> vars = refNext.assignedSuperVariables.get(sup.clazz);
        if (vars.containsKey(rawType)) {
          // The class contains an entry of the var name as generic so transform it to the
          // applied type.
          resolvedType = vars.get(rawType);
          if (resolvedType.isGeneric()) {
            TypeReference o = refNext.genericTypesMap.get(resolvedType.getBase());
            bounds = o.getBounds();
          }
        }
      }
      sup = refNext;
    }

    return bounds;
  }

  @Override
  public String toString() {
    return "ClassReference(" + this.clazz + ")";
  }

  public Class<?> getClazz() {
    return clazz;
  }

  public static ClassReference wrap(Class<?> clazz) {
    if (CACHE.containsKey(clazz)) {
      return CACHE.get(clazz);
    }
    return new ClassReference(clazz);
  }

  /**
   * @param superClazz The super-class or super-interface.
   * @return The map of resolved generic types.
   */
  public static Map<String, TypeReference> createTypeMap(
      Class<?> superClazzClazz, Type superClazz) {
    Type[] types = superClazzClazz.getTypeParameters();
    String rawSuperClazzName = superClazz.getTypeName();

    int height = 0;
    List<TypeReference> vars = new ArrayList<>();
    StringBuilder var = new StringBuilder();
    for (int x = 0; x < rawSuperClazzName.length(); x++) {
      char curr = rawSuperClazzName.charAt(x);
      if (curr == '<') {
        height++;
        if (height > 1) {
          var.append(curr);
        }
      } else if (curr == '>') {
        height--;
        if (height > 0) {
          var.append(curr);
        } else {
          break;
        }
      } else if (curr == ',' && height == 1) {
        vars.add(TypeReference.wrap(var.toString()));
        var = new StringBuilder();
      } else if (height != 0) {
        var.append(curr);
      }
    }

    if (!var.isEmpty()) {
      vars.add(TypeReference.wrap(var.toString()));
    }

    Map<String, TypeReference> map = new HashMap<>();
    for (int x = 0; x < types.length; x++) {
      map.put(types[x].getTypeName(), vars.get(x));
    }

    return map;
  }

  public static void clearCache() {
    CACHE.clear();
  }

  static class Bar {}

  static class Foo<F extends Bar> extends ArrayList<F> {}

  public static void main(String[] args) throws Exception {

    Class<?> deCl = Foo.class;
    Method mGet = deCl.getMethod("get", int.class);

    ClassReference reference = ClassReference.wrap(deCl);
    MethodReference methodReference = reference.getMethodReference(mGet);

    System.out.println(
        "Resolved return type: " + methodReference.getReturnReference().getResolvedType());
  }
}
