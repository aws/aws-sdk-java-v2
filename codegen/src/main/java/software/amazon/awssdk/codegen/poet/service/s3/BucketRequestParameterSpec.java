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

package software.amazon.awssdk.codegen.poet.service.s3;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.SdkRequest;

public class BucketRequestParameterSpec implements ClassSpec {
    private final IntermediateModel model;
    private final PoetExtension poetExtension;

    public BucketRequestParameterSpec(IntermediateModel model) {
        this.model = model;
        this.poetExtension = new PoetExtension(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                      .addAnnotation(SdkInternalApi.class);
        b.addMethod(setParams());

        return b.build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(getPackage(),
                             model.getMetadata().getServiceName() + "BucketRequestParams");
    }

    private String getPackage() {
        return model.getMetadata().getFullClientInternalPackageName();
    }

    private MethodSpec setParams() {
        Map<String, OperationModel> operations = model.getOperations();

        MethodSpec.Builder b = MethodSpec.methodBuilder("bucketName")
                                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                         .addParameter(SdkRequest.class, "request")
                                         .returns(ParameterizedTypeName.get(Optional.class, String.class));

        operations.forEach((n, m) -> {
            Optional<MemberModel> bucketContextParam = bucketContextParam(m);
            bucketContextParam.ifPresent(bucket -> b.addCode(addBucketParamStatement(bucket, requestClassName(m))));
        });

        b.addStatement("return Optional.empty()");

        return b.build();
    }

    private CodeBlock addBucketParamStatement(MemberModel bucketMember, ClassName requestClassName) {
        CodeBlock.Builder b = CodeBlock.builder();
        b.beginControlFlow("if (request instanceof $T)", requestClassName);
        b.addStatement("return Optional.of((($T) request).$N())", requestClassName, bucketMember.getFluentGetterMethodName());
        b.endControlFlow();
        return b.build();
    }

    private ClassName requestClassName(OperationModel opModel) {
        String requestClassName = model.getNamingStrategy().getRequestClassName(opModel.getOperationName());
        return poetExtension.getModelClass(requestClassName);
    }

    private Optional<MemberModel> bucketContextParam(OperationModel opModel) {
        return opModel.getInputShape().getMembers().stream()
                      .filter(m -> m.getContextParam() != null &&
                                   "bucket".equalsIgnoreCase(m.getContextParam().getName())).findFirst();
    }
}
