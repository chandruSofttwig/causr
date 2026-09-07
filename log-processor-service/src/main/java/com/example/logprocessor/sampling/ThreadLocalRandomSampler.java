package com.example.logprocessor.sampling;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class ThreadLocalRandomSampler implements RandomSampler {

  @Override
  public double nextUnitDouble() {
    return ThreadLocalRandom.current().nextDouble();
  }
}
