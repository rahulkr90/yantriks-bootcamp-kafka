package com.yantriks.bootcamp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryUpdateKey {
    private String orgId;
    private String productId;
    private String locationType;
    private String locationId;
    private String uom;
}
