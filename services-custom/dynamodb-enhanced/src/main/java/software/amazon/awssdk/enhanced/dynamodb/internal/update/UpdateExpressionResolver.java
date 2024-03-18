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

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.isNullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.removeActionsFor;
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.setActionsFor;
import static software.amazon.awssdk.utils.CollectionUtils.filterMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 *
 */
@SdkInternalApi
public final class UpdateExpressionResolver {

    private final UpdateExpression extensionExpression;
    private final UpdateExpression requestExpression;
    private final Map<String, AttributeValue> itemNonKeyAttributes;
    private final TableMetadata tableMetadata;

    private UpdateExpressionResolver(Builder builder) {
        this.extensionExpression = builder.transformationExpression;
        this.requestExpression = builder.requestExpression;
        this.itemNonKeyAttributes = builder.nonKeyAttributes;
        this.tableMetadata = builder.tableMetadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Resolves all available and potential update expressions by priority and returns a merged update expression. It may
     * return null, if the item attribute map is empty / does not contain non-null attributes and no other update expressions
     * are present.
     * <p>
     * Conditions that will result in error:
     * <ul>
     *     <li>Two expressions contain actions referencing the same attribute</li>
     * </ul>
     * <p>
     * <b>Note: </b> The presence of attributes in update expressions submitted through the request or generated from extensions
     * take precedence over removing attributes based on item configuration.
     * For example, when IGNORE_NULLS is set to true (default), the client generates REMOVE actions for all
     * attributes in the schema that are not explicitly set in the request item submitted to the operation. If such
     * attributes are referenced in update expressions on the request or from extensions, the remove actions are filtered
     * out.
     */
    public UpdateExpression resolve() {
        UpdateExpression itemSetExpression = generateItemSetExpression(itemNonKeyAttributes, tableMetadata);

        List<String> nonRemoveAttributes = attributesPresentInExpressions(Arrays.asList(extensionExpression, requestExpression));
        UpdateExpression itemRemoveExpression = generateItemRemoveExpression(itemNonKeyAttributes, nonRemoveAttributes);

        UpdateExpression itemExpression = UpdateExpression.mergeExpressions(itemSetExpression, itemRemoveExpression);
        UpdateExpression extensionItemExpression = UpdateExpression.mergeExpressions(extensionExpression, itemExpression);
        return UpdateExpression.mergeExpressions(requestExpression, extensionItemExpression);
    }

    private static List<String> attributesPresentInExpressions(List<UpdateExpression> updateExpressions) {
        return updateExpressions.stream()
                                .map(UpdateExpressionConverter::findAttributeNames)
                                .flatMap(List::stream)
                                .collect(Collectors.toList());
    }

    public static UpdateExpression generateItemSetExpression(Map<String, AttributeValue> itemMap,
                                                             TableMetadata tableMetadata) {

        Map<String, AttributeValue> setAttributes = filterMap(itemMap, e -> !isNullAttributeValue(e.getValue()));
        return UpdateExpression.builder()
                               .actions(setActionsFor(setAttributes, tableMetadata))
                               .build();
    }

    public static UpdateExpression generateItemRemoveExpression(Map<String, AttributeValue> itemMap,
                                                                List<String> nonRemoveAttributes) {
        Map<String, AttributeValue> removeAttributes =
            filterMap(itemMap, e -> isNullAttributeValue(e.getValue()) && !nonRemoveAttributes.contains(e.getKey()));

        return UpdateExpression.builder()
                               .actions(removeActionsFor(removeAttributes))
                               .build();
    }

    public static final class Builder {

        private TableMetadata tableMetadata;
        private UpdateExpression transformationExpression;
        private UpdateExpression requestExpression;
        private Map<String, AttributeValue> nonKeyAttributes;

        public Builder tableMetadata(TableMetadata tableMetadata) {
            this.tableMetadata = tableMetadata;
            return this;
        }

        public Builder transformationExpression(UpdateExpression transformationExpression) {
            this.transformationExpression = transformationExpression;
            return this;
        }

        public Builder itemNonKeyAttributes(Map<String, AttributeValue> nonKeyAttributes) {
            this.nonKeyAttributes = nonKeyAttributes;
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
