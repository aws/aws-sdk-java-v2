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

package software.amazon.awssdk.codegen.poet.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.squareup.javapoet.ClassName;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.client.SyncClientInterface;

/**
 * Validate functionality of {@link PoetUtils} methods.
 */
public class PoetUtilsTest {

    @Test
    public void findExtensionInterface_whenNotExists_returnEmpty() {
        ClassName cl = ClassName.get("software.amazon.awssdk.codegen.poet.utils", "NoClient");
        Optional<Class<?>> extensionClass = PoetUtils.findExtensionInterface(cl, SyncClientInterface.class);
        assertThat(extensionClass).isEmpty();
    }

    @Test
    public void findExtensionInterface_whenExists_returnClass() {
        ClassName cl = ClassName.get("software.amazon.awssdk.codegen.poet.utils","CorrectClient");
        Optional<Class<?>> extensionClass = PoetUtils.findExtensionInterface(cl, getClass());
        assertThat(extensionClass).isPresent();
        assertThat(extensionClass.get().getSimpleName()).isEqualTo("CorrectClientExtensionMethods");
    }

    @Test
    public void findExtensionInterface_whenIncorrectType_throwException() {
        ClassName cl = ClassName.get("software.amazon.awssdk.codegen.poet.utils","IncorrectClient");
        assertThatThrownBy(() -> PoetUtils.findExtensionInterface(cl, getClass()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("interface");
    }
}
