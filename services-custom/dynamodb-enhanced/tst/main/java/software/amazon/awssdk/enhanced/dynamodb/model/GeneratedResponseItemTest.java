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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;

import java.util.Collections;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionCondition;
import software.amazon.awssdk.enhanced.dynamodb.converter.ItemAttributeValueConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.IntegerConverter;
import software.amazon.awssdk.enhanced.dynamodb.converter.bundled.StringConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class GeneratedResponseItemTest {
    @Test
    public void allConfigurationMethodsWork() {
        AttributeValue attributeValue = AttributeValue.builder().s("a").build();
        AttributeValue attributeValue2 = AttributeValue.builder().s("b").build();

        ItemAttributeValueConverter converter = new StringConverter();
        ItemAttributeValueConverter converter2 = new IntegerConverter();

        GeneratedResponseItem item = GeneratedResponseItem.builder()
                                                          .putAttribute("toremove", attributeValue)
                                                          .clearAttributes()
                                                          .putAttribute("toremove2", attributeValue)
                                                          .removeAttribute("toremove2")
                                                          .putAttributes(Collections.singletonMap("foo", attributeValue))
                                                          .putAttribute("foo2", attributeValue2)
                                                          .addConverter(converter)
                                                          .clearConverters()
                                                          .addConverters(Collections.singletonList(converter))
                                                          .addConverter(converter2)
                                                          .build();

        assertThat(item.attributes()).hasSize(2);
        assertThat(item.attribute("foo")).isEqualTo(attributeValue);
        assertThat(item.attribute("foo2")).isEqualTo(attributeValue2);

        assertThat(item.converters()).hasSize(2);
        assertThat(item.converters().get(0)).isEqualTo(converter);
        assertThat(item.converters().get(1)).isEqualTo(converter2);
    }

    @Test
    public void toResponseItemCallsConfiguredConverters() {
        ItemAttributeValueConverter converter = Mockito.mock(ItemAttributeValueConverter.class);
        Mockito.when(converter.defaultConversionCondition()).thenReturn(ConversionCondition.isInstanceOf(Object.class));
        Mockito.when(converter.fromAttributeValue(anyObject(), anyObject(), anyObject()))
               .thenReturn(ResponseItem.builder().putAttribute("foo", null).build());

        ResponseItem item = GeneratedResponseItem.builder()
                                                 .putAttribute("foo", AttributeValue.builder().s("bar").build())
                                                 .addConverter(converter)
                                                 .build()
                                                 .toResponseItem();

        assertThat(item.attribute("foo")).isEqualTo(null);
    }
}