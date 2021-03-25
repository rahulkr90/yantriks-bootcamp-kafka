package com.yantriks.bootcamp.dto.payload;

import com.yantriks.bootcamp.dto.InventoryUpdateKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class InventoryKafkaPayload {
    private Boolean isFullyQualifiedTopicName;
    private InventoryUpdateKey key;
    private String operation;
    private String topic;
    private InventoryUpdatePayload value;
  //  private Audit audit;
}
