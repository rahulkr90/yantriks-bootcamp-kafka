package com.yantriks.bootcamp.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EmployeePayload {
    private String id;
    private String organizationId;
    private String employeeName;
    private String contactNumber;
    private String role;
    private Address address;

}
