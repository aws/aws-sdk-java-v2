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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.exception.SdkClientException;

public class ServiceOperationsSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final PoetExtension poetExt;

    public ServiceOperationsSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.poetExt = new PoetExtension(intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className());
        builder.addAnnotation(SdkPublicApi.class);
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        operationFields().forEach(builder::addField);
        builder.addMethod(requestObjectToOperationMethod());

        return builder.build();
    }

    private List<FieldSpec> operationFields() {
        return intermediateModel.getOperations().keySet()
            .stream()
            .map(name -> {
                String fieldName = intermediateModel.getNamingStrategy().getEnumValueName(name);
                FieldSpec.Builder builder = FieldSpec.builder(String.class, fieldName, Modifier.PUBLIC, Modifier.FINAL);
                builder.initializer("$S", name);
                return builder.build();
            })
            .collect(Collectors.toList());
    }

    //    public String requestObjectToName(QueryRequest req) {
    //         if (req instanceof APostOperationRequest) {
    //             return "APostOperation";
    //         }
    //
    //         ...
    //
    //         throw SdkClientException.create("Unknown request object!");
    //     }
    private MethodSpec requestObjectToOperationMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("requestObjectToName");

        builder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        builder.addParameter(poetExt.getModelClass(intermediateModel.getMetadata().getBaseRequestName()), "req");

        intermediateModel.getOperations()
                         .entrySet()
                         .stream()
                         .map(e -> {
                             String name = e.getKey();
                             ClassName requestClass = poetExt.getModelClassFromShape(e.getValue().getInputShape());

                             CodeBlock.Builder instanceCheck = CodeBlock.builder();
                             instanceCheck.beginControlFlow("if (req instanceof $T)", requestClass);
                             instanceCheck.addStatement("return $S", name);
                             instanceCheck.endControlFlow();

                             return instanceCheck.build();
                          })
                         .forEach(builder::addCode);

        builder.addStatement("throw $T.create($S)", SdkClientException.class, "Unknown request object");

        builder.returns(String.class);

        return builder.build();
    }

    @Override
    public ClassName className() {
        String name = intermediateModel.getMetadata().getServiceName() + "Operations";
        // String pkg = intermediateModel.getMetadata().getModelPackageName();
        // return ClassName.get(pkg, name);
        return poetExt.getModelClass(name);
    }
}
