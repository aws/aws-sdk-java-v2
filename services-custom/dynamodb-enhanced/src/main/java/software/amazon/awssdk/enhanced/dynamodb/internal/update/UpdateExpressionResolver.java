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
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.removeActionsFor;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.setActionsFor;
import static software.amazon.awssdk.utils.CollectionUtils.filterMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Resolves and merges UpdateExpressions from multiple sources (item attributes, extensions, requests) with priority-based
 * conflict resolution and smart filtering to prevent attribute conflicts.
 */
@SdkInternalApi
public final class UpdateExpressionResolver {

    private final TableMetadata tableMetadata;
    private final Map<String, AttributeValue> nonKeyAttributes;
    private final UpdateExpression extensionExpression;
    private final UpdateExpression requestExpression;

    private UpdateExpressionResolver(Builder builder) {
        this.tableMetadata = builder.tableMetadata;
        this.nonKeyAttributes = builder.nonKeyAttributes;
        this.extensionExpression = builder.extensionExpression;
        this.requestExpression = builder.requestExpression;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Merges UpdateExpressions from three sources with priority: item attributes (lowest), extension expressions (medium),
     * request expressions (highest).
     *
     * <p><b>Steps:</b> Identify attributes used by extensions/requests to prevent REMOVE conflicts →
     * create item SET/REMOVE actions → merge extensions (override item) → merge request (override all).
     *
     * <p><b>Backward compatibility:</b> Without request expressions, behavior is identical to previous versions.
     * <p><b>Exceptions:</b> DynamoDbException may be thrown when the same attribute is updated by multiple sources.
     *
     * @return merged UpdateExpression, or empty if no updates needed
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

    public static UpdateExpression generateItemSetExpression(Map<String, AttributeValue> itemMap,
                                                             TableMetadata tableMetadata) {

        Map<String, AttributeValue> setAttributes = filterMap(itemMap, e -> !isNullAttributeValue(e.getValue()));
        return UpdateExpression.builder()
                               .actions(setActionsFor(setAttributes, tableMetadata))
                               .build();
    }

    public static UpdateExpression generateItemRemoveExpression(Map<String, AttributeValue> itemMap,
                                                                Collection<String> nonRemoveAttributes) {
        Map<String, AttributeValue> removeAttributes =
            filterMap(itemMap, e -> isNullAttributeValue(e.getValue()) && !nonRemoveAttributes.contains(e.getKey()));

        return UpdateExpression.builder()
                               .actions(removeActionsFor(removeAttributes))
                               .build();
    }

    public static final class Builder {

        private TableMetadata tableMetadata;
        private Map<String, AttributeValue> nonKeyAttributes;
        private UpdateExpression extensionExpression;
        private UpdateExpression requestExpression;

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

        public UpdateExpressionResolver build() {
            return new UpdateExpressionResolver(this);
        }

    }
}