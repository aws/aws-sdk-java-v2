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

package software.amazon.awssdk.codegen.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;

public final class SpecUtils {
    private SpecUtils(){
    }

    public static void addCustomMarshaller(CodeBlock.Builder builder, OperationModel opModel) {
        ClassName marshaller = PoetUtils.classNameFromFqcn(opModel.getInputShape().getMarshallerFqcn());
        builder.add(".withMarshaller(new $T())\n", marshaller);
    }

    public static CodeBlock putPresignedUrlAttribute(OperationModel opModel) {
        if (opModel.isPresignedUrl()) {
            return CodeBlock.of(".putExecutionAttribute($T.PRESIGNED_URL, $L.presignedUrl().toURI())\n",
                                SdkInternalExecutionAttribute.class, opModel.getInput().getVariableName());
        }

        return CodeBlock.of("");
    }
}
