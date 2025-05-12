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

package org.qubership.atp.ram.models;

import static java.util.Comparator.comparing;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.javers.core.metamodel.annotation.Value;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Value
public class JiraTicket implements Comparable<JiraTicket> {
    private String url;
    private Timestamp createdDate;
    private Boolean resolved;

    /**
     * JiraTicket constructor.
     */
    public JiraTicket(String url) {
        this.url = url;
        this.createdDate = Timestamp.valueOf(LocalDateTime.now());
        this.resolved = false;
    }

    @Override
    public int compareTo(JiraTicket that) {
        int dateCompare = comparing(JiraTicket::getResolved).compare(this, that);
        if (dateCompare != 0) {
            return dateCompare;
        } else {
            return comparing(JiraTicket::getCreatedDate).reversed().compare(this, that);
        }
    }

    /**
     * Get jira ticket key.
     */
    @SneakyThrows
    public String getKey() {
        String path = new URI(url).getPath();

        return path.substring(path.lastIndexOf('/') + 1);
    }
}
