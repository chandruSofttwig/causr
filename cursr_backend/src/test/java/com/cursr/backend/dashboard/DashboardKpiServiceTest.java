package com.cursr.backend.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardKpiServiceTest {

  @Test
  void serviceRowStatus_redWhenHighErrorOrLatency() {
    assertThat(
            DashboardKpiService.serviceRowStatus(
                Map.of("error_percent", 6.0, "p99_ms", 100.0)))
        .isEqualTo("RED");
    assertThat(
            DashboardKpiService.serviceRowStatus(
                Map.of("error_percent", 0.5, "p99_ms", 900.0)))
        .isEqualTo("RED");
  }

  @Test
  void serviceRowStatus_amberWhenModerate() {
    assertThat(
            DashboardKpiService.serviceRowStatus(
                Map.of("error_percent", 2.0, "p99_ms", 100.0)))
        .isEqualTo("AMBER");
    assertThat(
            DashboardKpiService.serviceRowStatus(
                Map.of("error_percent", 0.1, "p99_ms", 350.0)))
        .isEqualTo("AMBER");
  }

  @Test
  void serviceRowStatus_greenWhenLow() {
    assertThat(
            DashboardKpiService.serviceRowStatus(
                Map.of("error_percent", 0.5, "p99_ms", 150.0)))
        .isEqualTo("GREEN");
  }
}
