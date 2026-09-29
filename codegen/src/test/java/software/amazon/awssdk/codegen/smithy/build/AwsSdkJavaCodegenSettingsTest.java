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

package software.amazon.awssdk.codegen.smithy.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

class AwsSdkJavaCodegenSettingsTest {

    @Test
    void emptySettings_hasNoCustomizationConfig() {
        AwsSdkJavaCodegenSettings settings = AwsSdkJavaCodegenSettings.fromNode(Node.objectNode());

        assertThat(settings.customizationConfig()).isNull();
    }

    @Test
    void customizationConfig_withoutBaseDir_remainsRelative() {
        AwsSdkJavaCodegenSettings settings = AwsSdkJavaCodegenSettings.fromNode(settings(
            AwsSdkJavaCodegenSettings.CUSTOMIZATION_CONFIG, "config/customization.config"));

        assertThat(settings.customizationConfig()).isEqualTo(Paths.get("config/customization.config"));
    }

    @Test
    void customizationConfig_withBaseDir_resolvesRelativePath() {
        Path baseDir = Paths.get("/service/module");
        AwsSdkJavaCodegenSettings settings = AwsSdkJavaCodegenSettings.fromNode(settings(
            AwsSdkJavaCodegenSettings.BASE_DIR, baseDir.toString(),
            AwsSdkJavaCodegenSettings.CUSTOMIZATION_CONFIG, "config/customization.config"));

        assertThat(settings.customizationConfig()).isEqualTo(baseDir.resolve("config/customization.config"));
    }

    @Test
    void unknownSetting_fails() {
        assertThatThrownBy(() -> AwsSdkJavaCodegenSettings.fromNode(settings("unknown", "value")))
            .isInstanceOf(ExpectationNotMetException.class)
            .hasMessageContaining("unknown");
    }

    private static ObjectNode settings(String key, String value) {
        return Node.objectNodeBuilder()
                   .withMember(key, value)
                   .build();
    }

    private static ObjectNode settings(String firstKey, String firstValue, String secondKey, String secondValue) {
        return Node.objectNodeBuilder()
                   .withMember(firstKey, firstValue)
                   .withMember(secondKey, secondValue)
                   .build();
    }
}
