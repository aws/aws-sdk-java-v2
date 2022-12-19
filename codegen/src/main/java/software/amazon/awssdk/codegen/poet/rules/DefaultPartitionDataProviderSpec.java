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

package software.amazon.awssdk.codegen.poet.rules;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Validate;

public class DefaultPartitionDataProviderSpec implements ClassSpec {
    private final IntermediateModel model;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;

    private final ClassName partitionsClass;

    public DefaultPartitionDataProviderSpec(IntermediateModel model) {
        this.model = model;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
        this.partitionsClass = endpointRulesSpecUtils.rulesRuntimeClassName("Partitions");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addAnnotation(SdkInternalApi.class)
                                            .addSuperinterface(
                                                endpointRulesSpecUtils.rulesRuntimeClassName("PartitionDataProvider"));

        builder.addField(partitionDataField());
        builder.addField(partitionsLazyField());
        builder.addMethod(loadPartitionsMethod());
        builder.addMethod(doLoadPartitionsMethod());
        return builder.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.rulesRuntimeClassName("DefaultPartitionDataProvider");
    }

    private MethodSpec loadPartitionsMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("loadPartitions")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(partitionsClass);

        builder.addStatement("return PARTITIONS.getValue()");

        return builder.build();
    }

    private FieldSpec partitionDataField() {
        FieldSpec.Builder builder = FieldSpec.builder(String.class, "DEFAULT_PARTITION_DATA", Modifier.PRIVATE,
                                                      Modifier.STATIC, Modifier.FINAL);
        builder.initializer("$S", readPartitionsJson());
        return builder.build();
    }

    private FieldSpec partitionsLazyField() {
        ParameterizedTypeName lazyType = ParameterizedTypeName.get(ClassName.get(Lazy.class),
                                                                   partitionsClass);
        FieldSpec.Builder builder = FieldSpec.builder(lazyType, "PARTITIONS")
                                             .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
        CodeBlock init = CodeBlock.builder()
                                  .addStatement("new $T<>($T::doLoadPartitions)", Lazy.class, className())
                                  .build();

        builder.initializer(init);

        return builder.build();
    }

    private MethodSpec doLoadPartitionsMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("doLoadPartitions")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(partitionsClass);

        builder.addStatement("return $T.fromNode($T.parser().parse(DEFAULT_PARTITION_DATA))", partitionsClass, JsonNode.class);

        return builder.build();
    }

    private String readPartitionsJson() {
        String jsonPath = endpointRulesSpecUtils.rulesEngineResourceFiles()
                                                .stream()
                                                .filter(e -> e.endsWith("partitions.json.resource"))
                                                .findFirst()
                                                .orElseThrow(
                                                    () -> new RuntimeException("Could not find partitions.json.resource"));

        return loadResourceAsString("/" + jsonPath);
    }

    private String loadResourceAsString(String path) {
        try {
            return IoUtils.toUtf8String(loadResource(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InputStream loadResource(String name) {
        InputStream resourceAsStream = DefaultPartitionDataProviderSpec.class.getResourceAsStream(name);
        Validate.notNull(resourceAsStream, "Failed to load resource from %s", name);
        return resourceAsStream;
    }
}
