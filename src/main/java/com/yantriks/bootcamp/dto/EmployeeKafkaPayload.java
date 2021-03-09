package com.yantriks.bootcamp.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeKafkaPayload {
    private  EmployeeUpdateKey key;
    private String topic;
    private  EmployeePayload value;

}
