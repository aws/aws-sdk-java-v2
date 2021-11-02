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

package software.amazon.awssdk.enhanced.dynamodb.converters;

import java.time.Instant;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;


/**
 * This class updated the LastUpdatedTimeStamp by as offset before storing to DDB.
 */
@ThreadSafe
@Immutable
public final class EpochMillisFormatTestConverter implements AttributeConverter<Instant> {

    public EpochMillisFormatTestConverter() {
    }

    public static EpochMillisFormatTestConverter create() {
        return new EpochMillisFormatTestConverter();
    }

    @Override
    public EnhancedType<Instant> type() {
        return EnhancedType.of(Instant.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

    @Override
    public AttributeValue transformFrom(Instant input) {
        return AttributeValue.builder().n(String.valueOf(input.toEpochMilli())).build();
    }

    @Override
    public Instant transformTo(AttributeValue input) {
        return Instant.ofEpochMilli(Long.parseLong(input.n()));
    }

}
