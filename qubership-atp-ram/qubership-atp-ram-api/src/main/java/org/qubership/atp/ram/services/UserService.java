/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.ram.services;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.ram.models.UserInfo;
import org.qubership.atp.ram.utils.StreamUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService {
    private String issuer;

    @Value("${keycloak.auth-server-url}")
    private String baseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Qualifier("m2mRestTemplate")
    private final RestTemplate m2mRestTemplate;

    public UserService(@Qualifier("restTemplate") RestTemplate m2mRestTemplate) {
        this.m2mRestTemplate = m2mRestTemplate;
    }

    @PostConstruct
    public void init() {
        this.issuer = this.baseUrl + "/admin/realms/" + this.realm;
    }

    /**
     * Find {@link UserInfo} by specified id.
     *
     * @param userId user identifier
     * @return user info
     */
    public UserInfo getUserInfoById(UUID userId) {
        String url = issuer + "/users/" + userId;
        UserInfo userInfo;
        try {
            userInfo = m2mRestTemplate.getForObject(url, UserInfo.class);
        } catch (RestClientException e) {
            return new UserInfo("-");
        }
        if (Objects.isNull(userInfo)) {
            log.error("Failed to find User Info by id: {}", userId);
            throw new AtpEntityNotFoundException("User Info", userId);
        }

        return userInfo;
    }

    /**
     * Find {@link UserInfo} by specified id.
     *
     * @param userIds user identifiers
     * @return user info
     */
    public List<UserInfo> getUserByIds(Set<UUID> userIds) {
        // Keycloak API doesn't provide endpoints to filter users by ids directly
        String url = issuer + "/users";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("max", Integer.MAX_VALUE);

        UserInfo[] infos = m2mRestTemplate.getForObject(builder.toUriString(), UserInfo[].class);
        if (nonNull(infos)) {
            return StreamUtils.filterList(asList(infos), UserInfo::getId, userIds);
        }

        return Collections.emptyList();
    }

    /**
     * Get user id from auth token.
     *
     * @param token current user auth token
     * @return user id
     */
    public UUID getUserIdFromToken(String token) {
        UUID userId = null;
        if (StringUtils.isNotBlank(token)) {
            try {
                String[] splitToken = token.split(" ");
                if (splitToken.length < 2) {
                    log.warn("Cannot parse token: invalid format");
                    return null;
                }
                JWT jwt = JWTParser.parse(splitToken[1]);
                Map<String, Object> tokenData = switch (jwt) {
                    case PlainJWT plainJWT -> plainJWT.getPayload().toJSONObject();
                    case SignedJWT signedJWT -> signedJWT.getPayload().toJSONObject();
                    case EncryptedJWT encryptedJWT -> encryptedJWT.getPayload().toJSONObject();
                    default -> null; // in fact, we never visit it, due to earlier parse exception
                };
                if (tokenData != null && tokenData.containsKey("sub")) {
                    userId = UUID.fromString(tokenData.get("sub").toString());
                }
            } catch (Exception e) {
                log.warn("Cannot parse token with error: ", e);
            }
        }
        return userId;
    }

    /**
     * Get {@link UserInfo} from auth token.
     *
     * @param token current user auth token
     * @return user info
     */
    public UserInfo getUserInfoFromToken(String token) {
        UUID userId = getUserIdFromToken(token);
        return getUserInfoById(userId);
    }
}
