package com.yantriks.bootcamp.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

public class KafkaConstants {
  public static final List<String> KAFKA_TOPIC_NAME_DELIMITERS = ImmutableList.of("", "-", "_", ".");
  public static final String INTEGRATION_SERVICE_QUERY_PARAM = "integrationService";
  public static final String REPLICATE_ENDPOINT = "/replicate";
  public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
  public static final String OPERATION_HEADER = "OPERATION";
  public static final String PUBLISH_TIME = "publishTime";
  public static final String STATUS_CODE = "status_code";
  public static final String ERROR_CODE = "ERROR_CODE";
  public static final String HYPHEN = "-";
  public static final String DG_FORCED_ERROR_MESSAGE="Location Streamer Forced During DG Load";

  //Operations
  public static final String OPERATION_CREATE="CREATE";
  public static final String OPEARTION_MODIFY="MODIFY";

  // Application properties name prefix
  public static final String OVERRIDE_KAFKA_TOPIC_GROUP_ID_PROPERTY_PREFIX = "override.group-id.";
  public static final String OVERRIDE_KAFKA_TOPIC_NAME_PROPERTY_PREFIX = "override.topic-name.";

  // Application properties names
  public static final String SCHEDULER_TIME_TO_LIVE_PROPERTY_OVERRIDE = "kafka.consumer.override.%s.scheduler.time-to-live";
  public static final String DLQ_PUBLISH_ENABLED_PROPERTY_OVERRIDE = "kafka.consumer.override.%s.dlq-publish.enabled";
  public static final String COMMIT_INTERVAL_PROPERTY_OVERRIDE = "kafka.consumer.override.%s.commit.interval";
  public static final String BACKOFF_INITIAL_PROPERTY_OVERRIDE = "kafka.consumer.override.%s.retry.backoff.initial";
  public static final String BACKOFF_MAX_PROPERTY_OVERRIDE = "kafka.consumer.override.%s.retry.backoff.max";
  public static final String RETRY_LIMIT_PROPERTY_OVERRIDE = "kafka.consumer.override.%s.retry.limit";
  public static final String CONSUMER_ENABLED_PROPERTY_OVERRIDE = "kafka.consumer.override.%s.enabled";

  public static final String PRODUCER_ENABLED_PROPERTY_OVERRIDE = "kafka.producer.override.%s.enabled";

  public static final Set<String> IMMUTABLE_KAFKA_TOPIC = ImmutableSet.of("yas-location-transaction-type-config-internal-cache-updates");

}
