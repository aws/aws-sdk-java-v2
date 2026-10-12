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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

public class BeanAttributeGetterTest {

    @Test
    public void create_validGetter_succeeds() throws Exception {
        Method getter = ValidBean.class.getDeclaredMethod("getValue");

        BeanAttributeGetter<ValidBean, String> attributeGetter = BeanAttributeGetter.create(
            ValidBean.class, getter, MethodHandles.lookup());

        ValidBean bean = new ValidBean();
        assertThat(attributeGetter.apply(bean)).isEqualTo("test");
    }

    @Test
    public void create_getterWithParameters_throwsException() throws Exception {
        Method getter = InvalidBean.class.getDeclaredMethod("getValue", String.class);

        assertThatThrownBy(() -> BeanAttributeGetter.create(InvalidBean.class, getter, MethodHandles.lookup()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has parameters, despite being named like a getter");
    }

    public static class ValidBean {
        public String getValue() {
            return "test";
        }
    }

    public static class InvalidBean {
        public String getValue(String param) {
            return param;
        }
    }
}