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

package software.amazon.awssdk.codegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import software.amazon.smithy.model.Model;

public class SmithyModelWithCustomizations {
    private final Model smithyModel;
    private final CustomizationConfig customizationConfig;

    private SmithyModelWithCustomizations(Builder builder) {
        this.smithyModel = builder.smithyModel;
        this.customizationConfig = builder.customizationConfig == null ?
                                   CustomizationConfig.create() : builder.customizationConfig;
        Validate.notNull(this.smithyModel, "Smithy Model is required");
    }

    public static Builder builder() {
        return new Builder();
    }

    public Model getSmithyModel() {
        return smithyModel;
    }

    public CustomizationConfig getCustomizationConfig() {
        return customizationConfig;
    }

    public static class Builder implements SdkBuilder<Builder, SmithyModelWithCustomizations> {
        private Model smithyModel;
        private Path modelLocation;
        private CustomizationConfig customizationConfig;

        public Builder smithyModel(Model smithyModel) {
            this.smithyModel = smithyModel;
            return this;
        }

        public Builder smithyModel(Path smithyModelLocation) {
            this.modelLocation = smithyModelLocation;
            return this;
        }

        public Builder customizationConfig(CustomizationConfig customizationConfig) {
            this.customizationConfig = customizationConfig;
            return this;
        }

        @Override
        public SmithyModelWithCustomizations build() {
            if (modelLocation != null && smithyModel != null) {
                throw new IllegalArgumentException("Exactly one of smithyModel file or model must be set.");
            }
            if (modelLocation != null) {
                // load the model
                try {
                    String content = new String(Files.readAllBytes(modelLocation), StandardCharsets.UTF_8);
                    this.smithyModel = Model.assembler()
                                   .addUnparsedModel(modelLocation.toString(), content)
                                   .discoverModels(getClass().getClassLoader())
                        .disableValidation()
                        .assemble().unwrap();
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read model file", e);
                }
            }
            return new SmithyModelWithCustomizations(this);
        }
    }
}
