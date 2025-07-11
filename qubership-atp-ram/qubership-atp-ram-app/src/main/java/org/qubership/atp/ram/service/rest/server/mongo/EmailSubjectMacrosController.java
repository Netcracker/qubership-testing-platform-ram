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

package org.qubership.atp.ram.service.rest.server.mongo;

import java.util.List;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.ram.service.emailsubjectmacros.EmailSubjectMacrosService;
import org.qubership.atp.ram.service.rest.dto.EmailSubjectMacrosResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/emailsubjectmacros")
@RequiredArgsConstructor
public class EmailSubjectMacrosController /*implements EmailSubjectMacrosControllerApi*/ {

    private final EmailSubjectMacrosService service;

    @GetMapping
    @AuditAction(auditAction = "Get all email subject macros")
    public List<EmailSubjectMacrosResponse> getAll() {
        return service.getAll();
    }
}
