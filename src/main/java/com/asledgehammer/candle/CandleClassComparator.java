package com.asledgehammer.candle;

import java.util.Comparator;

class CandleClassComparator implements Comparator<Class<?>> {

  static CandleClassComparator INSTANCE = new CandleClassComparator();

  @Override
  public int compare(Class<?> o1, Class<?> o2) {
    int compare = o1.getSimpleName().compareTo(o2.getSimpleName());
    if (compare == 0) return o1.getName().compareTo(o2.getName());
    return compare;
  }
}
