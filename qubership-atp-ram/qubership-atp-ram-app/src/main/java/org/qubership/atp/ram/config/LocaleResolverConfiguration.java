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

package org.qubership.atp.ram.config;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class LocaleResolverConfiguration {

    @Value("${locale.resolver.lang:en}")
    private String language;

    /**
     * Set default locale US.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver headerLocaleResolver = new AcceptHeaderLocaleResolver();
        headerLocaleResolver.setDefaultLocale(new Locale(language));
        List<Locale> localeList = headerLocaleResolver.getSupportedLocales();
        localeList.add(Locale.US);
        return headerLocaleResolver;
    }
}
