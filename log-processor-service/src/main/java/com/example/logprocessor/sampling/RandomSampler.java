package com.example.logprocessor.sampling;

@FunctionalInterface
public interface RandomSampler {

  /** Uniform double in [0.0, 1.0). */
  double nextUnitDouble();
}
