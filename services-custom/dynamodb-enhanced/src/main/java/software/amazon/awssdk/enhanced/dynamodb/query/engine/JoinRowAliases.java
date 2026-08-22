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

package software.amazon.awssdk.enhanced.dynamodb.query.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;

/**
 * Builds the {@code itemsByAlias} map for {@link EnhancedQueryRow} in join
 * queries. Centralizes base/joined alias keys so engines stay readable.
 */
@SdkInternalApi
public final class JoinRowAliases {

    private JoinRowAliases() {
    }

    /**
     * Row for LEFT/FULL when the join key is null on the base side, or when no joined rows exist for the key: base attributes
     * plus an empty joined map.
     */
    public static Map<String, Map<String, Object>> leftOuterJoinRowWithEmptyJoined(Map<String, Object> base) {
        Map<String, Map<String, Object>> itemsByAlias = new HashMap<>(2);
        itemsByAlias.put(QueryEngineSupport.BASE_ALIAS, base);
        itemsByAlias.put(QueryEngineSupport.JOINED_ALIAS, Collections.emptyMap());
        return itemsByAlias;
    }

    /**
     * Row when both base and joined attribute maps are present (inner match, or outer join with at least one joined row).
     */
    public static Map<String, Map<String, Object>> innerJoinRow(Map<String, Object> base, Map<String, Object> joined) {
        Map<String, Map<String, Object>> itemsByAlias = new HashMap<>(2);
        itemsByAlias.put(QueryEngineSupport.BASE_ALIAS, base);
        itemsByAlias.put(QueryEngineSupport.JOINED_ALIAS, joined);
        return itemsByAlias;
    }

    /**
     * Row for RIGHT/FULL when scanning the joined table for keys with no base match: empty base map and joined attributes.
     */
    public static Map<String, Map<String, Object>> rightOuterJoinRowWithEmptyBase(Map<String, Object> joined) {
        Map<String, Map<String, Object>> itemsByAlias = new HashMap<>(2);
        itemsByAlias.put(QueryEngineSupport.BASE_ALIAS, Collections.emptyMap());
        itemsByAlias.put(QueryEngineSupport.JOINED_ALIAS, joined);
        return itemsByAlias;
    }
}
