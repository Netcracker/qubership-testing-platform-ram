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

import java.util.Date;

import javax.persistence.MappedSuperclass;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public abstract class DateAuditorEntity extends RamObject {

    @CreatedBy
    protected UserInfo createdBy;

    @CreatedDate
    protected Date createdWhen;

    @LastModifiedBy
    @DiffInclude
    protected UserInfo modifiedBy;

    @LastModifiedDate
    @DiffInclude
    protected Date modifiedWhen;

}
