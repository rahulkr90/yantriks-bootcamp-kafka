package com.yantriks.bootcamp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan( basePackages = "com.yantriks.bootcamp")
public class ReactiveKafkaDemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(ReactiveKafkaDemoApplication.class, args);
	}
}
