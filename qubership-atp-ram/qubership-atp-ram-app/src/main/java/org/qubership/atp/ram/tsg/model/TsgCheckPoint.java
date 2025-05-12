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

package org.qubership.atp.ram.tsg.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TsgCheckPoint {

    @JsonProperty("Name")
    private String name;
    @JsonProperty("Status")
    private String status;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("Check Points")
    private List<TsgCheckPoint> checkPoints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TsgCheckPoint> getCheckPoints() {
        return checkPoints;
    }

    public void setCheckPoints(List<TsgCheckPoint> checkPoints) {
        this.checkPoints = checkPoints;
    }

    /**
     * Add new TsgCheckpoint to existing list.
     */
    public void addCheckPoint(TsgCheckPoint checkPoint) {
        if (this.checkPoints == null) {
            checkPoints = new ArrayList<>();
        }
        checkPoints.add(checkPoint);
    }
}

