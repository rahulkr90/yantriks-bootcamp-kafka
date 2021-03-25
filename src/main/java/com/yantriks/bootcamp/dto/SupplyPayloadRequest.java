package com.yantriks.bootcamp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter

public class SupplyPayloadRequest {
    private Map<String,Double> quantityMap;
    private List<String> payloadList;

    public SupplyPayloadRequest(){
            quantityMap = new HashMap<>();
            payloadList = new ArrayList<>();
    }
}
