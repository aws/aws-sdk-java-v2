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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.AttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link StringBuffer} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string.
 *
 * <p>
 * This supports reading any DynamoDB attribute type into a string buffer.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class StringBufferAttributeConverter implements AttributeConverter<StringBuffer> {
    public static final StringAttributeConverter STRING_CONVERTER = StringAttributeConverter.create();

    public static StringBufferAttributeConverter create() {
        return new StringBufferAttributeConverter();
    }

    @Override
    public TypeToken<StringBuffer> type() {
        return TypeToken.of(StringBuffer.class);
    }

    @Override
    public AttributeValue transformFrom(StringBuffer input) {
        return STRING_CONVERTER.transformFrom(input.toString());
    }

    @Override
    public StringBuffer transformTo(AttributeValue input) {
        return new StringBuffer(STRING_CONVERTER.transformTo(input));
    }
}
