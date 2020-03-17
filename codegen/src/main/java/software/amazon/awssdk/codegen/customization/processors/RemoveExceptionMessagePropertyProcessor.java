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

package software.amazon.awssdk.codegen.customization.processors;

import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

/**
 * This processor removes the member *message* in the exception shapes from the
 * intermediate model. Every exception class generated extends
 * SdkException and the *message* member is inherited from that class.
 */
public class RemoveExceptionMessagePropertyProcessor implements CodegenCustomizationProcessor {

    private static final boolean IGNORE_CASE = true;

    @Override
    public void preprocess(ServiceModel serviceModel) {
        // Do Nothing
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {

        for (ShapeModel shapeModel : intermediateModel.getShapes().values()) {
            if (ShapeType.Exception == shapeModel.getShapeType()) {
                shapeModel.removeMemberByC2jName("message", IGNORE_CASE);
            }
        }
    }
}
