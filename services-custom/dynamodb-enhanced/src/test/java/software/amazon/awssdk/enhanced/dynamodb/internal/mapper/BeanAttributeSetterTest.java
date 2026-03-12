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

public class BeanAttributeSetterTest {

    @Test
    public void create_validSetter_succeeds() throws Exception {
        Method setter = ValidBean.class.getDeclaredMethod("setValue", String.class);

        BeanAttributeSetter<ValidBean, String> attributeSetter = BeanAttributeSetter.create(
            ValidBean.class, setter, MethodHandles.lookup());

        ValidBean bean = new ValidBean();
        attributeSetter.accept(bean, "newValue");
        assertThat(bean.getValue()).isEqualTo("newValue");
    }

    @Test
    public void create_setterWithNoParameters_throwsException() throws Exception {
        Method setter = InvalidBean.class.getDeclaredMethod("setValue");

        assertThatThrownBy(() -> BeanAttributeSetter.create(InvalidBean.class, setter, MethodHandles.lookup()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("doesn't have just 1 parameter, despite being named like a setter");
    }

    public static class ValidBean {
        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class InvalidBean {
        public void setValue() {
        }
    }
}