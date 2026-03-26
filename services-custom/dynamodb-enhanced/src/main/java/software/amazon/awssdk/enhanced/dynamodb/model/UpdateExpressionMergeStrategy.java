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

package software.amazon.awssdk.enhanced.dynamodb.model;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Controls how update actions from three sources (POJO attributes, extensions, and request-level expressions) are combined before
 * being sent to DynamoDB.
 *
 * <ul>
 *   <li>{@link #LEGACY} (default) &mdash; all actions are concatenated as-is. If two actions target overlapping
 *       document paths (for example, replacing an entire attribute and also updating a nested path under that same
 *       attribute), DynamoDB rejects the request with a "Two document paths overlap" error. The only automatic safety
 *       is that if a POJO attribute is {@code null}, its {@code REMOVE} action is suppressed when the same attribute
 *       name appears in an extension or request expression.</li>
 *
 *  <li>{@link #PRIORITIZE_HIGHER_SOURCE} &mdash; actions are grouped by <em>top-level attribute name</em> (see below).
 *       For each name, only actions from the single highest-priority source that references that name are kept.
 *       Priority (highest to lowest): request &gt; extension &gt; POJO. Different top-level names do not compete with
 *       each other: one attribute may contribute only request actions and another only extension actions, and both
 *       groups still appear in the merged expression.</li>
 * </ul>
 *
 * <p><b>Top-level name (for {@link #PRIORITIZE_HIGHER_SOURCE}):</b> resolve expression-name placeholders, then take the
 * part of the path before the first {@code .} or {@code [} (for example, {@code list[0]} &rarr; {@code list}, and
 * {@code object.listAttr[0]} &rarr; {@code object}). If multiple sources update paths with the same top-level name,
 * only the highest-priority source's actions for that attribute are kept.
 * Precedence is: request &gt; extension &gt; POJO.
 *
 * <p>Default: {@link #LEGACY}. Not setting this flag preserves backward-compatible behavior.
 *
 * @see UpdateItemEnhancedRequest.Builder#updateExpressionMergeStrategy(UpdateExpressionMergeStrategy)
 * @see TransactUpdateItemEnhancedRequest.Builder#updateExpressionMergeStrategy(UpdateExpressionMergeStrategy)
 */
@SdkPublicApi
public enum UpdateExpressionMergeStrategy {
    LEGACY,
    PRIORITIZE_HIGHER_SOURCE
}
