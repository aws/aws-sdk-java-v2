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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;

import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ChainAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ConversionContext;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.SubtypeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.TypeToken;

public class ChainAttributeConverterTest {
    @Test
    public void convertersAreHighestPriority() {
        AttributeConverter<?> converter = converter(String.class);
        SubtypeAttributeConverter<?> subtypeConverter = subtypeConverter(String.class);

        ChainAttributeConverter<Object> chain =
                ChainAttributeConverter.builder()
                                       .addConverter(converter)
                                       .addSubtypeConverter(subtypeConverter)
                                       .build();

        chain.toAttributeValue("", ConversionContext.builder().attributeConverter(chain).build());

        Mockito.verify(converter).toAttributeValue(any(), any(ConversionContext.class));
        Mockito.verify(subtypeConverter, never()).toAttributeValue(any(), any(ConversionContext.class));
    }

    @Test
    public void subtypeConvertersWorkForSubtypes() {
        SubtypeAttributeConverter<?> subtypeConverter = subtypeConverter(CharSequence.class);

        ChainAttributeConverter<Object> chain =
                ChainAttributeConverter.builder()
                                       .addSubtypeConverter(subtypeConverter)
                                       .build();

        chain.toAttributeValue("", ConversionContext.builder().attributeConverter(chain).build());

        Mockito.verify(subtypeConverter).toAttributeValue(any(), any(ConversionContext.class));
    }

    @Test
    public void matchingConvertersFavorFirstAdded() {
        AttributeConverter<?> instanceConverter = converter(String.class);
        AttributeConverter<?> instanceConverter2 = converter(String.class);

        ChainAttributeConverter<Object> chain =
                ChainAttributeConverter.builder()
                                       .addConverter(instanceConverter)
                                       .addConverter(instanceConverter2)
                                       .build();

        chain.toAttributeValue("", ConversionContext.builder().attributeConverter(chain).build());

        Mockito.verify(instanceConverter).toAttributeValue(any(), any(ConversionContext.class));
        Mockito.verify(instanceConverter2, never()).toAttributeValue(any(), any(ConversionContext.class));
    }

    @Test
    public void matchingSubtypeConvertersFavorFirstAdded() {
        SubtypeAttributeConverter<?> subtypeConverter = subtypeConverter(CharSequence.class);
        SubtypeAttributeConverter<?> subtypeConverter2 = subtypeConverter(CharSequence.class);

        ChainAttributeConverter<Object> chain =
                ChainAttributeConverter.builder()
                                       .addSubtypeConverter(subtypeConverter)
                                       .addSubtypeConverter(subtypeConverter2)
                                       .build();

        chain.toAttributeValue("", ConversionContext.builder().attributeConverter(chain).build());

        Mockito.verify(subtypeConverter).toAttributeValue(any(), any(ConversionContext.class));
        Mockito.verify(subtypeConverter, never()).toAttributeValue(any(), any(ConversionContext.class));
    }

    @Test
    public void noMatchingConverterDelegatesToParent() {
        AttributeConverter<?> converter = converter(String.class);
        SubtypeAttributeConverter<?> subtypeConverter = subtypeConverter(String.class);
        SubtypeAttributeConverter<Object> parent = subtypeConverter(Object.class);

        ChainAttributeConverter<Object> chain =
                ChainAttributeConverter.builder()
                                       .addConverter(converter)
                                       .addSubtypeConverter(subtypeConverter)
                                       .parent(parent)
                                       .build();

        chain.toAttributeValue(1, ConversionContext.builder().attributeConverter(chain).build());

        Mockito.verify(parent).toAttributeValue(any(), any(ConversionContext.class));
        Mockito.verify(converter, never()).toAttributeValue(any(), any(ConversionContext.class));
        Mockito.verify(subtypeConverter, never()).toAttributeValue(any(), any(ConversionContext.class));
    }

    @Test
    public void noMatchingConverterWithNullParentFails() {
        AttributeConverter<?> exactInstanceConverter = converter(String.class);
        SubtypeAttributeConverter<?> instanceConverter = subtypeConverter(String.class);

        ChainAttributeConverter<Object> chain =
                ChainAttributeConverter.builder()
                                       .addConverter(exactInstanceConverter)
                                       .addSubtypeConverter(instanceConverter)
                                       .build();

        assertThatThrownBy(() -> chain.toAttributeValue(1, ConversionContext.builder().attributeConverter(chain).build()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void nestedChainsDoNotRewrap() {
        ChainAttributeConverter<Object> chain = ChainAttributeConverter.builder().build();
        ChainAttributeConverter<Object> chain2 = ChainAttributeConverter.builder().addSubtypeConverter(chain).build();
        assertThat(chain == chain2).isTrue();
    }

    private <T> AttributeConverter<?> converter(Class<T> type) {
        AttributeConverter<T> converter = Mockito.mock(AttributeConverter.class);
        Mockito.when(converter.type()).thenReturn(TypeToken.of(type));
        return converter;
    }

    private <T> SubtypeAttributeConverter<T> subtypeConverter(Class<T> type) {
        SubtypeAttributeConverter<T> converter = Mockito.mock(SubtypeAttributeConverter.class);
        Mockito.when(converter.type()).thenReturn(TypeToken.of(type));
        return converter;
    }
}