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
 * Join types supported by the complex query API when joining a base table to a joined table on a common attribute (e.g.
 * customerId).
 *
 * <ul>
 *   <li><b>INNER</b>: Only rows that have a matching pair (base row and joined row with the same join key)
 *       are emitted. Base rows with no matching joined row, and joined rows with no matching base row,
 *       are omitted.</li>
 *   <li><b>LEFT</b>: Every base row is emitted. When there is at least one joined row with the same join key,
 *       one result row per (base, joined) pair is emitted. When there is no matching joined row (or the base
 *       join key is null), a single row is emitted with the base and an empty joined side.</li>
 *   <li><b>RIGHT</b>: Every joined row is emitted. When there is at least one base row with the same join key,
 *       those pairs are emitted during the base-driven phase. Joined rows whose join key never appeared on the
 *       base side are emitted as rows with an empty base and that joined row.</li>
 *   <li><b>FULL</b>: Union of LEFT and RIGHT. Each base row appears at least once (with empty joined when no
 *       match); each joined row appears at least once (with empty base when no match); matching pairs appear
 *       as (base, joined) rows.</li>
 * </ul>
 */
public enum JoinType {
    /**
     * Only matching (base, joined) pairs; no base-only or joined-only rows.
     */
    INNER,

    /**
     * Every base row; joined side empty when there is no match.
     */
    LEFT,

    /**
     * Every joined row; base side empty when there is no match.
     */
    RIGHT,

    /**
     * Every base and every joined row; unmatched sides are empty.
     */
    FULL
}

