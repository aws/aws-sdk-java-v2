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
package software.amazon.awssdk.mapper.dynamodb.internal.marshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller.BooleanAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals Java {@code Boolean} objects to Dynamodb-native
 * {@code BOOL} attribute values.
 */
@SdkInternalApi
public class BooleanToBooleanMarshaller implements BooleanAttributeMarshaller {

    private static final BooleanToBooleanMarshaller INSTANCE =
            new BooleanToBooleanMarshaller();

    public static BooleanToBooleanMarshaller instance() {
        return INSTANCE;
    }

    private BooleanToBooleanMarshaller() {
    }

    @Override
    public AttributeValue marshall(Object obj) {
        return AttributeValue.builder().bool((Boolean) obj).build();
    }
}
