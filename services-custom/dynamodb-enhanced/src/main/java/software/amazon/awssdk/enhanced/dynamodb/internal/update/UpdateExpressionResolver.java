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

package software.amazon.awssdk.enhanced.dynamodb.internal.update;

import static java.util.Objects.requireNonNull;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.attributesPresentInOtherExpressions;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.generateItemRemoveExpression;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.generateItemSetExpression;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.resolveDocumentPath;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy;
import software.amazon.awssdk.enhanced.dynamodb.update.AddAction;
import software.amazon.awssdk.enhanced.dynamodb.update.DeleteAction;
import software.amazon.awssdk.enhanced.dynamodb.update.RemoveAction;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Builds one {@link UpdateExpression} from three places the enhanced client can get updates:
 * <ul>
 *   <li>Non-key fields on the item (POJO) &mdash; turned into {@code SET} / {@code REMOVE} actions</li>
 *   <li>An optional expression from {@link software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension
 *       extensions}</li>
 *   <li>An optional {@link UpdateExpression} on the update request</li>
 * </ul>
 * <p>
 * How those pieces are combined is controlled by {@link UpdateExpressionMergeStrategy} on the request. This class applies
 * that strategy so the result can be sent to DynamoDB as a single update expression.
 *
 * @see UpdateExpressionMergeStrategy
 */
@SdkInternalApi
public final class UpdateExpressionResolver {

    private final TableMetadata tableMetadata;
    private final Map<String, AttributeValue> nonKeyAttributes;
    private final UpdateExpression extensionExpression;
    private final UpdateExpression requestExpression;
    private final UpdateExpressionMergeStrategy updateExpressionMergeStrategy;

    private UpdateExpressionResolver(Builder builder) {
        this.tableMetadata = builder.tableMetadata;
        this.nonKeyAttributes = builder.nonKeyAttributes;
        this.extensionExpression = builder.extensionExpression;
        this.requestExpression = builder.requestExpression;
        this.updateExpressionMergeStrategy = builder.updateExpressionMergeStrategy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private TableMetadata tableMetadata;
        private Map<String, AttributeValue> nonKeyAttributes = Collections.emptyMap();
        private UpdateExpression extensionExpression;
        private UpdateExpression requestExpression;
        private UpdateExpressionMergeStrategy updateExpressionMergeStrategy = UpdateExpressionMergeStrategy.LEGACY;

        /**
         * Builds a {@link UpdateExpressionResolver}. When {@link #nonKeyAttributes(Map)} is non-empty,
         * {@link #tableMetadata(TableMetadata)} is required so item {@code SET} and {@code REMOVE} actions can be generated.
         *
         * @return a new resolver instance
         */
        public UpdateExpressionResolver build() {
            return new UpdateExpressionResolver(this);
        }

        public Builder tableMetadata(TableMetadata tableMetadata) {
            this.tableMetadata = requireNonNull(
                tableMetadata, "A TableMetadata is required when generating an Update Expression");
            return this;
        }

        public Builder nonKeyAttributes(Map<String, AttributeValue> nonKeyAttributes) {
            if (nonKeyAttributes == null) {
                this.nonKeyAttributes = Collections.emptyMap();
            } else {
                this.nonKeyAttributes = Collections.unmodifiableMap(new HashMap<>(nonKeyAttributes));
            }
            return this;
        }

        public Builder extensionExpression(UpdateExpression extensionExpression) {
            this.extensionExpression = extensionExpression;
            return this;
        }

        public Builder requestExpression(UpdateExpression requestExpression) {
            this.requestExpression = requestExpression;
            return this;
        }

        public Builder updateExpressionMergeStrategy(UpdateExpressionMergeStrategy updateExpressionMergeStrategy) {
            this.updateExpressionMergeStrategy = updateExpressionMergeStrategy == null
                                                 ? UpdateExpressionMergeStrategy.LEGACY
                                                 : updateExpressionMergeStrategy;
            return this;
        }
    }

    /**
     * Returns a single merged {@link UpdateExpression} ready for DynamoDB, or {@code null} if there is nothing to update.
     * <p>
     * <b>What gets merged</b> &mdash; If the builder supplied non-key attribute values, those are converted into item
     * {@code SET} and {@code REMOVE} actions first. Those are then combined with the extension expression and the request
     * expression (either may be absent).
     * <p>
     * <b>{@link UpdateExpressionMergeStrategy#LEGACY}</b> (default) &mdash; All actions from all sources are chained together.
     * If DynamoDB considers any two paths to overlap, the service rejects the request. One safeguard remains: a {@code REMOVE}
     * for a {@code null} POJO field is skipped when that attribute name also appears in the extension or request expression.
     * <p>
     * <b>{@link UpdateExpressionMergeStrategy#PRIORITIZE_HIGHER_SOURCE}</b> &mdash; Actions are merged in three passes: keep
     * every request action; keep extension actions that do not overlap any request path; keep item actions that do not overlap
     * the request or any extension action that was kept. Full rules, examples, and the definition of path overlap are on
     * {@link UpdateExpressionMergeStrategy}.
     *
     * @return the merged expression, or {@code null} if no actions remain
     * @see UpdateExpressionMergeStrategy
     */
    public UpdateExpression resolve() {
        UpdateExpression itemExpression = null;

        if (!nonKeyAttributes.isEmpty()) {
            Set<String> attributesExcludedFromRemoval = attributesPresentInOtherExpressions(
                Arrays.asList(extensionExpression, requestExpression));

            itemExpression = UpdateExpression.mergeExpressions(
                generateItemSetExpression(nonKeyAttributes, tableMetadata),
                generateItemRemoveExpression(nonKeyAttributes, attributesExcludedFromRemoval));
        }

        if (updateExpressionMergeStrategy == UpdateExpressionMergeStrategy.PRIORITIZE_HIGHER_SOURCE) {
            return mergeWithPathPriority(itemExpression, extensionExpression, requestExpression);
        }

        return Stream.of(itemExpression, extensionExpression, requestExpression)
                     .filter(Objects::nonNull)
                     .reduce(UpdateExpression::mergeExpressions)
                     .orElse(null);
    }

    TableMetadata tableMetadata() {
        return tableMetadata;
    }

    Map<String, AttributeValue> nonKeyAttributes() {
        return nonKeyAttributes;
    }

    UpdateExpression extensionExpression() {
        return extensionExpression;
    }

    UpdateExpression requestExpression() {
        return requestExpression;
    }

    UpdateExpressionMergeStrategy updateExpressionMergeStrategy() {
        return updateExpressionMergeStrategy;
    }

    /**
     * {@link UpdateExpressionMergeStrategy#PRIORITIZE_HIGHER_SOURCE}: merge request, then extension actions that do not conflict
     * with the request, then item actions that do not conflict with the request or retained extension paths.
     */
    private static UpdateExpression mergeWithPathPriority(UpdateExpression itemExpression,
                                                          UpdateExpression extensionExpression,
                                                          UpdateExpression requestExpression) {
        Set<String> requestResolvedPaths = resolvedPaths(requestExpression);
        UpdateExpression extensionExpressionFiltered =
            excludeOverlappingActions(extensionExpression, requestResolvedPaths);

        Set<String> higherPriorityResolvedPaths = new HashSet<>(requestResolvedPaths);
        higherPriorityResolvedPaths.addAll(resolvedPaths(extensionExpressionFiltered));
        UpdateExpression itemExpressionFiltered =
            excludeOverlappingActions(itemExpression, higherPriorityResolvedPaths);

        return Stream.of(requestExpression, extensionExpressionFiltered, itemExpressionFiltered)
                     .filter(Objects::nonNull)
                     .reduce(UpdateExpression::mergeExpressions)
                     .orElse(null);
    }

    private static Stream<UpdateAction> streamActions(UpdateExpression expression) {
        return Stream.of(expression.setActions().stream(),
                         expression.removeActions().stream(),
                         expression.deleteActions().stream(),
                         expression.addActions().stream())
                     .flatMap(Function.identity());
    }

    private static Set<String> resolvedPaths(UpdateExpression expression) {
        if (expression == null) {
            return Collections.emptySet();
        }
        return streamActions(expression)
            .map(UpdateExpressionResolver::resolvePath)
            .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Copy of {@code expression} without actions whose path overlaps any path in {@code higherPriorityResolvedPaths}.
     * {@code null} if nothing remains; unchanged when the path set is empty (or when {@code expression} is {@code null}).
     */
    private static UpdateExpression excludeOverlappingActions(UpdateExpression expression,
                                                              Set<String> higherPriorityResolvedPaths) {
        if (expression == null) {
            return null;
        }
        if (higherPriorityResolvedPaths.isEmpty()) {
            return expression;
        }
        List<UpdateAction> retained = streamActions(expression)
            .filter(action -> !conflictsWith(resolvePath(action), higherPriorityResolvedPaths))
            .collect(Collectors.toList());

        return retained.isEmpty() ? null : UpdateExpression.builder().actions(retained).build();
    }

    private static String resolvePath(UpdateAction action) {
        if (action instanceof SetAction) {
            SetAction a = (SetAction) action;
            return resolveDocumentPath(a.path(), a.expressionNames());
        }
        if (action instanceof RemoveAction) {
            RemoveAction a = (RemoveAction) action;
            return resolveDocumentPath(a.path(), a.expressionNames());
        }
        if (action instanceof DeleteAction) {
            DeleteAction a = (DeleteAction) action;
            return resolveDocumentPath(a.path(), a.expressionNames());
        }
        if (action instanceof AddAction) {
            AddAction a = (AddAction) action;
            return resolveDocumentPath(a.path(), a.expressionNames());
        }
        throw new IllegalArgumentException("Unsupported UpdateAction: " + action.getClass());
    }

    /**
     * Whether {@code candidatePath} overlaps any path in {@code higherPriorityPaths} in the DynamoDB document sense: same path,
     * or one path is a prefix of the other at a {@code .} or {@code [} boundary.
     */
    private static boolean conflictsWith(String candidatePath, Set<String> higherPriorityPaths) {
        return higherPriorityPaths.stream()
                                  .anyMatch(higher ->
                                                candidatePath.equals(higher)
                                                || candidatePath.startsWith(higher + ".")
                                                || candidatePath.startsWith(higher + "[")
                                                || higher.startsWith(candidatePath + ".")
                                                || higher.startsWith(candidatePath + "["));
    }
}
