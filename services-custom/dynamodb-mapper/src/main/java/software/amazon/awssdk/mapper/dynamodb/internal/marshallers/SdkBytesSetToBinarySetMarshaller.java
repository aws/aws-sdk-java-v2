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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller.BinarySetAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Marshals sets of immutable {@link SdkBytes} values into DynamoDB binary-set attributes.
 */
@SdkInternalApi
public final class SdkBytesSetToBinarySetMarshaller implements BinarySetAttributeMarshaller {

    private static final SdkBytesSetToBinarySetMarshaller INSTANCE = new SdkBytesSetToBinarySetMarshaller();

    public static SdkBytesSetToBinarySetMarshaller instance() {
        return INSTANCE;
    }

    private SdkBytesSetToBinarySetMarshaller() {
    }

    @Override
    public AttributeValue marshall(Object obj) {
        @SuppressWarnings("unchecked")
        Set<SdkBytes> sdkBytes = (Set<SdkBytes>) obj;
        List<SdkBytes> attributes = new ArrayList<SdkBytes>(sdkBytes);
        return AttributeValue.createBs(attributes);
    }
}
