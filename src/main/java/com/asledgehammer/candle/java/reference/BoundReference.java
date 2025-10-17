package com.asledgehammer.candle.java.reference;

/** Any reference that supports type-boundaries. */
public interface BoundReference {
  /**
   * @return The upper type-boundaries allowed as an array.
   */
  TypeReference[] getBounds();
}
