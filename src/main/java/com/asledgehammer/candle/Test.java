package com.asledgehammer.candle;

import zombie.core.math.PZMath;

import java.lang.reflect.Method;

public class Test {

  public static void main(String[] yargs) {
    for (Method method : PZMath.class.getDeclaredMethods()) {
      if (method.getName().equals("clamp")) {
        System.out.println(method);
      }
    }
  }
}
