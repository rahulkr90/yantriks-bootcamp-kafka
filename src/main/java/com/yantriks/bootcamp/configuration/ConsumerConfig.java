package com.yantriks.bootcamp.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ConsumerConfig {
/*
  @Value("${api.retry.connectTimeOut}")
  private Integer connectTimeOut;
  @Value("${api.retry.readTimeOut}")
  private Integer readTimeOut;*/

    @Bean
    ObjectMapper objectMapper(){

        return new ObjectMapper();
    }
    @Bean
    public WebClient webClient(WebClient.Builder builder) {

        //int connectTimeOut, long readTimeOut
   /* HttpClient httpClient = HttpClient.create()
        .tcpConfiguration(tcpClient -> {
          tcpClient = tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeOut);
          tcpClient = tcpClient.doOnConnected(conn -> conn
              .addHandlerLast(new ReadTimeoutHandler(readTimeOut, TimeUnit.MILLISECONDS)));
          return tcpClient;
        });
    // create a client http connector using above http client
    ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
    // use this configured http connector to build the web client
    return builder.clientConnector(connector).build();
  }*/

        return builder.build();
    }
}
