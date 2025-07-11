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

package org.qubership.atp.ram.service.rest.server;

import org.qubership.atp.ram.service.direct.AtpLinkProviderService;
import org.qubership.atp.ram.service.rest.dto.EditorLinkDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/atp")
@RestController()
@RequiredArgsConstructor
public class AtpLinkProviderController /*implements AtpLinkProviderControllerApi*/ {

    private final AtpLinkProviderService service;

    @GetMapping(value = "/editor/link")
    public EditorLinkDto getLinkToEditor() {
        return new EditorLinkDto(service.getEditorLink());
    }
}
