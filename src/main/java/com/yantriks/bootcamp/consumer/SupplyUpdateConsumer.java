package com.yantriks.bootcamp.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.yantriks.bootcamp.configuration.CustomKafkaConsumerProperties;
import com.yantriks.bootcamp.configuration.CustomKafkaProducerConfiguration;
import com.yantriks.bootcamp.configuration.KafkaConstants;
import com.yantriks.bootcamp.deserialization.KafkaConsumerConfigurationProperties;
import com.yantriks.bootcamp.dto.InventoryUpdateKey;
import com.yantriks.bootcamp.dto.InventoryUpdateValue;
import com.yantriks.bootcamp.dto.payload.InventoryKafkaPayload;
import com.yantriks.bootcamp.dto.payload.InventoryUpdatePayload;
import com.yantriks.bootcamp.dto.payload.SupplyToPayload;
import com.yantriks.bootcamp.service.CommonKafkaIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

@Component
public class SupplyUpdateConsumer<K,V,R> {

    private static  final Logger log = LoggerFactory.getLogger(SupplyUpdateConsumer.class);

    @Autowired
    private CustomKafkaConsumerProperties customKafkaConsumerProperties;

    @Autowired
    private CustomKafkaProducerConfiguration defaultProducerPropsConfiguration;


   /* @Autowired
    private InventorySupplyMapper inventorySupplyMapper;*/

    @Autowired
    protected Environment env;
    @Value("${kafka.consumer.commit.interval:5s}")
    private Duration commitInterval;
    @Value("${kafka.consumer.retry.limit:0}")
    private int retryLimit;
    @Value("${ypfp.kafka.topic-name-delimiter:-}")
    private String topicNameDelimiter;


    private String customerName;
    @Value("${streamer.custom-supply-update-topic}")
    private String customInventoryTopicName;

    @Value(("${streamer.inventory-update-topic}"))
    private String inventoryUpdateTopic;
    @Autowired
    private CommonKafkaIntegrationService commonKafkaIntegrationService;

    private KafkaSender<K, V> dlqSender;
    private KafkaSender<K, V> sender;
    @Autowired
    private ObjectMapper objectMapper;
    private SimpleDateFormat dateFormat;
    private KafkaConsumerConfigurationProperties config;

    @Autowired
    private WebClient webClient;

    public SupplyUpdateConsumer() {
    }


    @EventListener(ApplicationReadyEvent.class)
    protected void consume() {

        SenderOptions<K, V> senderOptions = SenderOptions.create(defaultProducerPropsConfiguration.getProducer());
        dlqSender = KafkaSender.create(senderOptions);
        sender = KafkaSender.create(senderOptions);
        dateFormat = new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy");
        Map<String, Object> consumerProps = new HashMap<>();
        final  String topicName =  commonKafkaIntegrationService.generateTopicName(customInventoryTopicName);
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
                messageInput(record,offset);
             //   offset.acknowledge();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    private void messageInput(ReceiverRecord<K, V> record, ReceiverOffset offset) throws JsonProcessingException {

        InventoryUpdateValue updateValue = new Gson().fromJson(getRequestBody(record),InventoryUpdateValue.class);
        Double tmpQty = updateValue.getTo().getSupplyTypes().stream().filter(item->!item.getSupplyType().equals("SOH")).
                mapToDouble(items->items.getQuantity()).sum();
        Double sohQty = updateValue.getTo().getSupplyTypes().stream().filter(item->item.getSupplyType().equals("SOH")).
                mapToDouble(items-> items.getQuantity()).sum();

        Double onhandQty =  sohQty - tmpQty;

        InventoryUpdatePayload actualInventoryUpdateValue = new InventoryUpdatePayload();
        actualInventoryUpdateValue.setAudit(updateValue.getAudit());
        actualInventoryUpdateValue.setEventType(updateValue.getEventType());
        actualInventoryUpdateValue.setLocationId(updateValue.getLocationId());
        actualInventoryUpdateValue.setEventType(updateValue.getEventType());
        actualInventoryUpdateValue.setLocationType(updateValue.getLocationType());
        actualInventoryUpdateValue.setFeedType(updateValue.getFeedType());
        actualInventoryUpdateValue.setUpdateTimeStamp(updateValue.getUpdateTimeStamp());
        actualInventoryUpdateValue.setOrgId(updateValue.getOrgId());
        actualInventoryUpdateValue.setTo(new SupplyToPayload("ONHAND",updateValue.getTo().getSupplyTypes().get(0).getSegment()));
        actualInventoryUpdateValue.setProductId(updateValue.getProductId());
        actualInventoryUpdateValue.setQuantity(onhandQty);
        actualInventoryUpdateValue.setUom(updateValue.getUom());

        InventoryKafkaPayload kafkaPayload =  new InventoryKafkaPayload(false, (InventoryUpdateKey) record.key(),"CREATE",inventoryUpdateTopic,
                actualInventoryUpdateValue);
        ObjectMapper mapper = new ObjectMapper();
       String message =  mapper.writeValueAsString(kafkaPayload);
        System.out.println("Message is" + message);

        callKafkaService(message,offset);



       /* String inputMessage = buildSupplyUpdateRestProxy(record, inventoryUpdateTopic,
                "CREATE", false, formSupplyToPayload("ONHAND"), onhandQty);*/


       // System.out.println(record.value());

    }

    private void callKafkaService(String message, ReceiverOffset offset) {

        webClient.post().uri("ttp://localhost:8094/kafka-rest-services")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(message))
                .retrieve()
                .bodyToMono(String.class).subscribe();
        offset.acknowledge();
    }



    private String buildRecordBody(Object v) throws JsonProcessingException {

        DateTimeFormatter desiredFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        LocalDateTime dateTime = LocalDateTime.parse(LocalDateTime.now(ZoneOffset.UTC).toString());
        String updateTime = dateTime.format(desiredFormatter);

        String supplyDetail = objectMapper.writeValueAsString(v);
        JsonNode supplyDetailNode = objectMapper.readTree(supplyDetail);
       // JsonNode availabilityByProducts = atpDetailNode.get("availabilityByProducts");
        ObjectNode jsonObject = objectMapper.createObjectNode().objectNode();
        ObjectNode recordObject = objectMapper.createObjectNode().objectNode();
        ObjectNode valueObject = objectMapper.createObjectNode().objectNode();
        recordObject.set("value", valueObject);

        valueObject.put("operationType", "CREATE");
        valueObject.put("updateTime", updateTime.toString());
        ObjectNode entityObject = objectMapper.createObjectNode().objectNode();
        valueObject.set("audit", entityObject);
       /* entityObject.put("transactionType", supplyDetailNode.get("transactionType").asText());
        entityObject.put("sellingChannel", atpDetailNode.get("sellingChannel").asText());
        entityObject.put("orgId", atpDetailNode.get("orgId").asText());
*/
        ObjectNode keyObject = objectMapper.createObjectNode().objectNode();
        recordObject.set("key", keyObject);

      /*  keyObject.put("transactionType", atpDetailNode.get("transactionType").asText());
        keyObject.put("uom", availabilityByProducts.get(0).get("uom").asText());
        keyObject.put("productId", availabilityByProducts.get(0).get("productId").asText());
        keyObject.put("sellingChannel", atpDetailNode.get("sellingChannel").asText());
        keyObject.put("orgId", atpDetailNode.get("orgId").asText());*/
        log.info("ATP Threshold Update Time {} ", updateTime);
        return jsonObject.toString();

    }

    private double calculateOnHandQty(Double stockOnHandQty, List factoredSupplyTypes, Map<String, Double> supplyTypeMap) {
        double tempQty = factoredSupplyTypes.stream().
                filter(item -> !("SOH".equals(item)) && supplyTypeMap.get(item) != null).
                mapToDouble(supplyTypeMap::get).sum();
        return stockOnHandQty - tempQty;
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
        consumerProps.put(JsonDeserializer.KEY_DEFAULT_TYPE, InventoryUpdateKey.class);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, InventoryUpdateValue.class);
        if (groupId != null) {
            consumerProps.put(GROUP_ID_CONFIG, groupId);
        }
    }
}
