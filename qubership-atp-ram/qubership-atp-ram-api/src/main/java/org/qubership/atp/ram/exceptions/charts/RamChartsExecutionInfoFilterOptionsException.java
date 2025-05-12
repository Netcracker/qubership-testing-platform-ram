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

package org.qubership.atp.ram.exceptions.charts;

import static java.lang.String.format;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "RAM-8000")
public class RamChartsExecutionInfoFilterOptionsException extends AtpException {

    public static final String DEFAULT_MESSAGE = "Invalid number of execution requests: %s. Value Should be greater "
            + "than 1 and less than 21";

    public RamChartsExecutionInfoFilterOptionsException(Integer numberOfErs) {
        super(format(DEFAULT_MESSAGE, numberOfErs));
    }
}
