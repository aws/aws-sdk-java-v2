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

package software.amazon.awssdk.codegen.emitters;

import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Common paths used by generator tasks.
 */
public class GeneratorPathProvider {

    private final IntermediateModel model;
    private final String sourceDirectory;
    private final String testDirectory;

    public GeneratorPathProvider(IntermediateModel model, String sourceDirectory, String testDirectory) {
        this.model = model;
        this.sourceDirectory = sourceDirectory;
        this.testDirectory = testDirectory;
    }

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getTestDirectory() {
        return testDirectory;
    }

    public String getModelDirectory() {
        return sourceDirectory + "/" + Utils.packageToDirectory(model.getMetadata().getFullModelPackageName());
    }

    public String getTransformDirectory() {
        return sourceDirectory + "/" + Utils.packageToDirectory(model.getMetadata().getFullTransformPackageName());
    }

    public String getClientDirectory() {
        return sourceDirectory + "/" + Utils.packageToDirectory(model.getMetadata().getFullClientPackageName());
    }

    public String getPaginatorsDirectory() {
        return sourceDirectory + "/" + Utils.packageToDirectory(model.getMetadata().getFullPaginatorsPackageName());
    }

    public String getAuthorizerDirectory() {
        return sourceDirectory + "/" + Utils.packageToDirectory(model.getMetadata().getFullAuthPolicyPackageName());
    }

    public String getWaitersDirectory() {
        return sourceDirectory + "/" + Utils.packageToDirectory(model.getMetadata().getFullWaitersPackageName());
    }

    public String getWaitersInternalDirectory() {
        return sourceDirectory + "/" + Utils.packageToDirectory(model.getMetadata().getFullWaitersInternalPackageName());
    }
}
