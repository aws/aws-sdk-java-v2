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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;

/**
 * Controls how update actions from the POJO item, {@link DynamoDbEnhancedClientExtension extensions}, and the request’s
 * {@link UpdateExpression} are merged into a single update expression for DynamoDB.
 *
 * <p><b>{@link #LEGACY}</b> (default) &mdash; Concatenate all actions. DynamoDB fails the request if any document paths overlap.
 * Additionally, when a POJO field is {@code null}, its {@code REMOVE} is omitted if that attribute name also appears in an
 * extension or request expression, so the same name is not both removed and set in one call.
 *
 * <p><b>{@link #PRIORITIZE_HIGHER_SOURCE}</b> &mdash; Omits lower-priority actions whose <em>resolved</em> path overlaps a
 * higher-priority path, using the same overlap rules as DynamoDB. Expression name placeholders ({@code #token}) are resolved
 * first. Two paths overlap if they are equal or one continues the other at {@code .} (map segment) or {@code [} (list index).
 * Sibling map keys or different list indices do not overlap.
 *
 * <p><b>When paths overlap:</b> the request wins over the extension over the POJO.
 *
 * <p><b>How the merged expression is built:</b>
 * <ol>
 *   <li>Include all update actions from the request.</li>
 *   <li>Include an extension action only if its path does not overlap any request path.</li>
 *   <li>Include a POJO-derived action only if its path does not overlap any request path and does not overlap any extension
 *       action included in step 2.</li>
 * </ol>
 * <p>Extension actions omitted in step 2 are not part of the merged expression and are not considered when applying step 3.
 *
 * <p><b>Examples:</b>
 * <ul>
 *   <li>{@code profile.name} and {@code profile.city} &mdash; no overlap; both may appear in the merged expression.</li>
 *   <li>{@code profile} and {@code profile.name} &mdash; overlap; the lower-priority action is omitted.</li>
 *   <li>{@code items[0]} and {@code items[1]} &mdash; no overlap; both may appear in the merged expression.</li>
 *   <li>Two actions on {@code items[0]} &mdash; overlap; only the higher-priority source contributes its action.</li>
 * </ul>
 *
 * <p>Default: {@link #LEGACY}.
 *
 * @see UpdateItemEnhancedRequest.Builder#updateExpressionMergeStrategy(UpdateExpressionMergeStrategy)
 * @see TransactUpdateItemEnhancedRequest.Builder#updateExpressionMergeStrategy(UpdateExpressionMergeStrategy)
 */
@SdkPublicApi
public enum UpdateExpressionMergeStrategy {
    LEGACY,
    PRIORITIZE_HIGHER_SOURCE
}
