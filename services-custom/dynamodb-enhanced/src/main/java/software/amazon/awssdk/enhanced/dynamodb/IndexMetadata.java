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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * A metadata class that stores information about an index
 */
@SdkPublicApi
@ThreadSafe
public interface IndexMetadata {
    /**
     * The name of the index
     */
    String name();

    /**
     * The partition keys for the index in order.
     * @default For backward compatibility the default implementation returns singleton list of partitionKey if present or empty
     * list.
     * External implementations of the interface must explicitly override this method to return the partitionKeys collection and
     * to enable the composite key support to return multiple elements in the correct order.
     */
    default List<KeyAttributeMetadata> partitionKeys() {
        return partitionKey().map(Collections::singletonList).orElse(Collections.emptyList());
    }

    /**
     * The sort keys for the index in order.
     * @default For backward compatibility the default implementation returns singleton list of sortKey if present or empty list.
     * External implementations of the interface must explicitly override this method to return the sortKeys collection and
     * to enable the composite key support to return multiple elements in the correct order.
     */
    default List<KeyAttributeMetadata> sortKeys() {
        return sortKey().map(Collections::singletonList).orElse(Collections.emptyList());
    }

    /**
     * The partition key for the index; if there is one.
     * @deprecated Use {@link #partitionKeys()} for unified single/composite key support
     */
    @Deprecated
    default Optional<KeyAttributeMetadata> partitionKey() {
        List<KeyAttributeMetadata> keys = partitionKeys();
        return keys.isEmpty() ? Optional.empty() : Optional.of(keys.get(0));
    }

    /**
     * The sort key for the index; if there is one.
     * @deprecated Use {@link #sortKeys()} for unified single/composite key support
     */
    @Deprecated
    default Optional<KeyAttributeMetadata> sortKey() {
        List<KeyAttributeMetadata> keys = sortKeys();
        return keys.isEmpty() ? Optional.empty() : Optional.of(keys.get(0));
    }
}
