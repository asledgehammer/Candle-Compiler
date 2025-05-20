package com.asledgehammer.candle;

import org.objectweb.asm.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

public class CandleClassBag {

  private final List<Class<?>> classes = new ArrayList<>();

  public CandleClassBag() {
    addClasses();
    classes.sort(Comparator.comparing(Class::getSimpleName));
  }

  private void addClassesFromJar(Path jar) {
    try (FileSystem gameJar = FileSystems.newFileSystem(jar)) {
      Path exposerPath = gameJar.getPath("zombie", "Lua", "LuaManager$Exposer.class");
      if (!Files.exists(exposerPath) || !Files.isRegularFile(exposerPath)) {
        return;
      }

      ClassReader reader = new ClassReader(
              Files.newInputStream(exposerPath)
      );
      reader.accept(new ClassVisitor(Opcodes.ASM9) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
          if (name.equals("exposeAll")) {
            return new ExposeMethodVisitor(
                    api,
                    super.visitMethod(access, name, descriptor, signature, exceptions)
            );
          }
          return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
      }, 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addClasses() {
    addClass(LuaManager.GlobalObject.class);

    // name of game jar was previously unspecified, so to maintain compatibility we must check all jars
    try (Stream<Path> paths = Files.list(Path.of("lib"))) {
      paths
           .filter((path) -> path.getFileName().toString().endsWith(".jar"))
           .forEach(this::addClassesFromJar);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addClass(Class<?> clazz) {
    if (classes.contains(clazz) || isExempt(clazz)) return;
    classes.add(clazz);
    Class<?> superClazz = clazz.getSuperclass();
    // TODO: if the superclass is not actually exposed, static members and constructors should not be rendered
    if (superClazz != null) addClass(superClazz);

    Class<?>[] interfazes = clazz.getInterfaces();
    for (Class<?> interfaze: interfazes) {
      if (classes.contains(interfaze)) continue;
      classes.add(interfaze);
    }
  }

  public List<Class<?>> getClasses() {
    return classes;
  }

  public static boolean isExempt(Class<?> clazz) {
    return clazz == boolean.class
        | clazz == Boolean.class
        | clazz == byte.class
        | clazz == Byte.class
        | clazz == short.class
        | clazz == Short.class
        | clazz == int.class
        | clazz == Integer.class
        | clazz == float.class
        | clazz == Float.class
        | clazz == double.class
        | clazz == Double.class
        | clazz == long.class
        | clazz == Long.class
        | clazz == String.class
        | clazz == char.class
        | clazz == Character.class
        | clazz == Object.class
        | clazz == void.class
        | clazz == Void.class
        | clazz == KahluaTable.class;
  }

  /// Method visitor that exposes classes exposed by the method.
  private class ExposeMethodVisitor extends MethodVisitor {
    protected ExposeMethodVisitor(int api, MethodVisitor methodVisitor) {
      super(api, methodVisitor);
    }

    /// The class on the top of the stack.
    Type topClass = null;

    @Override
    public void visitLdcInsn(Object value) {
      if (value instanceof Type type) {
        // this makes the assumption that any class pushed will not be removed before the next call to setExposed
        // even though this is dumb, in practice for this limited usage it seems reliable
        // there isn't any need to do anything else with class objects in that method
        topClass = type;
      }
      super.visitLdcInsn(value);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
      if (owner.equals("zombie/Lua/LuaManager$Exposer") && name.equals("setExposed")) {
        assert topClass != null;
        try {
          Class<?> exposedClazz = Class.forName(topClass.getClassName(), false, this.getClass().getClassLoader());
          addClass(exposedClazz);
          topClass = null;
        } catch(ClassNotFoundException exception) {
          System.out.println("Cannot find exposed type " + topClass.getClassName());
        }
      }
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
  }
}
