/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb.document.spec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.document.TableWriteItems;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Full parameter specification for the BatchWriteItem API.
 */
public class BatchWriteItemSpec extends AbstractSpec<BatchWriteItemRequest> {
    private Collection<TableWriteItems> tableWriteItems;
    private Map<String, List<WriteRequest>> unprocessedItems;

    public BatchWriteItemSpec() {
        super(BatchWriteItemRequest.builder().build());
    }

    public Collection<TableWriteItems> getTableWriteItems() {
        return tableWriteItems;
    }

    public BatchWriteItemSpec withTableWriteItems(
            TableWriteItems... tableWriteItems) {
        if (tableWriteItems == null) {
            this.tableWriteItems = null;
        } else {
            Set<String> names = new LinkedHashSet<String>();
            for (TableWriteItems e : tableWriteItems) {
                names.add(e.getTableName());
            }
            if (names.size() != tableWriteItems.length) {
                throw new IllegalArgumentException(
                        "table names must not duplicate in the list of TableWriteItems");
            }
            this.tableWriteItems = Arrays.asList(tableWriteItems);
        }
        return this;
    }


    public String getReturnConsumedCapacity() {
        return getRequest().returnConsumedCapacityAsString();
    }


    public BatchWriteItemSpec withReturnConsumedCapacity(ReturnConsumedCapacity capacity) {
        setRequest(getRequest().toBuilder().returnConsumedCapacity(capacity).build());
        return this;
    }

    public Map<String, List<WriteRequest>> getUnprocessedItems() {
        return unprocessedItems;
    }

    public BatchWriteItemSpec withUnprocessedItems(
            Map<String, List<WriteRequest>> unprocessedItems) {
        this.unprocessedItems = Collections.unmodifiableMap(
                new LinkedHashMap<String, List<WriteRequest>>(unprocessedItems));
        return this;
    }
}
