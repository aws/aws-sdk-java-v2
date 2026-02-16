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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.codegen.lite.Utils;
import software.amazon.awssdk.codegen.lite.regions.model.PartitionRegionsMetadata;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.StringUtils;

public class PartitionMetadataGenerator implements PoetClass {

    /**
     * Hardcoded mapping of partition IDs to display names.
     * This preserves backward compatibility since partitions.json only provides
     * partition IDs, while the old endpoints.json had separate partitionName fields.
     * New partitions will fall back to using their ID as the display name.
     */
    private static final Map<String, String> PARTITION_DISPLAY_NAMES =
            ImmutableMap.<String, String>builder()
                        .put("aws", "AWS Standard")
                        .put("aws-cn", "AWS China")
                        .put("aws-us-gov", "AWS GovCloud (US)")
                        .put("aws-iso", "AWS ISO (US)")
                        .put("aws-iso-b", "AWS ISOB (US)")
                        .put("aws-iso-e", "AWS ISOE (Europe)")
                        .put("aws-iso-f", "AWS ISOF")
                        .put("aws-eusc", "AWS EUSC")
                        .build();

    private final PartitionRegionsMetadata partition;
    private final String basePackage;
    private final String regionBasePackage;

    public PartitionMetadataGenerator(PartitionRegionsMetadata partition,
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
                                          .initializer("$S", partition.getId())
                                          .build())
                       .addField(FieldSpec.builder(String.class, "NAME")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer("$S", PARTITION_DISPLAY_NAMES.getOrDefault(
                                              partition.getId(), partition.getId()))
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

        String defaultDnsSuffix = partition.getOutputs().getDnsSuffix();
        String dualStackDnsSuffix = partition.getOutputs().getDualStackDnsSuffix();
        boolean supportsFips = partition.getOutputs().isSupportsFIPS();
        boolean supportsDualStack = partition.getOutputs().isSupportsDualStack();

        builder.add(".put(")
               .add(partitionEndpointKey(Collections.emptyList()))
               .add(", $S)", defaultDnsSuffix);

        if (supportsFips) {
            builder.add(".put(")
                   .add(partitionEndpointKey(Collections.singletonList("fips")))
                   .add(", $S)", defaultDnsSuffix);
        }

        if (supportsDualStack && supportsFips) {
            validateDualStackDnsSuffix(dualStackDnsSuffix);
            builder.add(".put(")
                   .add(partitionEndpointKey(Arrays.asList("dualstack", "fips")))
                   .add(", $S)", dualStackDnsSuffix);
        }

        if (supportsDualStack) {
            validateDualStackDnsSuffix(dualStackDnsSuffix);
            builder.add(".put(")
                   .add(partitionEndpointKey(Collections.singletonList("dualstack")))
                   .add(", $S)", dualStackDnsSuffix);
        }

        return builder.add(".build()").build();
    }

    private CodeBlock hostnames() {
        CodeBlock.Builder builder =
            CodeBlock.builder()
                     .add("$T.<$T, $T>builder()", ImmutableMap.class, partitionEndpointKeyClass(), String.class);

        boolean supportsFips = partition.getOutputs().isSupportsFIPS();
        boolean supportsDualStack = partition.getOutputs().isSupportsDualStack();
        String dualStackDnsSuffix = partition.getOutputs().getDualStackDnsSuffix();

        builder.add(".put(")
               .add(partitionEndpointKey(Collections.emptyList()))
               .add(", $S)", "{service}.{region}.{dnsSuffix}");

        if (supportsFips) {
            builder.add(".put(")
                   .add(partitionEndpointKey(Collections.singletonList("fips")))
                   .add(", $S)", "{service}-fips.{region}.{dnsSuffix}");
        }

        if (supportsDualStack && supportsFips) {
            validateDualStackDnsSuffix(dualStackDnsSuffix);
            builder.add(".put(")
                   .add(partitionEndpointKey(Arrays.asList("dualstack", "fips")))
                   .add(", $S)", "{service}-fips.{region}.{dnsSuffix}");
        }

        if (supportsDualStack) {
            validateDualStackDnsSuffix(dualStackDnsSuffix);
            builder.add(".put(")
                   .add(partitionEndpointKey(Collections.singletonList("dualstack")))
                   .add(", $S)", "{service}.{region}.{dnsSuffix}");
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
        return ClassName.get(basePackage, Stream.of(partition.getId().split("-"))
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

    private void validateDualStackDnsSuffix(String dualStackDnsSuffix) {
        if (StringUtils.isBlank(dualStackDnsSuffix)) {
            throw new IllegalStateException("Partition " + partition.getId() 
                + " claims to support dualstack but dualStackDnsSuffix is null or empty");
        }
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
