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

import java.util.Collections;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class GeneratedRequestItemTest {
    @Test
    public void allConfigurationMethodsWork() {
        AttributeValue attributeValue = AttributeValue.builder().s("a").build();
        AttributeValue attributeValue2 = AttributeValue.builder().s("b").build();

        GeneratedRequestItem item = GeneratedRequestItem.builder()
                                                        .putAttribute("toremove", attributeValue)
                                                        .clearAttributes()
                                                        .putAttribute("toremove2", attributeValue)
                                                        .removeAttribute("toremove2")
                                                        .putAttributes(Collections.singletonMap("foo", attributeValue))
                                                        .putAttribute("foo2", attributeValue2)
                                                        .build();

        assertThat(item.attributes()).hasSize(2);
        assertThat(item.attribute("foo")).isEqualTo(attributeValue);
        assertThat(item.attribute("foo2")).isEqualTo(attributeValue2);
    }
}