package com.yantriks.bootcamp.dto;

import com.yantriks.bootcamp.dto.payload.SupplyToPayload;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ActualInventoryUpdateValue {
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
