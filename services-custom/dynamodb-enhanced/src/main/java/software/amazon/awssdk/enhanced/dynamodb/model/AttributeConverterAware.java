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

import java.util.Collection;
import java.util.List;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.DefaultAttributeConverter;

/**
 * An interface applied to all objects that wish to expose their underlying {@link AttributeConverter}s.
 *
 * <p>
 * <b>Default Converters</b>
 *
 * Most built-in Java types have some defined conversion behavior by default. The exhaustive list of default-supported classes
 * can be found in the {@link DefaultAttributeConverter}.
 *
 * <b>Converter Precedence</b>
 *
 * When converting from Java types, configured converters have a precedence, based on:
 * <ol>
 *     <li>Where they were configured.</li>
 *     <li>Whether they are a {@link AttributeConverter} or a {@link SubtypeAttributeConverter}.</li>
 *     <li>The order in which they were configured.</li>
 * </ol>
 *
 * <i>Converter Precedence - Where they were configured</i>
 *
 * Converters are defined in three places:
 * <ol>
 *     <li>{@link RequestItem}-level converters are those added with the {@link RequestItem.Builder}.</li>
 *     <li>{@link DynamoDbEnhancedClient}-level converters are those added with the {@link DynamoDbEnhancedClient.Builder}.</li>
 *     <li>Default-level converters are those included by default with the client ({@link DefaultAttributeConverter}).</li>
 * </ol>
 *
 * Converters configured in a {@link RequestItem.Builder} always take precedence over the ones configured with
 * {@link DynamoDbEnhancedClient.Builder} and in the {@link DefaultAttributeConverter}. Converters configured in a
 * {@link DynamoDbEnhancedClient.Builder} always take precedence over the ones in the {@link DefaultAttributeConverter}.
 *
 * No converters are included in the {@link RequestItem.Builder} or {@link DynamoDbEnhancedClient.Builder} by default, so
 * converters you configure are always used over the ones that ship with the SDK.
 *
 * <i>Converter Precedence - {@link AttributeConverter} or {@link SubtypeAttributeConverter}s</i>
 *
 * If multiple converters are configured at the same location (either a {@link RequestItem} or a {@link DynamoDbEnhancedClient}),
 * their relative precedence is determined by whether they are a {@link AttributeConverter} or {@link SubtypeAttributeConverter}.
 *
 * An {@code AttributeConverter} converter (that can convert exactly one type) will always be used before a
 * {@code SubtypeAttributeConverter} converter (that can convert a type and its subclasses), assuming the Java type exactly
 * matches the requested type. For example, a {@code AttributeConverter<Integer>} converter will always be used before a
 * {@code SubtypeAttributeConverter<Number>} or {@code SubtypeAttributeConverter<Integer>} converter, if the Java type is an
 * Integer.
 *
 * <i>Converter Precedence - The order in which they were configured.</i>
 *
 * If multiple converters are configured at the same location (either a {@link RequestItem} or a {@link DynamoDbEnhancedClient})
 * AND they have the same type, their precedence is determined by the order in which they were configured.
 *
 * Converters added last always have a higher precedence than those added first. For example, the last
 * {@code AttributeConverter<String>)} converter added to a {@link RequestItem.Builder} will have higher
 * precedence than any other {@code AttributeConverter<String>} converters that were added before it.
 *
 * Most importantly, <b>type does not matter for {@code SubtypeAttributeConverter}s</b>. For example,
 * a {@code SubtypeAttributeConverter<Object>} converter will take precedence over a
 * {@code SubtypeAttributeConverter<String>)} converter added before it, assuming they are both configured at the
 * same level ({@link RequestItem} or {@link DynamoDbEnhancedClient}). Indeed, a
 * {@code SubtypeAttributeConverter<Object>} configured last at a specific level will result in no other
 * {@code SubtypeAttributeConverter}s configured at the same level being used, because all Java types are an subtype of Object.
 */
@SdkPublicApi
@ThreadSafe
@Immutable
public interface AttributeConverterAware<T> {
    /**
     * Retrieve all converters that were directly configured on this object.
     *
     * <p>
     * This call should never fail with an {@link Exception}.
     */
    List<AttributeConverter<? extends T>> converters();

    /**
     * Retrieve all subtype converters that were directly configured on this object.
     *
     * <p>
     * This call should never fail with an {@link Exception}.
     */
    List<SubtypeAttributeConverter<? extends T>> subtypeConverters();

    /**
     * An interface applied to all objects that can be configured with {@link AttributeConverter}s.
     *
     * <p>
     * This call should never fail with an {@link Exception}.
     */
    @NotThreadSafe
    interface Builder<T> {
        /**
         * Add all of the provided converters to this builder, in the order of the provided collection.
         *
         * <p>
         * Converters later in the provided list take precedence over the ones earlier in the list.
         *
         * <p>
         * Converters configured with {@link #addConverter(AttributeConverter)} always take precedence over the ones configured
         * with {@link #addSubtypeConverter(SubtypeAttributeConverter)}.
         *
         * <p>
         * Converters configured in {@link RequestItem.Builder} always take precedence over the ones configured in
         * {@link DynamoDbEnhancedClient.Builder}.
         *
         * <p>
         * Converters configured in {@link DynamoDbEnhancedClient.Builder} always take precedence over the ones provided by the
         * {@link DefaultAttributeConverter}.
         *
         * <p>
         * Reasons this call may fail with a {@link RuntimeException}:
         * <ol>
         *     <li>The provided converter collection or one of its members is null.</li>
         *     <li>If this or any other {@code converter}-modifying method is called in parallel with this one.
         *     This method is not thread safe.</li>
         * </ol>
         *
         * @see AttributeConverter
         */
        Builder<T> addConverters(Collection<? extends AttributeConverter<? extends T>> converters);

        /**
         * Add a converter to this builder.
         *
         * <p>
         * Converters added later take precedence over the ones added earlier.
         *
         * <p>
         * Converters configured with {@link #addConverter(AttributeConverter)} always take precedence over the ones configured
         * with {@link #addSubtypeConverter(SubtypeAttributeConverter)}.
         *
         * <p>
         * Converters configured in {@link RequestItem.Builder} always take precedence over the ones configured in
         * {@link DynamoDbEnhancedClient.Builder}.
         *
         * <p>
         * Converters configured in {@link DynamoDbEnhancedClient.Builder} always take precedence over the ones provided by the
         * {@link DefaultAttributeConverter}.
         *
         * <p>
         * Reasons this call may fail with a {@link RuntimeException}:
         * <ol>
         *     <li>The provided converter is null.</li>
         *     <li>If this or any other {@code converter}-modifying method is called in parallel from multiple threads.
         *     This method is not thread safe.</li>
         * </ol>
         */
        Builder<T> addConverter(AttributeConverter<? extends T> converter);

        /**
         * Add all of the provided subtype converters to this builder, in the order of the provided collection.
         *
         * <p>
         * Converters later in the provided list take precedence over the ones earlier in the list, even if the earlier ones
         * refer to a more specific type. Converters should be added in an order from least-specific to most-specific.
         *
         * <p>
         * Converters configured with {@link #addConverter(AttributeConverter)} always take precedence over the ones configured
         * with {@link #addSubtypeConverter(SubtypeAttributeConverter)}.
         *
         * <p>
         * Converters configured in {@link RequestItem.Builder} always take precedence over the ones configured in
         * {@link DynamoDbEnhancedClient.Builder}.
         *
         * <p>
         * Converters configured in {@link DynamoDbEnhancedClient.Builder} always take precedence over the ones provided by the
         * {@link DefaultAttributeConverter}.
         *
         * <p>
         * Reasons this call may fail with a {@link RuntimeException}:
         * <ol>
         *     <li>The provided converter collection or one of its members is null.</li>
         *     <li>If this or any other {@code converter}-modifying method is called in parallel with this one.
         *     This method is not thread safe.</li>
         * </ol>
         *
         * @see SubtypeAttributeConverter
         */
        Builder<T> addSubtypeConverters(Collection<? extends SubtypeAttributeConverter<? extends T>> converters);


        /**
         * Add the provided subtype converter to this builder.
         *
         * <p>
         * Converters added later take precedence over the ones added earlier, even if the earlier ones refer to
         * a more specific type. Converters should be added in an order from least-specific to most-specific.
         *
         * <p>
         * Converters configured with {@link #addConverter(AttributeConverter)} always take precedence over the ones configured
         * with {@link #addSubtypeConverter(SubtypeAttributeConverter)}.
         *
         * <p>
         * Converters configured in {@link RequestItem.Builder} always take precedence over the ones configured in
         * {@link DynamoDbEnhancedClient.Builder}.
         *
         * <p>
         * Converters configured in {@link DynamoDbEnhancedClient.Builder} always take precedence over the ones provided by the
         * {@link DefaultAttributeConverter}.
         *
         * <p>
         * Reasons this call may fail with a {@link RuntimeException}:
         * <ol>
         *     <li>The provided converter collection or one of its members is null.</li>
         *     <li>If this or any other {@code converter}-modifying method is called in parallel with this one.
         *     This method is not thread safe.</li>
         * </ol>
         *
         * @see SubtypeAttributeConverter
         */
        Builder<T> addSubtypeConverter(SubtypeAttributeConverter<? extends T> converter);

        /**
         * Reset the converters that were previously added with {@link #addConverters(Collection)} or
         * {@link #addConverter(AttributeConverter)}.
         *
         * <p>
         * This <b>does not</b> reset converters configured elsewhere. Converters configured in other locations, such as in the
         * {@link DefaultAttributeConverter}, will still be used.
         *
         * <p>
         * Reasons this call may fail with a {@link RuntimeException}:
         * <ol>
         *     <li>If this or any other {@code converter}-modifying method is called in parallel from multiple threads.
         *     This method is not thread safe.</li>
         * </ol>
         */
        Builder<T> clearConverters();

        /**
         * Reset the converters that were previously added with {@link #addSubtypeConverters(Collection)} or
         * {@link #addSubtypeConverter(SubtypeAttributeConverter)}.
         *
         * <p>
         * This <b>does not</b> reset converters configured elsewhere. Converters configured in other locations, such as in the
         * {@link DefaultAttributeConverter}, will still be used.
         *
         * <p>
         * Reasons this call may fail with a {@link RuntimeException}:
         * <ol>
         *     <li>If this or any other {@code converter}-modifying method is called in parallel from multiple threads.
         *     This method is not thread safe.</li>
         * </ol>
         */
        Builder<T> clearSubtypeConverters();
    }
}
