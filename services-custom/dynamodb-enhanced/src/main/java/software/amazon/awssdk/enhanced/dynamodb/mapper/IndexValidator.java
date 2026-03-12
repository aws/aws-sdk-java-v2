/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;

@SdkInternalApi
@ThreadSafe
final class IndexValidator {

    private static final int MIN_KEY_ORDER = -1;
    private static final int MAX_KEY_ORDER = 3;
    private static final int IMPLICIT_ORDER = -1;

    private static final String DUPLICATE_KEY_MSG = "Attempt to set an index key that conflicts with an existing "
                                                    + "index key of the same name and index. Index name: %s; "
                                                    + "attribute name: %s)";
    private static final String DUPLICATE_ATTRIBUTE_MSG = "Duplicate %s key '%s' for index '%s'";
    private static final String INVALID_ORDER_MSG = "Key order must be between %d and %d, got: %d";
    private static final String COMPOSITE_ORDERING_MSG = "Composite %s keys for index '%s' must all have explicit ordering (0,"
                                                         + "1,2,3)";
    private static final String NON_COMPOSITE_ORDERING_MSG = "Invalid non-composite %s key order for index '%s'. Expected: -1,0"
                                                             + " but got: %s";
    private static final String DUPLICATE_ORDER_MSG = "Duplicate %s key order %d for index '%s'";
    private static final String NON_SEQUENTIAL_MSG = "Non-sequential %s key orders for index '%s'. Expected: 0,1,2,3 but got: %s";

    private IndexValidator() {
    }

    static void validateKeyOrder(Order order) {
        if (order.getIndex() < MIN_KEY_ORDER || order.getIndex() > MAX_KEY_ORDER) {
            throw new IllegalArgumentException(String.format(INVALID_ORDER_MSG, MIN_KEY_ORDER, MAX_KEY_ORDER, order.getIndex()));
        }
    }

    static void validateNoDuplicateKeys(List<KeyAttributeMetadata> keys,
                                        String indexName, String attributeName) {
        if (keys.stream().anyMatch(k -> k.name().equals(attributeName))) {
            throw new IllegalArgumentException(String.format(DUPLICATE_KEY_MSG, indexName, attributeName));
        }
    }

    static void validateAllIndices(Collection<IndexMetadata> indices) {
        for (IndexMetadata index : indices) {
            // Skip validation for primary index - composite keys only supported for GSI
            if (TableMetadata.primaryIndexName().equals(index.name())) {
                continue;
            }
            validateCompositeKeyOrdering(index.partitionKeys(), "partition", index.name());
            validateCompositeKeyOrdering(index.sortKeys(), "sort", index.name());
        }
    }

    static void validateCompositeKeyOrdering(List<KeyAttributeMetadata> keys, String keyType, String indexName) {
        if (keys.size() <= 1) {
            if (keys.size() == 1) {
                int order = keys.get(0).order().getIndex();
                if (order != IMPLICIT_ORDER && order != 0) {
                    throw new IllegalArgumentException(String.format(NON_COMPOSITE_ORDERING_MSG, keyType, indexName, order));
                }
            }
            return;
        }

        Set<String> seenNames = new HashSet<>();
        Set<Integer> seenOrders = new HashSet<>();
        boolean[] orderPresent = new boolean[keys.size()];

        for (KeyAttributeMetadata key : keys) {
            String name = key.name();
            int order = key.order().getIndex();

            if (!seenNames.add(name)) {
                throw new IllegalArgumentException(String.format(DUPLICATE_ATTRIBUTE_MSG, keyType, name, indexName));
            }

            if (order == IMPLICIT_ORDER) {
                throw new IllegalArgumentException(String.format(COMPOSITE_ORDERING_MSG, keyType, indexName));
            }

            if (!seenOrders.add(order)) {
                throw new IllegalArgumentException(String.format(DUPLICATE_ORDER_MSG, keyType, order, indexName));
            }

            if (order >= 0 && order < keys.size()) {
                orderPresent[order] = true;
            }
        }

        for (int i = 0; i < keys.size(); i++) {
            if (!orderPresent[i]) {
                List<Integer> actualOrders = keys.stream().map(key -> key.order().getIndex())
                                                 .sorted()
                                                 .collect(Collectors.toList());
                throw new IllegalArgumentException(String.format(NON_SEQUENTIAL_MSG, keyType, indexName, actualOrders));
            }
        }
    }
}