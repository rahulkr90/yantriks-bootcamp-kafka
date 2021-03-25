package com.yantriks.yso.handler;

import com.yantriks.ypfp.common.api.dto.GenericExceptionResponse;
import com.yantriks.ypfp.common.api.exception.EntityDoesNotExistException;
import com.yantriks.ypfp.ycs.location.services.core.dto.LocationAndFulfillmentTypeDetail;
import com.yantriks.ypfp.ycs.location.services.core.dto.key.LocationAndFulfillmentTypeDetailKey;
import com.yantriks.yso.capacity.cache.services.api.controller.FulfillmentTypeCapacityController;
import com.yantriks.yso.capacity.cache.services.api.dto.request.FulfillmentTypeCapacityDetailRequest;
import com.yantriks.yso.capacity.cache.services.api.dto.response.FulfillmentTypeCapacityDetailResponse;
import com.yantriks.yso.capacity.cache.services.api.handler.FulfillmentTypeCapacityHandler;
import com.yantriks.yso.capacity.cache.services.api.mapper.FulfillmentTypeCapacityDetailAndRequestResponseMapper;
import com.yantriks.yso.capacity.cache.services.core.dto.CapacityDetail;
import com.yantriks.yso.capacity.cache.services.core.dto.FulfillmentTypeCapacityDetail;
import com.yantriks.yso.loader.CommonLocationFulfilmentTypeEntityLoader;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;

@Service
public class FulfillmentTypeCapacityCacheExtnHandler {

    @Autowired
    private WebClient webClient;
    @Generated
    private static final Logger log = LoggerFactory.getLogger(FulfillmentTypeCapacityCacheExtnHandler.class);
    @Autowired
    CommonLocationFulfilmentTypeEntityLoader loader;

    @Autowired
    FulfillmentTypeCapacityController controller;
    @Autowired
    private FulfillmentTypeCapacityHandler handler;


    @Value("${api.url}")
    private String apiHost;

    @Value("${api.endpoint}")
    private String apiendpoint;

    @Autowired
    private FulfillmentTypeCapacityDetailAndRequestResponseMapper mapper;

    public FulfillmentTypeCapacityCacheExtnHandler() {
    }

    public Mono<FulfillmentTypeCapacityDetail> manage(FulfillmentTypeCapacityDetail fulfillmentTypeCapacityDetail) {
         return (handler.update(fulfillmentTypeCapacityDetail)
                .onErrorResume(error -> {
                    if (!(error instanceof EntityDoesNotExistException)) {
                        log.debug("Error : {}",error.getMessage());
                    }
                    return handler.create(fulfillmentTypeCapacityDetail);
                })).doOnSuccess(s->{
            updateLocationFT(fulfillmentTypeCapacityDetail);
        });
    }

    private void updateLocationFT(FulfillmentTypeCapacityDetail fulfillmentTypeCapacityDetail) {
        LocationAndFulfillmentTypeDetailKey locationFTKey =
                LocationAndFulfillmentTypeDetailKey.builder().orgId(fulfillmentTypeCapacityDetail.getOrgId()).sellingChannel(fulfillmentTypeCapacityDetail.getSellingChannel())
                        .locationType(fulfillmentTypeCapacityDetail.getLocationType()).locationId(fulfillmentTypeCapacityDetail.getLocationId())
                        .fulfillmentType(fulfillmentTypeCapacityDetail.getFulfillmentType()).build();
        Mono<LocationAndFulfillmentTypeDetail> locationFTDetail = loader.getLoad(locationFTKey);

        locationFTDetail.subscribe(detail -> {
            loader.doLoad(buildLocationFTDetailRequest(fulfillmentTypeCapacityDetail.getCapacity(), detail)).subscribe();
        });
    }

    private Mono<LocationAndFulfillmentTypeDetail> buildLocationFTDetailRequest(Map<LocalDate, CapacityDetail> capacity,
                                                                                LocationAndFulfillmentTypeDetail detail) {
        if (capacity.containsKey(LocalDate.now())) {
            if (capacity.get(LocalDate.now()).getCapacity() > 0) {
                detail.setEnabled(true);
            } else if (capacity.get(LocalDate.now()).getCapacity() == 0) {
                detail.setEnabled(false);
            }
        } else {
            detail.setEnabled(false);
        }

        return Mono.just(detail);
    }

    public Mono<FulfillmentTypeCapacityDetailResponse> manage(FulfillmentTypeCapacityDetailRequest request) {

        return webClient
                .method(HttpMethod.PUT)
                .uri(apiHost.concat(apiendpoint))
                . contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
               // .body(request, FulfillmentTypeCapacityDetailRequest.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, error -> error.bodyToMono(GenericExceptionResponse.class))
                .bodyToMono(FulfillmentTypeCapacityDetailResponse.class)
                .onErrorResume(error -> {
                    if (error instanceof GenericExceptionResponse) {
                        HttpStatus httpStatus = HttpStatus.resolve(((GenericExceptionResponse) error).getStatus());
                        if (httpStatus.is4xxClientError() && error.getMessage().equals("Entity does not exist")) {
                            log.debug("Error Received ENTITY_DOES_NOT_EXIST hence attempting create reservation");
                            return webClient
                                    .method(HttpMethod.POST)
                                    .uri(apiHost.concat(apiendpoint))
                                    . contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromValue(request))
                                    .retrieve()
                                    .bodyToMono(FulfillmentTypeCapacityDetailResponse.class);
                                   /* .doOnSuccess(s->{
                                            updateLocationFT(request);
                                            //return Mono.empty();
                                    });*/
                        } else {
                            log.error("It is not 4xx with ENTITY_DOES_NOT_EXISTS hence returning error response");
                            return Mono.error(error);
                        }
                    }
                    else {
                        log.debug("Received error instead of Generic Exception Response");
                        return Mono.error(error);
                    }
                })
                .doOnSuccess(s->{
                    updateLocationFT(request);
                    //return Mono.empty();
                });
    }

    private void updateLocationFT(FulfillmentTypeCapacityDetailRequest request) {
      FulfillmentTypeCapacityDetail  fulfillmentTypeCapacityDetail =  mapper.requestToDetail(request);

        LocationAndFulfillmentTypeDetailKey locationFTKey =
                LocationAndFulfillmentTypeDetailKey.builder().orgId(fulfillmentTypeCapacityDetail.getOrgId()).sellingChannel(fulfillmentTypeCapacityDetail.getSellingChannel())
                        .locationType(fulfillmentTypeCapacityDetail.getLocationType()).locationId(fulfillmentTypeCapacityDetail.getLocationId())
                        .fulfillmentType(fulfillmentTypeCapacityDetail.getFulfillmentType()).build();

        Mono<LocationAndFulfillmentTypeDetail> locationFTDetail = loader.getLoad(locationFTKey);

        locationFTDetail.subscribe(detail -> {
            loader.doLoad(buildLocationFTDetailRequest(fulfillmentTypeCapacityDetail.getCapacity(), detail)).subscribe();
        });
    }
}