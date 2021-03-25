package com.yantriks.bootcamp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Audit {
    private String transactionId;
    private String transactionReason;
    private String transactionSystem;
    private String transactionType;
    private String transactionUser;
}
