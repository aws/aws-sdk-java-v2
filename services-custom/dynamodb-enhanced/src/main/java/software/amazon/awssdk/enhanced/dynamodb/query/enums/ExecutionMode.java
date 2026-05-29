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

package software.amazon.awssdk.enhanced.dynamodb.query.enums;

/**
 * Execution strategy for complex queries. Controls whether the engine may use a full table or index Scan.
 * <p>
 * <b>STRICT_KEY_ONLY</b> (default): Only key-based operations are allowed—{@code Query} (with partition key
 * and optional sort condition) and {@code BatchGetItem}. If the request cannot be satisfied with keys (e.g. no partition key
 * supplied, or join attribute is not a key on the joined table), the engine must fail or return empty; it must not perform a
 * Scan.
 * <p>
 * <b>ALLOW_SCAN</b>: Same as above, but the engine may fall back to a full table or index Scan when
 * there is no usable key or index. Use when you explicitly accept Scan's cost and latency for that query.
 */
public enum ExecutionMode {
    /**
     * Only Query and BatchGetItem; no Scan. Default.
     */
    STRICT_KEY_ONLY,

    /**
     * Query and BatchGetItem, plus optional Scan when no key/index is available.
     */
    ALLOW_SCAN
}

