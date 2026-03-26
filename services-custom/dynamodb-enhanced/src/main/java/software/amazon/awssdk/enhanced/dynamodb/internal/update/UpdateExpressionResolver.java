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
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.isNullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionConverter.findAttributeNames;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionConverter.removeNestingAndListReference;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.removeActionsFor;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.setActionsFor;
import static software.amazon.awssdk.utils.CollectionUtils.filterMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpressionMergeStrategy;
import software.amazon.awssdk.enhanced.dynamodb.update.SetAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateAction;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Merges update actions from POJO attributes, extensions, and request-level expressions into a single {@link UpdateExpression}.
 * Merge behavior is controlled by {@link UpdateExpressionMergeStrategy}.
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

    /**
     * Merges update actions from POJO, extension, and request sources into one {@link UpdateExpression}. Previously, all sources
     * were always concatenated and sent to DynamoDB; when two actions targeted overlapping document paths (for example, replacing
     * an entire attribute and also updating a nested path under that same attribute), the service responded with a "Two document
     * paths overlap" error.
     * <p>
     * To avoid a breaking change, {@link UpdateExpressionMergeStrategy} was added: it defaults to
     * {@link UpdateExpressionMergeStrategy#LEGACY}, preserving that original merge behavior. When set to
     * {@link UpdateExpressionMergeStrategy#PRIORITIZE_HIGHER_SOURCE}, the resolver drops conflicting lower-priority actions per
     * top-level attribute name so the request can succeed.
     *
     * <ul>
     *   <li><b>{@link UpdateExpressionMergeStrategy#LEGACY}</b> (default) &mdash; concatenates all actions as-is;
     *       overlapping paths cause a DynamoDB runtime error. Null-attribute REMOVE actions are suppressed when the
     *       same attribute appears in an extension or request expression.</li>
     *
     *   <li><b>{@link UpdateExpressionMergeStrategy#PRIORITIZE_HIGHER_SOURCE}</b> &mdash; groups actions by top-level
     *       attribute name (path before first {@code .} or {@code [}). For each name, only the highest-priority
     *       source's actions are kept: <em>request &gt; extension &gt; POJO</em>. Different top-level names do not
     *       compete with each other: one attribute may contribute only request actions and another only extension actions,
     *       and both groups still appear in the merged expression.</li>
     * </ul>
     *
     * @return the merged expression, or {@code null} when no updates are needed
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
            return mergeBySourcePriority(itemExpression, extensionExpression, requestExpression);
        }

        return Stream.of(itemExpression, extensionExpression, requestExpression)
                     .filter(Objects::nonNull)
                     .reduce(UpdateExpression::mergeExpressions)
                     .orElse(null);
    }

    private static Set<String> attributesPresentInOtherExpressions(Collection<UpdateExpression> updateExpressions) {
        return updateExpressions.stream()
                                .filter(Objects::nonNull)
                                .map(UpdateExpressionConverter::findAttributeNames)
                                .flatMap(List::stream)
                                .collect(Collectors.toSet());
    }

    private static UpdateExpression generateItemSetExpression(Map<String, AttributeValue> itemMap,
                                                              TableMetadata tableMetadata) {

        Map<String, AttributeValue> setAttributes = filterMap(itemMap, e -> !isNullAttributeValue(e.getValue()));
        return UpdateExpression.builder()
                               .actions(setActionsFor(setAttributes, tableMetadata))
                               .build();
    }

    private static UpdateExpression generateItemRemoveExpression(Map<String, AttributeValue> itemMap,
                                                                 Collection<String> nonRemoveAttributes) {
        Map<String, AttributeValue> removeAttributes =
            filterMap(itemMap, e -> isNullAttributeValue(e.getValue()) && !nonRemoveAttributes.contains(e.getKey()));

        return UpdateExpression.builder()
                               .actions(removeActionsFor(removeAttributes))
                               .build();
    }

    /**
     * For {@link UpdateExpressionMergeStrategy#PRIORITIZE_HIGHER_SOURCE}: assigns each top-level attribute name to at most one
     * source by priority (request, then extension, then POJO), then keeps only that source's actions for each assigned name.
     */
    private static UpdateExpression mergeBySourcePriority(UpdateExpression itemExpression,
                                                          UpdateExpression extensionExpression,
                                                          UpdateExpression requestExpression) {

        Set<String> requestOwned = new HashSet<>(findAttributeNames(requestExpression));
        Set<String> extensionOwned = new HashSet<>(findAttributeNames(extensionExpression));

        // Request wins over extension: extension only retains attribute names not already in the request expression.
        extensionOwned.removeAll(requestOwned);

        Set<String> itemOwned = new HashSet<>(findAttributeNames(itemExpression));
        // POJO-derived item expression is the lowest priority: drop attribute names claimed by request, then by extension.
        itemOwned.removeAll(requestOwned);
        itemOwned.removeAll(extensionOwned);

        return Stream.of(
                         filterByAttributes(requestExpression, requestOwned),
                         filterByAttributes(extensionExpression, extensionOwned),
                         filterByAttributes(itemExpression, itemOwned)
                     ).filter(Objects::nonNull)
                     .reduce(UpdateExpression::mergeExpressions)
                     .orElse(null);
    }

    /**
     * Returns a new {@link UpdateExpression} containing only actions whose resolved top-level attribute name is in
     * {@code attributeNames}, or {@code null} if nothing matches.
     */
    private static UpdateExpression filterByAttributes(UpdateExpression expression, Set<String> attributeNames) {
        if (expression == null || attributeNames.isEmpty()) {
            return null;
        }
        List<UpdateAction> retainedActions = new ArrayList<>();

        expression.setActions().stream()
                  .filter(action -> attributeNames.contains(baseAttributeName(action.path(), action.expressionNames())))
                  .forEach(retainedActions::add);

        expression.removeActions().stream()
                  .filter(action -> attributeNames.contains(baseAttributeName(action.path(), action.expressionNames())))
                  .forEach(retainedActions::add);

        expression.deleteActions().stream()
                  .filter(action -> attributeNames.contains(baseAttributeName(action.path(), action.expressionNames())))
                  .forEach(retainedActions::add);

        expression.addActions().stream()
                  .filter(action -> attributeNames.contains(baseAttributeName(action.path(), action.expressionNames())))
                  .forEach(retainedActions::add);

        return retainedActions.isEmpty()
               ? null
               : UpdateExpression.builder().actions(retainedActions).build();
    }

    /**
     * Returns the <em>root</em> DynamoDB attribute name for an update action path, used when merging expressions (for example to
     * decide which actions belong together or overlap).
     * <p>
     * The path is resolved in two steps:
     * <ol>
     *   <li>Substitute {@link SetAction#expressionNames() expression name} placeholders: each entry's key is replaced by its
     *   value in {@code fullAttributePath} (in map iteration order), as with {@code ExpressionAttributeNames}.</li>
     *   <li>{@link UpdateExpressionConverter#removeNestingAndListReference(String)} then takes the segment before the first
     *       {@code .} (nested map) or {@code [} (list index)—the top-level attribute stored on the item.</li>
     * </ol>
     * <p>
     * Examples (after any placeholder substitution):
     * <ul>
     *   <li>{@code list[0]} → {@code list}</li>
     *   <li>{@code object.listAttr[0]} → {@code object}</li>
     * </ul>
     */
    private static String baseAttributeName(String fullAttributePath, Map<String, String> expressionNames) {
        String result = fullAttributePath;

        Map<String, String> names = expressionNames != null ? expressionNames : Collections.emptyMap();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return removeNestingAndListReference(result);
    }

    public static final class Builder {

        private TableMetadata tableMetadata;
        private Map<String, AttributeValue> nonKeyAttributes = Collections.emptyMap();
        private UpdateExpression extensionExpression;
        private UpdateExpression requestExpression;
        private UpdateExpressionMergeStrategy updateExpressionMergeStrategy = UpdateExpressionMergeStrategy.LEGACY;

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

        public UpdateExpressionResolver build() {
            return new UpdateExpressionResolver(this);
        }
    }
}
