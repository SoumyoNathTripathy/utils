package com.uci.utils.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.uci.utils.cache.service.RedisCacheService;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.LoginRequest;
import io.fusionauth.domain.api.LoginResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class VaultService {
    public Cache<Object, Object> cache;

    /**
     * Get Fusion Auth Login Token for vault service
     * @return
     */
    /** NOT IN USE - using admin token directly **/
//    public String getLoginToken() {
//        String cacheKey = "vault-login-token";
//        if(cache.getIfPresent(cacheKey) != null) {
//            log.info("vault user token found");
//            return cache.getIfPresent(cacheKey).toString();
//        }
//        log.info("fetch vault user token");
//        FusionAuthClient fusionAuthClient = new FusionAuthClient(System.getenv("VAULT_FUSION_AUTH_TOKEN"), System.getenv("VAULT_FUSION_AUTH_URL"));
//        LoginRequest loginRequest = new LoginRequest();
//        loginRequest.loginId = "uci-user";
//        loginRequest.password = "abcd1234";
//        loginRequest.applicationId = UUID.fromString("a1313380-069d-4f4f-8dcb-0d0e717f6a6b");
//        ClientResponse<LoginResponse, Errors> loginResponse = fusionAuthClient.login(loginRequest);
//        if(loginResponse.wasSuccessful()) {
//            cache.put(cacheKey, loginResponse.successResponse.token);
//            return loginResponse.successResponse.token;
//        } else {
//            return null;
//        }
//    }

    /**
     * Retrieve Adapter Credentials From its Identifier
     *
     * @param secretKey - Adapter Identifier
     * @return Application
     */
    public Mono<JsonNode> getAdpaterCredentials(String secretKey) {
        String adminToken = System.getenv("VAULT_SERVICE_TOKEN");
        if(adminToken == null || adminToken.isEmpty()) {
            return Mono.just(null);
        }
        WebClient webClient = WebClient.builder().baseUrl(System.getenv("VAULT_SERVICE_URL")).build();
        String cacheKey = "adapter-credentials-by-id: " + secretKey;
        return CacheMono.lookup(key -> Mono.justOrEmpty(cache.getIfPresent(cacheKey) != null ? (JsonNode) cache.getIfPresent(key) : null)
                        .map(Signal::next), cacheKey)
                        .onCacheMissResume(() -> webClient.get()
                        .uri(builder -> builder.path("admin/secret/" + secretKey).build())
                        .headers(httpHeaders ->{
                            httpHeaders.set("ownerId", "8f7ee860-0163-4229-9d2a-01cef53145ba");
                            httpHeaders.set("ownerOrgId", "org1");
                            httpHeaders.set("admin-token", adminToken);
                        })
                        .retrieve().bodyToMono(String.class).map(response -> {
                            if (response != null) {
                                ObjectMapper mapper = new ObjectMapper();
                                try {
                                    Map<String, String> credentials = new HashMap<String, String>();
                                    JsonNode root = mapper.readTree(response);
                                    if(root.path("result") != null && root.path("result").path(secretKey) != null) {
                                        return root.path("result").path(secretKey);
                                    }
                                    return null;
                                } catch (JsonProcessingException e) {
                                    return null;
                                }
                            }
                            return null;
                        }).doOnError(throwable -> log.info("Error in getting bot: " + throwable.getMessage())).onErrorReturn(null))
                .andWriteWith((key, signal) -> Mono.fromRunnable(
                        () -> Optional.ofNullable(signal.get()).ifPresent(value -> cache.put(key, value))))
                .log("cache");

    }
}
