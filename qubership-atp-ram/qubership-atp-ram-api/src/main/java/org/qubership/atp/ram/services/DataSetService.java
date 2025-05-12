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

package org.qubership.atp.ram.services;

import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.ram.client.DataSetFeignClient;
import org.qubership.atp.ram.converter.DtoConvertService;
import org.qubership.atp.ram.model.DataSet;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataSetService {

    private DataSetFeignClient dataSetFeignClient;

    public DataSetService(DataSetFeignClient dataSetFeignClient) {
        this.dataSetFeignClient = dataSetFeignClient;
    }


    /**
     * Finds DataSet info in atp-datasets service by DataSets id-s.
     *
     * @param ids of DataSet entities in atp-datasets service
     * @return list of DataSet info
     */
    public List<DataSet> getDataSetsByIds(List<UUID> ids) {
        List<DataSet> dataSet = new DtoConvertService(new ModelMapper())
                .convertList(dataSetFeignClient.getDataSetsByIds(ids).getBody(), DataSet.class);
        log.debug("Get dataset: {} by id {}", dataSet, ids);
        return dataSet;
    }
}
