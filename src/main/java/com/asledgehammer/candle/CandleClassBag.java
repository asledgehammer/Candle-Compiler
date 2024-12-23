package com.asledgehammer.candle;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.util.*;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

public class CandleClassBag {

  private final List<Class<?>> classes = new ArrayList<>();

  public CandleClassBag() {
    addClasses();
    classes.sort(Comparator.comparing(Class::getSimpleName));
  }

  private void addClasses() {
    addClass(LuaManager.GlobalObject.class);

    ParserConfiguration config = new ParserConfiguration();
    config.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
    config.setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver(false)));
    JavaParser parser = new JavaParser(config);

    ParseResult<CompilationUnit> result;
    try {
      result = parser.parse(new File("./LuaManager.java"));
    } catch(FileNotFoundException exception) {
      exception.printStackTrace();
      return;
    }

    if (!result.isSuccessful()) {
      System.err.println("LuaManager.java could not be parsed.");
      return;
    }

    assert(result.getResult().isPresent());
    CompilationUnit unit = result.getResult().get();

    Optional<ClassOrInterfaceDeclaration> luaManager = unit.getClassByName("LuaManager");
    if (luaManager.isEmpty()) {
      System.err.println("No such class LuaManager");
      return;
    }

    ClassOrInterfaceDeclaration exposer = null;
    for (BodyDeclaration<?> member : luaManager.get().getMembers()) {
      if (!member.isClassOrInterfaceDeclaration()) continue;
      ClassOrInterfaceDeclaration clazzDeclaration = member.asClassOrInterfaceDeclaration();

      if (clazzDeclaration.getNameAsString().equals("Exposer")) {
        exposer = member.asClassOrInterfaceDeclaration();
        break;
      }
    }

    if (exposer == null) {
      System.err.println("zombie.Lua.LuaManager has no nested class Exposer.");
      return;
    }

    List<MethodDeclaration> exposeAllList = exposer.getMethodsByName("exposeAll");
    if (exposeAllList.isEmpty()) {
      System.err.println("zombie.Lua.LuaManager.Exposer has no such method exposeAll().");
      return;
    }

    Optional<BlockStmt> methodBody = exposeAllList.get(0).getBody();
    if (methodBody.isEmpty()) {
      System.err.println("Method zombie.Lua.LuaManager.Exposer.exposeAll() body is empty.");
      return;
    }

    for (Statement statement: methodBody.get().getStatements()) {
      if (!statement.isExpressionStmt()) continue;
      Expression expression = ((ExpressionStmt)statement).getExpression();

      if (!expression.isMethodCallExpr()) continue;
      MethodCallExpr methodCall = (MethodCallExpr)expression;

      if (!methodCall.getNameAsString().equals("setExposed")) continue;

      NodeList<Expression> arguments = methodCall.getArguments();
      if (arguments.isEmpty()) continue;
      Expression arg = arguments.get(0);

      // there's another method named setExposed called in this method with a different argument type
      if (!arg.isClassExpr()) continue;
      ClassExpr clazz = methodCall.getArguments().get(0).asClassExpr();

      String typeName = clazz.getType().resolve().asReferenceType().getQualifiedName();

      // convert . to $ in nested class names
      String shortType = clazz.getTypeAsString();
      if (shortType.contains(".")) {
        typeName = typeName.substring(0, typeName.length() - shortType.length()) + shortType.replace('.', '$');
      }

      try {
        Class<?> exposedClazz = Class.forName(typeName, false, this.getClass().getClassLoader());
        addClass(exposedClazz);
      } catch(ClassNotFoundException exception) {
        System.out.println("Cannot find exposed type " + typeName);
      }
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
}
