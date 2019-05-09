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

package software.amazon.awssdk.enhanced.dynamodb.converter;

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.InstantConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.StringConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.ConvertableItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.GeneratedResponseItem;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.model.RequestItem;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A converter between Java types and DynamoDB types.
 *
 * <p>
 * Examples:
 * <ul>
 *     <li>The {@link StringConverter} converts a {@link String} into a DynamoDB string ({@link AttributeValue#s()}).</li>
 *     <li>The {@link InstantConverter} converts an {@link Instant} into a DynamoDB number ({@link AttributeValue#n()}).</li>
 * </ul>
 *
 * <p>
 * More specifically, this converts Java types into {@link ItemAttributeValue}s, which are more user-friendly representations of
 * the generated {@link AttributeValue}. An {@link ItemAttributeValue} can always be converted to a generated
 * {@link AttributeValue} using {@link ItemAttributeValue#toGeneratedAttributeValue()}.
 *
 * <p>
 * <b>Default Converters</b>
 *
 * <p>
 * Most built-in Java types have some defined conversion behavior. The exhaustive list of supported classes can be found in the
 * {@link DefaultConverterChain}.
 *
 * <p>
 * <b>Converter Precedence</b>
 *
 * <p>
 * When converting from Java types, {@link ItemAttributeValueConverter}s have a precedence, based on:
 * <ol>
 *     <li>Where they were configured.</li>
 *     <li>The converter's {@link ConversionCondition}.</li>
 *     <li>The order in which they were configured.</li>
 * </ol>
 *
 * <p>
 * <i>Converter Precedence - Where they were configured</i>
 *
 * <p>
 * Converters can be specified in three places:
 * <ol>
 *     <li>{@link RequestItem}-level converters are those added with the {@link RequestItem.Builder}.</li>
 *     <li>{@link DynamoDbEnhancedClient}-level converters are those added with the {@link DynamoDbEnhancedClient.Builder}.</li>
 *     <li>Default-level converters are those included by default with the client ({@link DefaultConverterChain}).</li>
 * </ol>
 *
 * <p>
 * Converters configured in a {@link RequestItem.Builder} always take precedence over the ones configured with
 * {@link DynamoDbEnhancedClient.Builder} and in the {@link DefaultConverterChain}. Converters configured in a
 * {@link DynamoDbEnhancedClient.Builder} always take precedence over the ones in the {@link DefaultConverterChain}.
 *
 * <p>
 * No converters are included in the {@link RequestItem.Builder} or {@link DynamoDbEnhancedClient.Builder} by default, so
 * converters you configure are always used over the ones that ship with the SDK.
 *
 * <p>
 * <i>Converter Precedence - The converter's {@link ConversionCondition}</i>
 *
 * <p>
 * If multiple converters are configured at the same location (either a {@link RequestItem} or a {@link DynamoDbEnhancedClient}),
 * their relative precedence is determined by the converter's {@link #defaultConversionCondition()}.
 *
 * <p>
 * Converters can have one of two conditions:
 * <ol>
 *     <li>An {@code ExactInstanceOf} converter converts exactly one type of {@link Class}, and will not convert any other Java
 *     types.</li>
 *     <li>A {@code InstanceOf} converter converts a type of {@link Class}, and all of its subtypes.</li>
 * </ol>
 *
 * <p>
 * An {@code ExactInstanceOf} converter will always be used before a {@code InstanceOf} converter, if the Java type exactly
 * matches the requested type. For example, a {@code ConversionCondition.isExactInstanceOf(Integer.class)} converter will
 * always be used before a {@code ConversionCondition.isInstanceOf(Number.class)} converter, if the Java type is an Integer.
 *
 * <p>
 * <i>Converter Precedence - The order in which they were configured.</i>
 *
 * <p>
 * If multiple converters are configured at the same location (either a {@link RequestItem} or a {@link DynamoDbEnhancedClient})
 * AND they have the same {@link #defaultConversionCondition()}, their precedence is determined by the order in which they were
 * configured.
 *
 * <p>
 * Converters added first always have a higher precedence than those added later. For example, the first
 * {@code ConversionCondition.isExactInstanceOf(String.class)} converter added to a {@link RequestItem.Builder} will have higher
 * precedence than all other {@code ConversionCondition.isExactInstanceOf(String.class)} converters added later on.
 *
 * <p>
 * Most importantly, <b>type does not matter for {@code InstanceOf} converters</b>. For example,
 * {@code ConversionCondition.isInstanceOf(Object.class)} converter will take precedence over a
 * {@code ConversionCondition.isInstanceOf(String.class)} converter added later, assuming they are both configured at the
 * same level ({@link RequestItem} or {@link DynamoDbEnhancedClient}). Indeed, a
 * {@code ConversionCondition.isInstanceOf(Object.class)} configured first at a specific level will result in no other
 * {@code InstanceOf} converters configured at the same level being used, because all Java types are an instance of Object.
 */
@SdkPublicApi
@ThreadSafe
public interface ItemAttributeValueConverter {
    /**
     * The default condition under which this converter will be used. The converter can still be invoked directly, regardless
     * of this condition, but an exception may be thrown.
     *
     * <p>
     * This call should never fail with an {@link Exception}.
     */
    ConversionCondition defaultConversionCondition();

    /**
     * Convert the provided Java object into a {@link ItemAttributeValue} that can be persisted in DynamoDB.
     *
     * <p>
     * This input object was usually specified as part of a request, such as in
     * {@link RequestItem.Builder#putAttribute(String, Object)}.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided value cannot be converted by this converter.</li>
     *     <li>If the context is null.</li>
     * </ol>
     */
    ItemAttributeValue toAttributeValue(Object input, ConversionContext context);

    /**
     * Convert the provided {@link ItemAttributeValue} into a Java object that can be used by an application.
     *
     * <p>
     * This input value is usually retrieved from DynamoDB, such as in
     * {@link GeneratedResponseItem.Builder#putAttribute(String, AttributeValue)}.
     *
     * <p>
     * The desired type is usually specified by the customer, such as in {@link ConvertableItemAttributeValue#as(Class)}.
     *
     * <p>
     * Reasons this call may fail with a {@link RuntimeException}:
     * <ol>
     *     <li>If the provided item attribute value cannot be converted by this converter.</li>
     *     <li>If the desired type is not supported by this converter.</li>
     *     <li>If the desired type is null.</li>
     *     <li>If the context is null.</li>
     * </ol>
     */
    Object fromAttributeValue(ItemAttributeValue input, TypeToken<?> desiredType, ConversionContext context);
}
