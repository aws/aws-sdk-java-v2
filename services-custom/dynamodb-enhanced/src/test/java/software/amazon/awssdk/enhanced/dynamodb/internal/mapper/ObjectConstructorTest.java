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
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;

public class ObjectConstructorTest {

    @Test
    public void create_validNoArgsConstructor_succeeds() throws Exception {
        Constructor<ValidBean> constructor = ValidBean.class.getDeclaredConstructor();

        ObjectConstructor<ValidBean> objectConstructor = ObjectConstructor.create(
            ValidBean.class, constructor, MethodHandles.lookup());

        assertThat(objectConstructor.get()).isInstanceOf(ValidBean.class);
    }

    @Test
    public void create_constructorWithParameters_throwsException() throws Exception {
        Constructor<InvalidBean> constructor = InvalidBean.class.getDeclaredConstructor(String.class);

        assertThatThrownBy(() -> ObjectConstructor.create(InvalidBean.class, constructor, MethodHandles.lookup()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has no default constructor");
    }

    public static class ValidBean {
        public ValidBean() {
        }
    }

    public static class InvalidBean {
        public InvalidBean(String param) {
        }
    }
}