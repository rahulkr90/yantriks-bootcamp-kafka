package com.yantriks.bootcamp.dto.payload;

import lombok.*;

@Builder
@Setter
@Getter
@AllArgsConstructor
public class SupplyToPayload {
    String supplyType;
    String segment;
}
