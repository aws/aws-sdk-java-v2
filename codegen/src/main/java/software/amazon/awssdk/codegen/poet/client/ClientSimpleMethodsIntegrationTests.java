/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.client;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.regions.Region;

public class ClientSimpleMethodsIntegrationTests implements ClassSpec {

    private final IntermediateModel model;
    private final PoetExtensions poetExtensions;

    public ClientSimpleMethodsIntegrationTests(IntermediateModel model) {
        this.model = model;
        this.poetExtensions = new PoetExtensions(model);
    }

    @Override
    public TypeSpec poetSpec() {
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());

        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(Modifier.PUBLIC)
                                            .addField(FieldSpec.builder(interfaceClass, "client")
                                                               .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                                               .build())
                                            .addMethod(setup());

        model.simpleMethodsRequiringTesting().stream().map(o -> simpleMethodsTest(o)).forEach(builder::addMethod);

        return builder.build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getClientClass("SimpleMethodsIntegrationTest");
    }

    /**
     * Creates a setup method for instantiating a new client. If no regions are present for a service,
     * us-east-1 will be used. If the service is available in aws-global, that region will be used. If the
     * service is not available in aws-global but is in us-east-1, that region will be used. If a service is
     * not available in us-east-1 or aws-global, the first region in the available regions for a service will
     * be used.
     */
    private MethodSpec setup() {
        ClassName beforeClass = ClassName.get("org.junit", "BeforeClass");
        ClassName interfaceClass = poetExtensions.getClientClass(model.getMetadata().getSyncInterface());
        return MethodSpec.methodBuilder("setup")
                         .addAnnotation(beforeClass)
                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                         .beginControlFlow("if ($T.serviceMetadata().regions().isEmpty())", interfaceClass)
                         .addStatement("client = $T.builder().region($T.US_EAST_1).build()", interfaceClass, Region.class)
                         .endControlFlow()
                         .beginControlFlow("else if ($T.serviceMetadata().regions().contains($T.AWS_GLOBAL))",
                                           interfaceClass,
                                           Region.class)
                         .addStatement("client = $T.builder().region($T.AWS_GLOBAL).build()",
                                       interfaceClass,
                                       Region.class)
                         .endControlFlow()
                         .beginControlFlow("else if ($T.serviceMetadata().regions().contains($T.US_EAST_1))",
                                           interfaceClass,
                                           Region.class)
                         .addStatement("client = $T.builder().region($T.US_EAST_1).build()",
                                       interfaceClass,
                                       Region.class)
                         .endControlFlow()
                         .beginControlFlow("else")
                         .addStatement("client = $1T.builder().region($1T.serviceMetadata().regions().get(0)).build()",
                                       interfaceClass)
                         .endControlFlow()
                         .build();
    }

    private MethodSpec simpleMethodsTest(OperationModel opModel) {
        ClassName testClass = ClassName.get("org.junit", "Test");
        return MethodSpec.methodBuilder(opModel.getMethodName() + "_SimpleMethod_Succeeds")
                         .addAnnotation(testClass)
                         .addException(Exception.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addStatement("client.$N()", opModel.getMethodName())
                         .build();
    }
}
