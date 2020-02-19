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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.beanmapper;

import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TypeTokenTest {
    @Test
    public void anonymousCreationCapturesComplexTypeArguments() {
        TypeToken<Map<String, List<List<String>>>> typeToken = new TypeToken<Map<String, List<List<String>>>>(){};
        assertThat(typeToken.rawClass()).isEqualTo(Map.class);
        assertThat(typeToken.rawClassParameters().get(0).rawClass()).isEqualTo(String.class);
        assertThat(typeToken.rawClassParameters().get(1).rawClass()).isEqualTo(List.class);
        assertThat(typeToken.rawClassParameters().get(1).rawClassParameters().get(0).rawClass()).isEqualTo(List.class);
        assertThat(typeToken.rawClassParameters().get(1).rawClassParameters().get(0).rawClassParameters().get(0).rawClass())
            .isEqualTo(String.class);
    }

    @Test
    public void customTypesWork() {
        TypeToken<TypeTokenTest> typeToken = new TypeToken<TypeTokenTest>(){};
        assertThat(typeToken.rawClass()).isEqualTo(TypeTokenTest.class);
    }

    @Test
    public void nonStaticInnerTypesWork() {
        TypeToken<InnerType> typeToken = new TypeToken<InnerType>(){};
        assertThat(typeToken.rawClass()).isEqualTo(InnerType.class);
    }

    @Test
    public void staticInnerTypesWork() {
        TypeToken<InnerStaticType> typeToken = new TypeToken<InnerStaticType>(){};
        assertThat(typeToken.rawClass()).isEqualTo(InnerStaticType.class);
    }

    @Test
    public <T> void genericParameterTypesDontWork() {
        assertThatThrownBy(() -> new TypeToken<List<T>>(){}).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void helperCreationMethodsWork() {
        assertThat(TypeToken.of(String.class).rawClass()).isEqualTo(String.class);

        assertThat(TypeToken.listOf(String.class)).satisfies(v -> {
            assertThat(v.rawClass()).isEqualTo(List.class);
            assertThat(v.rawClassParameters()).hasSize(1);
            assertThat(v.rawClassParameters().get(0).rawClass()).isEqualTo(String.class);
        });

        assertThat(TypeToken.mapOf(String.class, Integer.class)).satisfies(v -> {
            assertThat(v.rawClass()).isEqualTo(Map.class);
            assertThat(v.rawClassParameters()).hasSize(2);
            assertThat(v.rawClassParameters().get(0).rawClass()).isEqualTo(String.class);
            assertThat(v.rawClassParameters().get(1).rawClass()).isEqualTo(Integer.class);
        });
    }

    @Test
    public void equalityIsBasedOnInnerEquality() {
        assertThat(TypeToken.of(String.class)).isEqualTo(TypeToken.of(String.class));
        assertThat(TypeToken.of(String.class)).isNotEqualTo(TypeToken.of(Integer.class));

        assertThat(new TypeToken<Map<String, List<String>>>(){}).isEqualTo(new TypeToken<Map<String, List<String>>>(){});
        assertThat(new TypeToken<Map<String, List<String>>>(){}).isNotEqualTo(new TypeToken<Map<String, List<Integer>>>(){});
    }

    public class InnerType {
    }

    public static class InnerStaticType {
    }
}