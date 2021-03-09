package com.yantriks.bootcamp.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.yantriks.bootcamp.configuration.CustomKafkaConsumerProperties;
import com.yantriks.bootcamp.configuration.CustomKafkaProducerConfiguration;
import com.yantriks.bootcamp.deserialization.KafkaConsumerConfigurationProperties;
import com.yantriks.bootcamp.dto.EmployeePayload;
import com.yantriks.bootcamp.dto.EmployeeUpdateKey;
import com.yantriks.bootcamp.service.CommonKafkaIntegrationService;
import com.yantriks.bootcamp.configuration.KafkaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

@Component
public class EmployeeKafkaConsumer<K,V,R> {
    private static  final Logger log = LoggerFactory.getLogger(EmployeeKafkaConsumer.class);
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
    private  SimpleDateFormat dateFormat;
    private KafkaConsumerConfigurationProperties config;

    private String customerName;
    @Value("${streamer.employee-update-topic}")
    private String consumerTopicName;

    @Value(("${streamer.employee-target-topic}"))
    private String employeeTargetTopicName;
    @Autowired
    private CommonKafkaIntegrationService commonKafkaIntegrationService;
    @EventListener(ApplicationReadyEvent.class)
    protected void consume() {

        SenderOptions<K, V> senderOptions = SenderOptions.create(defaultProducerPropsConfiguration.getProducer());
        dlqSender = KafkaSender.create(senderOptions);
        sender = KafkaSender.create(senderOptions);
        dateFormat = new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy");
        Map<String, Object> consumerProps = new HashMap<>();
        final  String topicName =  commonKafkaIntegrationService.generateTopicName(consumerTopicName);
        config = generateKafkaConsumerConfigurationProperties(topicName);
        consumerProps.putAll(customKafkaConsumerProperties.getConsumer());
        configureConsumerProperties(consumerProps, config.getGroupId());
        ReceiverOptions<K, V> receiverOptions =
                    ReceiverOptions.<K, V>create(consumerProps)
                            .subscription(Collections.singleton(topicName))
                            .commitInterval(commitInterval)
                            .addAssignListener(p -> log.info("Group partitions assigned: {}", p))
                            .addRevokeListener(p -> log.info("Group partitions revoked: {}", p));
            createKafkaReceiver(receiverOptions,null);
        }

        private Disposable createKafkaReceiver(ReceiverOptions<K, V> receiverOptions, String dlqTopicName){
        Flux<ReceiverRecord<K, V>> kafkaFlux = KafkaReceiver.create(receiverOptions).receive();
        return kafkaFlux.subscribe(record->{
            ReceiverOffset offset =  record.receiverOffset();
            log.info("Received record: {}", record);
            try {
                log.info("Message posted in topic is: {}" , getRequestBody(record));
                if (checkForMandatoryAttributes(record,offset)){
                    log.info("Mandatory parameter check passed :: Messages will be posted to target topic");
                    commonKafkaIntegrationService.postMessageToTopic(record,employeeTargetTopicName,sender,offset);
                    offset.acknowledge();
                }else {
                    log.info("Mandatory parameter check failed :: Messages will be posted to DLQ");
                    commonKafkaIntegrationService.postMessageToDLQTopic(record,consumerTopicName,dlqSender,offset);
                    offset.acknowledge();
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean checkForMandatoryAttributes(ReceiverRecord<K, V> record, ReceiverOffset offset) throws JsonProcessingException {
        EmployeePayload employeePayload = new Gson().fromJson(getRequestBody(record),EmployeePayload.class);
        if(employeePayload.getContactNumber() == null || employeePayload.getContactNumber().isEmpty()){
                return false;
        }
        if(employeePayload.getId() == null || employeePayload.getId().isEmpty()){
            return false;
        }
        if(employeePayload.getOrganizationId() == null || employeePayload.getOrganizationId().isEmpty()){
            return false;
        }
        if(employeePayload.getEmployeeName() == null || employeePayload.getEmployeeName().isEmpty()){
            return false;
        }
        return true;
    }

    private String getRequestBody(ReceiverRecord<K,V>record) throws JsonProcessingException {
        return record.value() != null ? objectMapper.writeValueAsString(record.value()) : null;

    }

    private KafkaConsumerConfigurationProperties generateKafkaConsumerConfigurationProperties(String topicName) {
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

}


