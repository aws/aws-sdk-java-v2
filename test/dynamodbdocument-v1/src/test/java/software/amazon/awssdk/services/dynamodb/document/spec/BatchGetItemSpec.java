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
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.document.TableKeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

/**
 * Full parameter specification for the BatchGetItem API.
 */
public class BatchGetItemSpec extends AbstractSpec<BatchGetItemRequest> {
    private Collection<TableKeysAndAttributes> tableKeyAndAttributes;
    private Map<String, KeysAndAttributes> unprocessedKeys;

    public BatchGetItemSpec() {
        super(BatchGetItemRequest.builder().build());
    }

    public Collection<TableKeysAndAttributes> getTableKeysAndAttributes() {
        return tableKeyAndAttributes;
    }

    public BatchGetItemSpec withTableKeyAndAttributes(
            TableKeysAndAttributes... tableKeyAndAttributes) {
        if (tableKeyAndAttributes == null) {
            this.tableKeyAndAttributes = null;
        } else {
            Set<String> names = new LinkedHashSet<String>();
            for (TableKeysAndAttributes e : tableKeyAndAttributes) {
                names.add(e.getTableName());
            }
            if (names.size() != tableKeyAndAttributes.length) {
                throw new IllegalArgumentException(
                        "table names must not duplicate in the list of TableKeysAndAttributes");
            }
            this.tableKeyAndAttributes = Arrays.asList(tableKeyAndAttributes);
        }
        return this;
    }


    public String getReturnConsumedCapacity() {
        return getRequest().returnConsumedCapacityString();
    }


    public BatchGetItemSpec withReturnConsumedCapacity(ReturnConsumedCapacity capacity) {
        setRequest(getRequest().toBuilder().returnConsumedCapacity(capacity).build());
        return this;
    }

    public Map<String, KeysAndAttributes> getUnprocessedKeys() {
        return unprocessedKeys;
    }

    public BatchGetItemSpec withUnprocessedKeys(
            Map<String, KeysAndAttributes> unprocessedKeys) {
        this.unprocessedKeys = Collections.unmodifiableMap(
                new LinkedHashMap<String, KeysAndAttributes>(unprocessedKeys));
        return this;
    }
}
