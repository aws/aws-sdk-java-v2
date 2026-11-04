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

package software.amazon.awssdk.enhanced.dynamodb.query.result;

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A single row from an enhanced query result.
 * <p>
 * A row can represent:
 * <ul>
 *     <li>A join result: items available by alias (e.g. {@code "base"}, {@code "joined"}).</li>
 *     <li>An aggregation result: aggregate values available by output name.</li>
 * </ul>
 */
@SdkInternalApi
public class EnhancedQueryRow {

    private final Map<String, Map<String, Object>> itemsByAlias;
    private final Map<String, Object> aggregates;

    protected EnhancedQueryRow(Map<String, Map<String, Object>> itemsByAlias,
                               Map<String, Object> aggregates) {
        this.itemsByAlias = itemsByAlias == null
                            ? Collections.emptyMap() : Collections.unmodifiableMap(itemsByAlias);
        this.aggregates = aggregates == null
                          ? Collections.emptyMap() : Collections.unmodifiableMap(aggregates);
    }

    /**
     * Returns the item attribute map for the given alias (e.g. "base" for base table,
     * "joined" for joined table).
     */
    public Map<String, Object> getItem(String alias) {
        return itemsByAlias.getOrDefault(alias, Collections.emptyMap());
    }

    /**
     * Returns all items by alias. Keys are table aliases; values are attribute name to value maps.
     */
    public Map<String, Map<String, Object>> itemsByAlias() {
        return itemsByAlias;
    }

    /**
     * Returns the aggregate value for the given output name.
     */
    public Object getAggregate(String outputName) {
        return aggregates.get(outputName);
    }

    /**
     * Returns all aggregate values by output name.
     */
    public Map<String, Object> aggregates() {
        return aggregates;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EnhancedQueryRow}.
     */
    public static class Builder {

        private Map<String, Map<String, Object>> itemsByAlias;
        private Map<String, Object> aggregates;

        protected Builder() {
        }

        public Builder itemsByAlias(Map<String, Map<String, Object>> itemsByAlias) {
            this.itemsByAlias = itemsByAlias;
            return this;
        }

        public Builder aggregates(Map<String, Object> aggregates) {
            this.aggregates = aggregates;
            return this;
        }

        public EnhancedQueryRow build() {
            return new EnhancedQueryRow(itemsByAlias, aggregates);
        }
    }
}
