package com.yantriks.bootcamp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantriks.bootcamp.configuration.CustomKafkaConsumerProperties;
import com.yantriks.bootcamp.configuration.CustomKafkaProducerConfiguration;
import com.yantriks.bootcamp.deserialization.KafkaConsumerConfigurationProperties;
import com.yantriks.bootcamp.dto.EmployeeKafkaPayload;
import com.yantriks.bootcamp.dto.EmployeePayload;
import com.yantriks.bootcamp.dto.EmployeeUpdateKey;
import com.yantriks.bootcamp.configuration.KafkaConstants;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

@Component
public class CommonKafkaIntegrationService<K,V,R> {
    private static  final Logger log = LoggerFactory.getLogger(CommonKafkaIntegrationService.class);
    @Autowired
    private CustomKafkaConsumerProperties customKafkaConsumerProperties;

    @Autowired
    private CustomKafkaProducerConfiguration defaultProducerPropsConfiguration;
    @Autowired
    protected Environment env;
    @Value("${kafka.consumer.commit.interval:5s}")
    private Duration commitInterval;
    @Value("${kafka.consumer.retry.limit:0}")
    private int retryLimit;
    @Value("${ypfp.kafka.topic-name-delimiter:-}")
    private String topicNameDelimiter;
    private KafkaSender<K, V> dlqSender;
    private KafkaSender<K, V> sender;
    @Autowired
    private ObjectMapper objectMapper;
    private SimpleDateFormat dateFormat;
    private KafkaConsumerConfigurationProperties config;
    @Value("${ypfp.customer-name}")
    private String customerName;
    @Value("${streamer.employee-update-topic}")
    private String consumerTopicName;

    @Value(("${streamer.employee-target-topic}"))
    private String employeeTargetTopicName;

   // final String consumerTopicName ="employee-detail-topic";
    //final String employeeTargetTopicName = "employee-detail-target-topic";

    @PostConstruct
    protected void init(){
      /*  Map<String, Object> consumerProps = new HashMap<>();
        config = generateKafkaConsumerConfigurationProperties(consumerTopicName);

        consumerProps.putAll(customKafkaConsumerProperties.getConsumer());
        configureConsumerProperties(consumerProps, config.getGroupId());*/
        SenderOptions<K, V> senderOptions = SenderOptions.create(defaultProducerPropsConfiguration.getProducer());
        dlqSender = KafkaSender.create(senderOptions);
        sender = KafkaSender.create(senderOptions);
    }

   /* @EventListener(ApplicationReadyEvent.class)
    public void consume() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.putAll(customKafkaConsumerProperties.getConsumer());
        configureConsumerProperties(consumerProps, config.getGroupId());
        ReceiverOptions<K, V> receiverOptions =
                ReceiverOptions.<K, V>create(consumerProps)
                        .subscription(Collections.singleton(consumerTopicName))
                        .commitInterval(commitInterval)
                        .addAssignListener(p -> log.info("Group partitions assigned: {}", p))
                        .addRevokeListener(p -> log.info("Group partitions revoked: {}", p));
        createKafkaReceiver(receiverOptions,null);
    }*/

  /*  private Disposable createKafkaReceiver(ReceiverOptions<K, V> receiverOptions, String dlqTopicName){
        Flux<ReceiverRecord<K, V>> kafkaFlux = KafkaReceiver.create(receiverOptions).receive();
        return kafkaFlux.subscribe(record->{
            ReceiverOffset offset =  record.receiverOffset();
            log.info("Received record: {}", record.value().toString());
            try {
               // System.out.println(getRequestBody(record));
                if (checkForMandatoryAttributes(record,offset)){
                    postMessageToTargetTopic(record,employeeTargetTopicName);
                }else {
                    postMessageToDLQTopic(record,consumerTopicName);
                }
                offset.acknowledge();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

        });


    }
*/
    protected KafkaConsumerConfigurationProperties generateKafkaConsumerConfigurationProperties(String topicName) {
        return KafkaConsumerConfigurationProperties.builder()
                .consumerEnabled(env.getProperty(String.format(KafkaConstants.CONSUMER_ENABLED_PROPERTY_OVERRIDE, topicName), Boolean.class, true))
                .commitInterval(env.getProperty(String.format(KafkaConstants.COMMIT_INTERVAL_PROPERTY_OVERRIDE, topicName), Duration.class, commitInterval))
                .retryLimit(env.getProperty(String.format(KafkaConstants.RETRY_LIMIT_PROPERTY_OVERRIDE, topicName), Integer.class, retryLimit))
                .groupId(env.getProperty(KafkaConstants.OVERRIDE_KAFKA_TOPIC_GROUP_ID_PROPERTY_PREFIX + topicName))
                .build();
    }
    protected void configureConsumerProperties(Map<String, Object> consumerProps, String groupId) {
        consumerProps.put(JsonDeserializer.KEY_DEFAULT_TYPE, EmployeeUpdateKey.class);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EmployeePayload.class);
        if (groupId != null) {
            consumerProps.put(GROUP_ID_CONFIG, groupId);
        }
    }

    public void postMessageToTopic(ReceiverRecord<K,V> record, String topicName, KafkaSender<K,V> sender, ReceiverOffset offset){
        Headers headers = new RecordHeaders(Arrays.stream(record.headers().toArray())
                .collect(Collectors.toList()));
        Mono<SenderRecord<K, V, R>> recordsToSend = (Mono<SenderRecord<K, V, R>>) Mono.just((SenderRecord<K, V, R>) SenderRecord.create(
                new ProducerRecord<K, V>(generateTopicName(topicName), null, record.timestamp(),(K)record.key(),(V)record.value(), headers), record.key()));
        sender.send(recordsToSend).doOnError(e->log.error("Error")).subscribe();

    }

    public void postMessageToDLQTopic(ReceiverRecord<K,V> record, String topicName, KafkaSender<K,V> sender, ReceiverOffset offset){
        Headers headers = new RecordHeaders(Arrays.stream(record.headers().toArray())
                .collect(Collectors.toList()));
        Mono<SenderRecord<K, V, R>> recordsToSend = (Mono<SenderRecord<K, V, R>>) Mono.just((SenderRecord<K, V, R>) SenderRecord.create(
                new ProducerRecord<K, V>(generateDLQTopicName(topicName), null, record.timestamp(),(K)record.key(),(V)record.value(), headers), record.key()));
        sender.send(recordsToSend).doOnError(e->log.error("Error")).subscribe();

    }

    public void postPayloadToTopic(EmployeeKafkaPayload employeeKafkaPayload){
        Mono<SenderRecord<K, V, R>> recordsToSend = (Mono<SenderRecord<K, V, R>>) Mono.just((SenderRecord<K, V, R>) SenderRecord.create(
                new ProducerRecord<K, V>(generateTopicName(employeeKafkaPayload.getTopic()), null, null,(K)employeeKafkaPayload.getKey(), (V) employeeKafkaPayload.getValue(), null), employeeKafkaPayload.getKey()));
        sender.send(recordsToSend).doOnError(e->log.error("Error")).subscribe();
    }

    public String generateTopicName(String topicName) {
        topicName = KafkaConstants.HYPHEN.equals(topicNameDelimiter) ? topicName : topicName.replace(KafkaConstants.HYPHEN, topicNameDelimiter);
        String generatedTopicName = String.join(topicNameDelimiter, customerName, topicName);
        return env.getProperty(KafkaConstants.OVERRIDE_KAFKA_TOPIC_NAME_PROPERTY_PREFIX + generatedTopicName, generatedTopicName);
    }

    public String generateDLQTopicName(String topicName) {
         topicName = KafkaConstants.HYPHEN.equals(topicNameDelimiter) ? topicName:topicName.replace(KafkaConstants.HYPHEN, topicNameDelimiter);
        String dlqTopicName = String.join(topicNameDelimiter, customerName, topicName,"DLQ");
        return env.getProperty(KafkaConstants.OVERRIDE_KAFKA_TOPIC_NAME_PROPERTY_PREFIX + dlqTopicName, dlqTopicName);
    }

}
