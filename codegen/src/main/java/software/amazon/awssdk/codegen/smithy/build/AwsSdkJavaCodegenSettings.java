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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Settings for the {@code aws-sdk-java-v2-codegen} smithy-build plugin, parsed from the plugin's configuration
 * object in {@code smithy-build.json}.
 *
 * <p>Example:
 * <pre>
 * {
 *     "version": "1.0",
 *     "sources": ["src/main/resources/codegen-resources/model.json"],
 *     "plugins": {
 *         "aws-sdk-java-v2-codegen": {
 *             "service": "com.amazonaws.account#Account",
 *             "customizationConfig": "src/main/resources/codegen-resources/customization.config"
 *         }
 *     }
 * }
 * </pre>
 */
@SdkInternalApi
public final class AwsSdkJavaCodegenSettings {
    static final String SERVICE = "service";
    static final String CUSTOMIZATION_CONFIG = "customizationConfig";
    static final String BASE_DIR = "baseDir";
    static final String WRITE_INTERMEDIATE_MODEL = "writeIntermediateModel";

    private final ShapeId service;
    private final Path customizationConfig;
    private final boolean writeIntermediateModel;

    private AwsSdkJavaCodegenSettings(ShapeId service, Path customizationConfig, boolean writeIntermediateModel) {
        this.service = service;
        this.customizationConfig = customizationConfig;
        this.writeIntermediateModel = writeIntermediateModel;
    }

    /**
     * Parses plugin settings, rejecting unknown keys so that typos surface as build failures rather than being
     * silently ignored.
     *
     * <p>Relative paths are resolved against the optional {@code baseDir} setting. Build tool integrations are
     * expected to set {@code baseDir} to the directory containing {@code smithy-build.json}; when it is absent,
     * relative paths resolve against the current working directory, which is the Smithy CLI's behavior.
     */
    public static AwsSdkJavaCodegenSettings fromNode(ObjectNode node) {
        node.warnIfAdditionalProperties(
            java.util.Arrays.asList(SERVICE, CUSTOMIZATION_CONFIG, BASE_DIR, WRITE_INTERMEDIATE_MODEL));

        ShapeId service = node.getStringMember(SERVICE)
                              .map(s -> ShapeId.from(s.getValue()))
                              .orElse(null);

        Path baseDir = node.getStringMember(BASE_DIR)
                           .map(s -> Paths.get(s.getValue()))
                           .orElseGet(() -> Paths.get(""));

        Path customizationConfig = node.getStringMember(CUSTOMIZATION_CONFIG)
                                       .map(s -> baseDir.resolve(s.getValue()))
                                       .orElse(null);

        boolean writeIntermediateModel = node.getBooleanMemberOrDefault(WRITE_INTERMEDIATE_MODEL, false);

        return new AwsSdkJavaCodegenSettings(service, customizationConfig, writeIntermediateModel);
    }

    /**
     * The service to generate, or {@code null} to infer it from the model. Inference only succeeds when the model
     * contains exactly one service shape.
     */
    public ShapeId service() {
        return service;
    }

    /**
     * Location of the SDK {@code customization.config} to apply, or {@code null} if the service has no
     * customizations.
     */
    public Path customizationConfig() {
        return customizationConfig;
    }

    public boolean writeIntermediateModel() {
        return writeIntermediateModel;
    }
}
