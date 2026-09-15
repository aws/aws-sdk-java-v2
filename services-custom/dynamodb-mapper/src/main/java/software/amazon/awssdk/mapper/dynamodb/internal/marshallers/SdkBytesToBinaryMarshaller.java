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

package software.amazon.awssdk.mapper.dynamodb.internal.marshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller.BinaryAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Marshals immutable {@link SdkBytes} values into DynamoDB binary attributes.
 */
@SdkInternalApi
public final class SdkBytesToBinaryMarshaller implements BinaryAttributeMarshaller {

    private static final SdkBytesToBinaryMarshaller INSTANCE = new SdkBytesToBinaryMarshaller();

    public static SdkBytesToBinaryMarshaller instance() {
        return INSTANCE;
    }

    private SdkBytesToBinaryMarshaller() {
    }

    @Override
    public AttributeValue marshall(Object obj) {
        return AttributeValue.createB((SdkBytes) obj);
    }
}
