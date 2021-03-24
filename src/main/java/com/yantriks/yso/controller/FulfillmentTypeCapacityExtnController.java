package com.yantriks.yso.controller;

import com.yantriks.ypfp.common.api.controller.GenericController;
import com.yantriks.yso.capacity.cache.services.api.dto.request.FulfillmentTypeCapacityDetailRequest;
import com.yantriks.yso.capacity.cache.services.api.dto.response.FulfillmentTypeCapacityDetailResponse;
import com.yantriks.yso.capacity.cache.services.api.mapper.FulfillmentTypeCapacityDetailAndRequestResponseMapper;
import com.yantriks.yso.handler.FulfillmentTypeCapacityExtensionHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;


@Slf4j
@RestController
@Profile("!hazelcast-cluster")
@Tag(name = "1.2 - Cache Capacity Services", description = "REST API for fulfillment-type-capacity.")
@RequestMapping(value = FulfillmentTypeCapacityExtnController.ENDPOINT)
public class FulfillmentTypeCapacityExtnController extends GenericController<FulfillmentTypeCapacityDetailRequest, FulfillmentTypeCapacityDetailResponse> {
    public static final String ENDPOINT = "/capacity-cache-services/extn/fulfillment-type-capacity";

    @Autowired
    private FulfillmentTypeCapacityExtensionHandler handler;

    @Autowired
    private FulfillmentTypeCapacityDetailAndRequestResponseMapper mapper;

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Disable/Enable Location Fulfillment Type data as per Capacity Cache Data",description ="This API supports defining capacity by date for a given Location and Fulfillment Type." )
    public Mono<ResponseEntity<FulfillmentTypeCapacityDetailResponse>> create(
            @Parameter(required = true) @Valid @RequestBody FulfillmentTypeCapacityDetailRequest request) {

        return handler.manage(mapper.requestToDetail(request))
                .map(mapper::toResponse)
                .transform(processGenericCreateResponse(request));

    }




    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected String getMetricURI() {
        return "/capacity-cache-services/extn/fulfillment-type-capacity";
    }
}
