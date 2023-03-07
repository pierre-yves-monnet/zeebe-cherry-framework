package io.camunda.cherry.db.entity;

import io.camunda.cherry.definition.AbstractRunner;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "RUNNEREXECUTION")
public class RunnerExecutionEntity {

  @Column(name = "type_executor", length = 10)
  @Enumerated(EnumType.STRING)
  public TypeExecutor typeExecutor;
  @Column(name = "runner_type", length = 100)
  public String runnerType;

  @Column(name = "execution_time")
  public Instant executionTime;
  @Column(name = "execution_ms")
  public Long executionMs;
  @Column(name = "status", length = 100)
  @Enumerated(EnumType.STRING)
  public AbstractRunner.ExecutionStatusEnum status;
  @Column(name = "error_code", length = 100)
  public String errorCode;
  @Column(name = "error_explanation", length = 500)
  public String errorExplanation;
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  public enum TypeExecutor {CONNECTOR, WORKER, WATCHER}

  // Save Log information: https://github.com/janzyka/blobs-jpa/

}
