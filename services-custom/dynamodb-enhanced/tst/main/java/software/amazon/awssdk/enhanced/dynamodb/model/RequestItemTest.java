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

public class RequestItemTest {
    @Test
    public void allConfigurationMethodsWork() {
        ItemAttributeValueConverter converter = new StringConverter();
        ItemAttributeValueConverter converter2 = new IntegerConverter();

        RequestItem item = RequestItem.builder()
                                      .putAttribute("toremove", "toremove")
                                      .clearAttributes()
                                      .putAttribute("toremove2", "toremove")
                                      .removeAttribute("toremove2")
                                      .putAttributes(Collections.singletonMap("foo", "bar"))
                                      .putAttribute("foo2", "bar2")
                                      .putAttribute("foo3", i -> i.putAttribute("foo", "bar"))
                                      .addConverter(converter)
                                      .clearConverters()
                                      .addConverters(Collections.singletonList(converter))
                                      .addConverter(converter2)
                                      .build();

        assertThat(item.attributes()).hasSize(3);
        assertThat(item.attribute("foo")).isEqualTo("bar");
        assertThat(item.attribute("foo2")).isEqualTo("bar2");
        assertThat(item.attribute("foo3")).isEqualTo(RequestItem.builder().putAttribute("foo", "bar").build());

        assertThat(item.converters()).hasSize(2);
        assertThat(item.converters().get(0)).isEqualTo(converter);
        assertThat(item.converters().get(1)).isEqualTo(converter2);
    }

    @Test
    public void toGeneratedRequestItemCallsConfiguredConverters() {
        ItemAttributeValue attributeValue =
                ItemAttributeValue.fromMap(Collections.singletonMap("foo", ItemAttributeValue.fromString("bar")));

        ItemAttributeValueConverter converter = Mockito.mock(ItemAttributeValueConverter.class);
        Mockito.when(converter.defaultConversionCondition()).thenReturn(ConversionCondition.isInstanceOf(Object.class));
        Mockito.when(converter.toAttributeValue(anyObject(), anyObject())).thenReturn(attributeValue);

        GeneratedRequestItem item = RequestItem.builder()
                                               .addConverter(converter)
                                               .build()
                                               .toGeneratedRequestItem();

        assertThat(item.attribute("foo")).isEqualTo(AttributeValue.builder().s("bar").build());
    }
}