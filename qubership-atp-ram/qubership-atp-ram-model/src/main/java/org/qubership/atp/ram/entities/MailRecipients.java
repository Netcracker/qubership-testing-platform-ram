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

package org.qubership.atp.ram.entities;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;
import lombok.Data;

@Data
public class MailRecipients {

    private static final int AVERAGE_EMAIL_SIZE = 35;
    private Set<String> recipients = Sets.newLinkedHashSet();

    /**
     * Convert {@link java.util.List} of recipients to string and recipients delimited with comma separator.
     * For example:
     * List[
     * "sz@nc.com",
     * "saza@nc.com"
     * ]
     * will be converted to: "sz@nc.com,saza@nc.com"
     *
     * @return string delimited by comma separator.
     */
    public String recipientsAsString() {
        StringBuilder result = new StringBuilder(recipients.size() * AVERAGE_EMAIL_SIZE);
        Iterator<String> iterator = recipients.iterator();
        while (iterator.hasNext()) {
            result.append(iterator.next());
            if (iterator.hasNext()) {
                result.append(',');
            }
        }
        return result.toString();
    }
}
