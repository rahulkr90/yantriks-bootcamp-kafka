package com.yantriks.bootcamp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SupplyType {
    private String supplyType;
    private Double quantity;
    private String segment;
}
