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

import static java.util.Collections.emptyList;
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
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.codegen.lite.Utils;
import software.amazon.awssdk.codegen.lite.regions.model.Partition;
import software.amazon.awssdk.utils.ImmutableMap;

public class PartitionMetadataGenerator implements PoetClass {

    private final Partition partition;
    private final String basePackage;
    private final String regionBasePackage;

    public PartitionMetadataGenerator(Partition partition,
                                      String basePackage,
                                      String regionBasePackage) {
        this.partition = partition;
        this.basePackage = basePackage;
        this.regionBasePackage = regionBasePackage;
    }

    @Override
    public TypeSpec poetClass() {
        TypeName mapByPartitionEndpointKey = ParameterizedTypeName.get(ClassName.get(Map.class),
                                                                       partitionEndpointKeyClass(),
                                                                       ClassName.get(String.class));

        return TypeSpec.classBuilder(className())
                       .addModifiers(FINAL, PUBLIC)
                       .addSuperinterface(ClassName.get(regionBasePackage, "PartitionMetadata"))
                       .addAnnotation(SdkPublicApi.class)
                       .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                    .addMember("value",
                                                               "$S",
                                                               "software.amazon.awssdk:codegen")
                                                    .build())
                       .addField(FieldSpec.builder(mapByPartitionEndpointKey, "DNS_SUFFIXES")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(dnsSuffixes())
                                          .build())
                       .addField(FieldSpec.builder(mapByPartitionEndpointKey, "HOSTNAMES")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(hostnames())
                                          .build())
                       .addField(FieldSpec.builder(String.class, "ID")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer("$S", partition.getPartition())
                                          .build())
                       .addField(FieldSpec.builder(String.class, "NAME")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer("$S", partition.getPartitionName())
                                          .build())
                       .addField(FieldSpec.builder(String.class, "REGION_REGEX")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer("$S", partition.getRegionRegex())
                                          .build())
                       .addMethod(getter("id", "ID"))
                       .addMethod(getter("name", "NAME"))
                       .addMethod(getter("regionRegex", "REGION_REGEX"))
                       .addMethod(dnsSuffixGetter())
                       .addMethod(hostnameGetter())
                       .build();
    }

    private CodeBlock dnsSuffixes() {
        CodeBlock.Builder builder =
            CodeBlock.builder()
                     .add("$T.<$T, $T>builder()", ImmutableMap.class, partitionEndpointKeyClass(), String.class);

        builder.add(".put(")
               .add(partitionEndpointKey(emptyList()))
               .add(", $S)", partition.getDnsSuffix());

        if (partition.getDefaults() != null) {
            partition.getDefaults().getVariants().forEach(variant -> {
                if (variant.getDnsSuffix() != null) {
                    builder.add(".put(")
                           .add(partitionEndpointKey(variant.getTags()))
                           .add(", $S)", variant.getDnsSuffix());
                }
            });
        }

        return builder.add(".build()").build();
    }

    private CodeBlock hostnames() {
        CodeBlock.Builder builder =
            CodeBlock.builder()
                     .add("$T.<$T, $T>builder()", ImmutableMap.class, partitionEndpointKeyClass(), String.class);


        if (partition.getDefaults() != null) {
            builder.add(".put(")
                   .add(partitionEndpointKey(emptyList()))
                   .add(", $S)", partition.getDefaults().getHostname());

            partition.getDefaults().getVariants().forEach(variant -> {
                if (variant.getHostname() != null) {
                    builder.add(".put(")
                           .add(partitionEndpointKey(variant.getTags()))
                           .add(", $S)", variant.getHostname());
                }
            });
        }

        return builder.add(".build()").build();
    }

    private MethodSpec dnsSuffixGetter() {
        return MethodSpec.methodBuilder("dnsSuffix")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC)
                         .returns(String.class)
                         .addParameter(partitionEndpointKeyClass(), "key")
                         .addStatement("return DNS_SUFFIXES.get(key)")
                         .build();
    }

    private MethodSpec hostnameGetter() {
        return MethodSpec.methodBuilder("hostname")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC)
                         .returns(String.class)
                         .addParameter(partitionEndpointKeyClass(), "key")
                         .addStatement("return HOSTNAMES.get(key)")
                         .build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(basePackage, Stream.of(partition.getPartition().split("-"))
                                                .map(Utils::capitalize)
                                                .collect(Collectors.joining()) + "PartitionMetadata");
    }

    private MethodSpec getter(String methodName, String field) {
        return MethodSpec.methodBuilder(methodName)
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(String.class)
                         .addStatement("return $L", field)
                         .build();
    }

    private CodeBlock partitionEndpointKey(Collection<String> tags) {
        CodeBlock.Builder result = CodeBlock.builder();
        result.add("$T.builder()", partitionEndpointKeyClass());

        if (!tags.isEmpty()) {
            CodeBlock tagsParameter = tags.stream()
                                          .map(tag -> CodeBlock.of("$T.of($S)", endpointTagClass(), tag))
                                          .collect(CodeBlock.joining(", "));

            result.add(".tags(").add(tagsParameter).add(")");
        }

        result.add(".build()");
        return result.build();
    }

    private ClassName endpointTagClass() {
        return ClassName.get(regionBasePackage, "EndpointTag");
    }

    private ClassName partitionEndpointKeyClass() {
        return ClassName.get(regionBasePackage, "PartitionEndpointKey");
    }
}
