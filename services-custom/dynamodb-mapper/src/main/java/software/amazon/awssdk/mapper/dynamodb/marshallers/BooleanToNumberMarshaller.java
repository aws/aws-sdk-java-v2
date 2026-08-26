/*
 * Copyright 2014-2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.mapper.dynamodb.marshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller.NumberAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A legacy marshaller that marshals Java {@code Booleans} into DynamoDB
 * Numbers, representing {@code true} as '1' and {@code false} as '0'. Retained
 * for backwards compatibility with older versions of the mapper which don't
 * know about the DynamoDB BOOL type.
 */
@SdkInternalApi
public class BooleanToNumberMarshaller implements NumberAttributeMarshaller {

    private static final BooleanToNumberMarshaller INSTANCE =
            new BooleanToNumberMarshaller();

    public static BooleanToNumberMarshaller instance() {
        return INSTANCE;
    }

    private BooleanToNumberMarshaller() {
    }

    @Override
    public AttributeValue marshall(Object obj) {
        Boolean bool = (Boolean) obj;
        if (bool == null || bool == false) {
            return AttributeValue.builder().n("0").build();
        } else {
            return AttributeValue.builder().n("1").build();
        }
    }
}
