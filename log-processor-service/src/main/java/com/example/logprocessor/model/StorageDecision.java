package com.example.logprocessor.model;

public enum StorageDecision {
  /** Do not persist. */
  DROP,
  /** High-signal logs; short retention (logs_hot). */
  HOT,
  /** Random sample of remaining logs; long retention (logs_cold). */
  COLD
}
