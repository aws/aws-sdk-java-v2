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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkPublicApi
public final class OptimisticLockingHelper {

    private static final String CUSTOM_METADATA_KEY = "VersionedRecordExtension:VersionAttribute";

    private OptimisticLockingHelper() {
    }

    public static DeleteItemEnhancedRequest withOptimisticLocking(
        DeleteItemEnhancedRequest request, AttributeValue oldVersionValue, String versionAttributeName) {

        Expression conditionExpression = createVersionCondition(oldVersionValue, versionAttributeName);
        return request.toBuilder()
                      .conditionExpression(conditionExpression)
                      .build();
    }

    public static TransactDeleteItemEnhancedRequest withOptimisticLocking(
        TransactDeleteItemEnhancedRequest request, AttributeValue oldVersionValue, String versionAttributeName) {

        Expression conditionExpression = createVersionCondition(oldVersionValue, versionAttributeName);
        return request.toBuilder()
                      .conditionExpression(conditionExpression)
                      .build();
    }

    public static <T> DeleteItemEnhancedRequest withOptimisticLocking(
        DeleteItemEnhancedRequest request, T keyItem, TableSchema<T> tableSchema) {
        
        Optional<String> versionAttribute = getVersionAttributeName(tableSchema);
        if (versionAttribute.isPresent()) {
            AttributeValue version = tableSchema.attributeValue(keyItem, versionAttribute.get());
            if (version != null) {
                return withOptimisticLocking(request, version, versionAttribute.get());
            }
        }
        return request;
    }

    public static <T> Optional<String> getVersionAttributeName(
        software.amazon.awssdk.enhanced.dynamodb.TableSchema<T> tableSchema) {
        return tableSchema.tableMetadata().customMetadataObject(CUSTOM_METADATA_KEY, String.class);
    }

    private static Expression createVersionCondition(AttributeValue oldVersionValue, String versionAttributeName) {
        return Expression.builder()
                         .expression(versionAttributeName + " = :version_value")
                         .putExpressionValue(":version_value", oldVersionValue)
                         .build();
    }
}