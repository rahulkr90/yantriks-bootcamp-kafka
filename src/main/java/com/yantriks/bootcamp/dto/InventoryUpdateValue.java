package com.yantriks.bootcamp.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InventoryUpdateValue {
    private String orgId;
    private String productId;
    private String locationType;
    private Boolean overrideZoneTransitionRule;
    private String locationId;
    private String feedType;
    private String uom;
    private String eventType;
    private Audit audit;
    private SupplyTo to;
    private String updateTimeStamp;
    private Double quantity;
}
