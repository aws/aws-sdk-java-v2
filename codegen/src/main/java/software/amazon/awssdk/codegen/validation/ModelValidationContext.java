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

package software.amazon.awssdk.codegen.validation;

import java.util.Optional;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

public final class ModelValidationContext {
    private final IntermediateModel intermediateModel;
    private final IntermediateModel shareModelsTarget;

    private ModelValidationContext(Builder builder) {
        this.intermediateModel = builder.intermediateModel;
        this.shareModelsTarget = builder.shareModelsTarget;
    }

    public IntermediateModel intermediateModel() {
        return intermediateModel;
    }

    public Optional<IntermediateModel> shareModelsTarget() {
        return Optional.ofNullable(shareModelsTarget);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private IntermediateModel intermediateModel;
        private IntermediateModel shareModelsTarget;

        public Builder intermediateModel(IntermediateModel intermediateModel) {
            this.intermediateModel = intermediateModel;
            return this;
        }

        public Builder shareModelsTarget(IntermediateModel shareModelsTarget) {
            this.shareModelsTarget = shareModelsTarget;
            return this;
        }

        public ModelValidationContext build() {
            return new ModelValidationContext(this);
        }
    }
}
