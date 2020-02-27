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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.converter.string.bundled.ZoneIdStringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between {@link ZoneId} and {@link AttributeValue}.
 *
 * <p>
 * This stores and reads values in DynamoDB as a string using {@link ZoneId#toString()} and {@link ZoneId#of(String)}.
 *
 * <p>
 * This can be created via {@link #create()}.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public final class ZoneIdAttributeConverter implements AttributeConverter<ZoneId> {
    public static final ZoneIdStringConverter STRING_CONVERTER = ZoneIdStringConverter.create();

    public static ZoneIdAttributeConverter create() {
        return new ZoneIdAttributeConverter();
    }

    @Override
    public TypeToken<ZoneId> type() {
        return TypeToken.of(ZoneId.class);
    }

    @Override
    public AttributeValue transformFrom(ZoneId input) {
        return ItemAttributeValue.fromString(STRING_CONVERTER.toString(input)).toGeneratedAttributeValue();
    }

    @Override
    public ZoneId transformTo(AttributeValue input) {
        try {
            return STRING_CONVERTER.fromString(input.s());
        } catch (ZoneRulesException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
