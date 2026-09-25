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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Settings for the {@value AwsSdkJavaCodegenPlugin#NAME} plugin.
 */
@SdkInternalApi
public final class AwsSdkJavaCodegenSettings {

    static final String CUSTOMIZATION_CONFIG = "customizationConfig";
    static final String BASE_DIR = "baseDir";

    private final Path customizationConfig;

    private AwsSdkJavaCodegenSettings(Path customizationConfig) {
        this.customizationConfig = customizationConfig;
    }

    /**
     * Creates settings from plugin configuration.
     */
    public static AwsSdkJavaCodegenSettings fromNode(ObjectNode node) {
        node.expectNoAdditionalProperties(Arrays.asList(CUSTOMIZATION_CONFIG, BASE_DIR));

        Path baseDir = node.getStringMember(BASE_DIR)
                           .map(s -> Paths.get(s.getValue()))
                           .orElseGet(() -> Paths.get(""));

        Path customizationConfig = node.getStringMember(CUSTOMIZATION_CONFIG)
                                       .map(s -> baseDir.resolve(s.getValue()))
                                       .orElse(null);

        return new AwsSdkJavaCodegenSettings(customizationConfig);
    }

    /**
     * Configured customization file, if any.
     */
    public Path customizationConfig() {
        return customizationConfig;
    }
}
