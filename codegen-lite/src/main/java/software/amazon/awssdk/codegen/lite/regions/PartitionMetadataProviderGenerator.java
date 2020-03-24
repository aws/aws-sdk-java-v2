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

package software.amazon.awssdk.codegen.lite.regions;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.codegen.lite.Utils;
import software.amazon.awssdk.codegen.lite.regions.model.Partitions;
import software.amazon.awssdk.utils.ImmutableMap;

public class PartitionMetadataProviderGenerator implements PoetClass {
    private final Partitions partitions;
    private final String basePackage;
    private final String regionBasePackage;

    public PartitionMetadataProviderGenerator(Partitions partitions,
                                           String basePackage,
                                           String regionBasePackage) {
        this.partitions = partitions;
        this.basePackage = basePackage;
        this.regionBasePackage = regionBasePackage;
    }

    @Override
    public TypeSpec poetClass() {
        TypeName mapOfPartitionMetadata = ParameterizedTypeName.get(ClassName.get(Map.class),
                                                                 ClassName.get(String.class),
                                                                 ClassName.get(regionBasePackage, "PartitionMetadata"));
        return TypeSpec.classBuilder(className())
                       .addModifiers(PUBLIC)
                       .addSuperinterface(ClassName.get(regionBasePackage, "PartitionMetadataProvider"))
                       .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                    .addMember("value", "$S", "software.amazon.awssdk:codegen")
                                                    .build())
                       .addAnnotation(SdkPublicApi.class)
                       .addModifiers(FINAL)
                       .addField(FieldSpec.builder(mapOfPartitionMetadata, "PARTITION_METADATA")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(partitions(partitions))
                                          .build())
                       .addMethod(getter())
                       .addMethod(partitionMetadata())
                       .build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(regionBasePackage, "GeneratedPartitionMetadataProvider");
    }

    private CodeBlock partitions(Partitions partitions) {
        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<String, PartitionMetadata>builder()", ImmutableMap.class);

        partitions.getPartitions()
                  .forEach(p -> builder.add(".put($S, new $T())", p.getPartition(), partitionMetadataClass(p.getPartition())));

        return builder.add(".build()").build();
    }

    private ClassName partitionMetadataClass(String partition) {
        return ClassName.get(basePackage, Stream.of(partition.split("-"))
                                                .map(Utils::capitalize)
                                                .collect(Collectors.joining()) + "PartitionMetadata");
    }

    private MethodSpec partitionMetadata() {
        return MethodSpec.methodBuilder("partitionMetadata")
                         .addModifiers(PUBLIC)
                         .addParameter(ClassName.get(regionBasePackage, "Region"), "region")
                         .returns(ClassName.get(regionBasePackage, "PartitionMetadata"))
                         .addStatement("return $L.values().stream().filter(p -> $L.id().matches(p.regionRegex()))" +
                                       ".findFirst().orElse(new $L())",
                                       "PARTITION_METADATA",
                                       "region",
                                       "AwsPartitionMetadata")
                         .build();
    }

    private MethodSpec getter() {
        return MethodSpec.methodBuilder("partitionMetadata")
                         .addModifiers(PUBLIC)
                         .addParameter(String.class, "partition")
                         .returns(ClassName.get(regionBasePackage, "PartitionMetadata"))
                         .addStatement("return $L.get($L)", "PARTITION_METADATA", "partition")
                         .build();
    }
}
