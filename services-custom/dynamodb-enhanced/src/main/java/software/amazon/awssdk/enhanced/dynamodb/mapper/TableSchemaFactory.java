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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.ExecutionContext;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

@SdkInternalApi
public class TableSchemaFactory {

    private TableSchemaFactory() {
    }

    public static <T> TableSchema<T> fromClass(Class<T> annotatedClass, ExecutionContext context) {
        if (annotatedClass.getAnnotation(DynamoDbImmutable.class) != null) {
            return ImmutableTableSchema.create(annotatedClass, context);
        }

        if (annotatedClass.getAnnotation(DynamoDbBean.class) != null) {
            return BeanTableSchema.create(annotatedClass, context);
        }

        throw new IllegalArgumentException("Class does not appear to be a valid DynamoDb annotated class. [class = " +
                                           "\"" + annotatedClass + "\"]");
    }
}
