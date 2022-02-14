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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.ImmutableTableSchema;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class DynamoDbEnhancedLogger {
    /**
     * Logger used for to assist customers in debugging {@link BeanTableSchema} and {@link ImmutableTableSchema} loading.
     */
    public static final Logger BEAN_LOGGER = Logger.loggerFor("software.amazon.awssdk.enhanced.dynamodb.beans");

    private DynamoDbEnhancedLogger() {
    }
}
