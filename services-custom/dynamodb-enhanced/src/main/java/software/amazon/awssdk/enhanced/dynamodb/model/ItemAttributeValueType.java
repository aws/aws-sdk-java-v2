/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An enum of all types that are supported by DynamoDB's {@link AttributeValue} and the enhanced client's
 * {@link ItemAttributeValue}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public enum ItemAttributeValueType {
    MAP,
    STRING,
    NUMBER,
    BYTES,
    BOOLEAN,
    NULL,
    SET_OF_STRINGS,
    SET_OF_NUMBERS,
    SET_OF_BYTES,
    LIST_OF_ATTRIBUTE_VALUES
}
