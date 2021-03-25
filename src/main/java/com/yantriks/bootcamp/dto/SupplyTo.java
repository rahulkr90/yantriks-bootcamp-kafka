package com.yantriks.bootcamp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SupplyTo {
    private List<SupplyType> supplyTypes;
}
