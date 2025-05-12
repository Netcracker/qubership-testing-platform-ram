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

package org.qubership.atp.ram.service.history.impl;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.javers.core.Changes;
import org.javers.core.ChangesByCommit;
import org.javers.core.Javers;
import org.javers.core.commit.CommitId;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.qubership.atp.ram.controllers.api.dto.history.HistoryItemDto;
import org.qubership.atp.ram.controllers.api.dto.history.HistoryItemResponseDto;
import org.qubership.atp.ram.controllers.api.dto.history.PageInfoDto;
import org.qubership.atp.ram.models.ObjectOperation;
import org.qubership.atp.ram.models.Operation;
import org.qubership.atp.ram.service.history.RetrieveHistoryService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRetrieveHistoryService implements RetrieveHistoryService {

    private final Javers javers;

    public AbstractRetrieveHistoryService(Javers javers) {
        this.javers = javers;
    }

    /**
     * Finds all history of changes for provided entity id.
     *
     * @param id     object id of requested entity
     * @param offset index of the first element in collection
     * @param limit  number of items in collection
     * @return list of changes
     */
    public HistoryItemResponseDto getAllHistory(UUID id, Integer offset, Integer limit) {

        log.debug(String.format("Get All History for entity = %s, offset = %s, limit = %s", id, offset, limit));

        JqlQuery query = getChangesByIdPaginationQuery(id, offset, limit);

        Changes changes = javers.findChanges(query);
        log.debug(String.format("Changes found for entity = %s,  changes = %s", id, changes.prettyPrint()));

        List<CdoSnapshot> snapshots = javers.findSnapshots(query);
        log.debug(String.format("Snapshots found for entity = %s, snapshots = %s", id, snapshots));

        List<ChangesByCommit> changesByCommits = changes.groupByCommit();

        List<HistoryItemDto> historyItemDtoList = changesByCommits
                .stream()
                .map(changesByCommit -> createHistoryItem(changesByCommit, snapshots))
                .collect(Collectors.toList());

        HistoryItemResponseDto response = new HistoryItemResponseDto();
        response.setHistoryItems(historyItemDtoList);
        response.setPageInfo(getPageInfo(id, offset, limit));

        return response;
    }

    private PageInfoDto getPageInfo(UUID id, Integer offset, Integer limit) {
        PageInfoDto pageInfo = new PageInfoDto();
        pageInfo.setOffset(offset);
        pageInfo.setLimit(limit);
        pageInfo.setItemsTotalCount(this.getCountOfCommits(id));
        return pageInfo;
    }

    public Integer getCountOfCommits(UUID id) {
        Changes changes = javers.findChanges(getChangesByIdQuery(id));
        return changes.groupByCommit().size();
    }

    private HistoryItemDto createHistoryItem(ChangesByCommit changesByCommit, List<CdoSnapshot> snapshots) {

        HistoryItemDto historyItemDto = new HistoryItemDto();
        CommitMetadata commit = changesByCommit.getCommit();
        historyItemDto.setVersion(getVersionByCommitId(snapshots, commit.getId()));
        historyItemDto.setType(getItemType());
        historyItemDto.setModifiedWhen(
                new Date(commit.getCommitDate().atOffset(ZoneOffset.UTC).toInstant().toEpochMilli()).toString());
        historyItemDto.setModifiedBy(defaultIfEmpty(commit.getAuthor(), EMPTY));

        Map<Boolean, List<Change>> partitions = changesByCommit.get()
                .stream()
                .filter(change -> change instanceof PropertyChange)
                .collect(Collectors.partitioningBy(change ->
                        CHILD_ACTIONS_PROPERTY.equals(((PropertyChange) change).getPropertyName())));

        historyItemDto.setChanged(calculateCommonChanges(partitions.get(false)));

        Optional<Change> childChanges = partitions.get(true).stream().findFirst();
        childChanges.ifPresent(change -> processChildChanges(change, historyItemDto));

        return historyItemDto;
    }

    protected Integer getVersionByCommitId(List<CdoSnapshot> snapshots, CommitId id) {
        Integer version = null;
        Optional<CdoSnapshot> snapshot = snapshots
                .stream()
                .filter(cdoSnapshot -> cdoSnapshot.getCommitId().equals(id))
                .findFirst();

        if (snapshot.isPresent()) {
            version = Long.valueOf(snapshot
                    .get()
                    .getVersion())
                    .intValue();
        }

        return version;
    }

    private List<String> calculateCommonChanges(List<Change> changes) {
        return changes
                .stream()
                .map(change -> ((PropertyChange) change).getPropertyName())
                .collect(Collectors.toList());
    }

    protected void processChildChanges(Change propertyChange, HistoryItemDto historyItemDto) {
        ContainerChange containerChange = (ContainerChange) propertyChange;
        List<ValueAdded> valueAddedChanges = containerChange.getValueAddedChanges();
        List<ObjectOperation> objectOperations = valueAddedChanges.stream()
                .map(ValueAdded::getAddedValue)
                .filter(o -> o instanceof ObjectOperation)
                .map(o -> (ObjectOperation) o)
                .collect(Collectors.toList());
        historyItemDto.setAdded(getOperationNames(objectOperations, Operation.ADD));
        historyItemDto.setDeleted(getOperationNames(objectOperations, Operation.REMOVE));
    }

    protected JqlQuery getChangesByIdPaginationQuery(UUID id, Integer offset, Integer limit) {
        return QueryBuilder.byInstanceId(id, getEntityClass())
                .withNewObjectChanges()
                .skip(offset)
                .limit(limit)
                .build();
    }

    protected JqlQuery getChangesByIdQuery(UUID id) {
        return QueryBuilder.byInstanceId(id, getEntityClass())
                .limit(Integer.MAX_VALUE)
                .withNewObjectChanges()
                .build();
    }

    private List<String> getOperationNames(List<ObjectOperation> objectOperations, Operation operation) {
        return objectOperations.stream()
                .filter(obj -> operation.equals(obj.getOperationType()))
                .map(ObjectOperation::getName)
                .collect(Collectors.toList());
    }
}
