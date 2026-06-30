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

package software.amazon.awssdk.codegen.poet.crac;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;

/**
 * Generates an empty {@link SdkWarmUpProvider} implementation per service. The {@code warmUp()} body is intentionally a
 * no-op in this stage; the synthetic priming call is added in a later stage. ServiceLoader requires a public no-arg
 * constructor, which the default constructor satisfies.
 */
public class WarmUpProviderSpec implements ClassSpec {

    private final IntermediateModel model;

    public WarmUpProviderSpec(IntermediateModel model) {
        this.model = model;
    }

    @Override
    public ClassName className() {
        return ClassName.get(model.getMetadata().getFullCracInternalPackageName(),
                             model.getMetadata().getServiceName() + "WarmUpProvider");
    }

    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(className())
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addAnnotation(SdkInternalApi.class)
                        .addSuperinterface(SdkWarmUpProvider.class)
                        .addMethod(warmUpMethod())
                        .build();
    }

    private MethodSpec warmUpMethod() {
        return MethodSpec.methodBuilder("warmUp")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .build();
    }
}
