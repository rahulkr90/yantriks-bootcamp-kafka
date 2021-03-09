package com.yantriks.bootcamp.producer;

import com.yantriks.bootcamp.dto.EmployeeKafkaPayload;
import com.yantriks.bootcamp.service.CommonKafkaIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RestController
public class EmployeeKafkaProducer<K,V,R> {

    //@Value("${kafka.consumer.commit.interval:5s}")
    private Duration commitInterval;

  /*  private static final Logger log = LoggerFactory.getLogger(EmployeeKafkaProducer.class.getName());
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "demo-topic1";
    private  KafkaSender<K, V> sender;
    private  SimpleDateFormat dateFormat;*/

    @Autowired
    private CommonKafkaIntegrationService commonKafkaIntegrationService;



   // @PostConstruct
  /* public void  init(){
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer.class);
        //props.put(ProducerConfig.G)
        SenderOptions<Integer, String> senderOptions = SenderOptions.create(props);
        sender = (KafkaSender<K, V>) KafkaSender.create(senderOptions);
        dateFormat = new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy");

Ø
    }


    @PostMapping(value = "/send-message")
    public Mono<Void> sendMessageToTopic(@RequestBody  String message){
        String key= "test" + new Random().nextInt();
        Mono<SenderRecord<K,V,R>> recordsToSend = Mono.just((SenderRecord<K, V, R>) SenderRecord.create(
                new ProducerRecord<K, V>(TOPIC, null, null, (K) key, (V) message, null), "Hey"));
            sender.send(recordsToSend).doOnError(e-> log.error("Error")).subscribe();
        return null;
    }
*/


    @PostMapping(value = "/send-employee-detail")
    public Mono<Object> sendEmployeePayload(@RequestBody EmployeeKafkaPayload message){
        commonKafkaIntegrationService.postPayloadToTopic(message);
        return Mono.just(1);
    }


}
