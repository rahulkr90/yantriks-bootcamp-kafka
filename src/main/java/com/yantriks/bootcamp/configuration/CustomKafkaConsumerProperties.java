package com.yantriks.bootcamp.configuration;

import com.yantriks.bootcamp.deserialization.JacksonDeserializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer2;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;


@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
@RequiredArgsConstructor
@Configuration
public class CustomKafkaConsumerProperties {

    private final String keyDeSerializerClassName;
    private final String valueDeSerializerClassName;

    protected Map<String, Object> consumer;
    public CustomKafkaConsumerProperties() {
        keyDeSerializerClassName = ErrorHandlingDeserializer2.class.getName();
        valueDeSerializerClassName = ErrorHandlingDeserializer2.class.getName();
    }

    @PostConstruct
    private void init() {
        if (consumer == null) {
            consumer = new HashMap<>();
        } else {
            consumer = flatten(consumer);
            setKeyDeSerializer();
            setValueDeSerializer();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flatten(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        map.forEach((key, value) -> {
            if (value instanceof Map) {
                Map<String, Object> nestedMap = flatten((Map<String, Object>) value);
                nestedMap.forEach((nestedKey, nestedValue) ->
                        result.put(key + "." + nestedKey, nestedValue));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    private void setKeyDeSerializer() {
        consumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeSerializerClassName);
        if (keyDeSerializerClassName.equals(ErrorHandlingDeserializer2.class.getName())) {
            consumer.put(ErrorHandlingDeserializer2.KEY_DESERIALIZER_CLASS, JacksonDeserializer.class.getName());
        }

    }

    private void setValueDeSerializer() {
        consumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeSerializerClassName);
        if (valueDeSerializerClassName.equals(ErrorHandlingDeserializer2.class.getName())) {
            consumer.put(ErrorHandlingDeserializer2.VALUE_DESERIALIZER_CLASS, JacksonDeserializer.class.getName());
        }
    }
}
