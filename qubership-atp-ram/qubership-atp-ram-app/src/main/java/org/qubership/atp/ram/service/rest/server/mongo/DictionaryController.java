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
import org.qubership.atp.ram.models.dictionary.Dictionary;
import org.qubership.atp.ram.services.dictionary.DictionaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dictionaries")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService service;

    @GetMapping("/{name}")
    @AuditAction(auditAction = "Get all dictionaries with name {{#name}}")
    public List<Dictionary> getAllDictionaries(@PathVariable final String name) {
        return service.getAllByName(name);
    }

    @PostMapping("/cache/evict")
    public void evict(@RequestParam String name) {
        service.evictDictionaryCacheByName(name);
    }
}
