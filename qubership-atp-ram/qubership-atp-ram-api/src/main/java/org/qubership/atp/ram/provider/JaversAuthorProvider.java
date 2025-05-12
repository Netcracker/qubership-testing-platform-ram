/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
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

package org.qubership.atp.ram.provider;

import org.javers.spring.auditable.AuthorProvider;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JaversAuthorProvider implements AuthorProvider {

    private final Provider<UserInfo> userInfoProvider;

    @Override
    public String provide() {
        String name = "unauthenticated";
        try {
            if (userInfoProvider != null && userInfoProvider.get() != null) {
                name = userInfoProvider.get().getUsername();
                name = name == null ? "" : name;
            }
        } catch (Exception e) {
            log.debug("Error occurred while getting user info", e);
        }
        return name;
    }
}
