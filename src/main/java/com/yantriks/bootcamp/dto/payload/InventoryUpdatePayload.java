package com.yantriks.bootcamp.dto.payload;

import com.yantriks.bootcamp.dto.Audit;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InventoryUpdatePayload {
    private String orgId;
    private String productId;
    private String locationType;
    private Boolean overrideZoneTransitionRule;
    private String locationId;
    private String feedType;
    private String uom;
    private String eventType;
    private Audit audit;
    private SupplyToPayload to;
    private String updateTimeStamp;
    private Double quantity;
}
