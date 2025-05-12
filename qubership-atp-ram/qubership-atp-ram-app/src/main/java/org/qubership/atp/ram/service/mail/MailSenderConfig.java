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

package org.qubership.atp.ram.service.mail;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
public class MailSenderConfig {
    private String mailSenderUrl;

    /**
     * Returns mailSender url.
     */
    public String getMailSenderUrl() {
        if (Strings.isNullOrEmpty(mailSenderUrl)) {
            mailSenderUrl = System.getProperty("atp.mailsender.url", "http://mailsender-service-address");
        }
        return mailSenderUrl;
    }

    /**
     * Set MailSender url.
     */
    public void setMailSenderUrl(String mailSenderUrl) {
        this.mailSenderUrl = mailSenderUrl;
    }
}
