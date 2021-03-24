package com.yantriks.yso.loader;

import com.yantriks.ypfp.common.api.dto.GenericExceptionResponse;
import com.yantriks.ypfp.ycs.location.services.core.dto.LocationAndFulfillmentTypeDetail;
import com.yantriks.ypfp.ycs.location.services.core.dto.key.LocationAndFulfillmentTypeDetailKey;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LocationFulfillmentTypeEntryLoader {

    @Autowired
    private WebClient webClient;


    @Value("${common.services.calculate-api.url}")
    private String locationFulfillmentTypeUrl;

    public Mono<LocationAndFulfillmentTypeDetail> getLoad(LocationAndFulfillmentTypeDetailKey key)  {

        return webClient
                .method(HttpMethod.GET)
                .uri(locationFulfillmentTypeUrl + getUriComponents(key))
                .retrieve().bodyToMono(LocationAndFulfillmentTypeDetail.class);



    }

  /*  public Mono<? extends LocationAndFulfillmentTypeDetail> createLoad(Mono<LocationAndFulfillmentTypeDetail> detail) {

        return webClient
                .method(HttpMethod.PUT)
                .uri(locationFulfillmentTypeUrl + "/location-services/location-fulfillment-type")
                .body(detail, LocationAndFulfillmentTypeDetail.class)
                .retrieve()
                .bodyToMono(LocationAndFulfillmentTypeDetail.class);
    }*/

    public Mono<? extends LocationAndFulfillmentTypeDetail> createLoad(Mono<LocationAndFulfillmentTypeDetail> detail,HttpMethod httpMethod) {

        return webClient
                .method(httpMethod)
                .uri(locationFulfillmentTypeUrl + "/location-services/location-fulfillment-type")
                .body(detail, LocationAndFulfillmentTypeDetail.class)
                .retrieve()
                .bodyToMono(LocationAndFulfillmentTypeDetail.class);
    }
    public Mono<LocationAndFulfillmentTypeDetail> doLoad(Mono<LocationAndFulfillmentTypeDetail> detail)  {
        log.debug("before update locationAndFulfillmentTypeDetail : {}", detail.subscribe(System.out::println));
        return webClient
                .method(HttpMethod.PUT)
                .uri(locationFulfillmentTypeUrl + "/location-services/location-fulfillment-type")
                .body(detail, LocationAndFulfillmentTypeDetail.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, error -> error.bodyToMono(GenericExceptionResponse.class))
                .bodyToMono(LocationAndFulfillmentTypeDetail.class)
                .onErrorResume(error -> {
                    if (error instanceof GenericExceptionResponse) {
                        HttpStatus httpStatus = HttpStatus.resolve(((GenericExceptionResponse) error).getStatus());
                        if (httpStatus.is4xxClientError() && error.getMessage().equals("Entity does not exist")) {
                            log.debug("Error Received ENTITY_DOES_NOT_EXIST hence attempting create reservation");
                            return createLoad(detail, HttpMethod.POST);
                        } else {
                            log.error("It is not 4xx with ENTITY_DOES_NOT_EXISTS hence returning error response");
                            return Mono.error(error);
                        }
                    }
                    else {
                        log.debug("Received error instead of Generic Exception Response");
                        return Mono.error(error);
                    }
                });
    }
    private UriComponents getUriComponents(LocationAndFulfillmentTypeDetailKey key) {
        return UriComponentsBuilder.fromUriString("/common-services/location")
                .path("/{orgId}")
                .path("/{sellingChannel}")
                .path("/{locationType}")
                .path("/{locationId}")
                .path("/{fulfillmentType}")
                .buildAndExpand(key.getOrgId(), key.getSellingChannel(), key.getLocationType(), key.getLocationId(),
                        key.getFulfillmentType());
    }
    protected Logger log() {
        return log;
    }

}
