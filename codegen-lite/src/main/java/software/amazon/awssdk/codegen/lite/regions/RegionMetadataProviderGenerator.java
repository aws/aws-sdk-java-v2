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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.codegen.lite.Utils;
import software.amazon.awssdk.codegen.lite.regions.model.Partitions;
import software.amazon.awssdk.utils.ImmutableMap;

public class RegionMetadataProviderGenerator implements PoetClass {

    private final Partitions partitions;
    private final String basePackage;
    private final String regionBasePackage;

    public RegionMetadataProviderGenerator(Partitions partitions,
                                           String basePackage,
                                           String regionBasePackage) {
        this.partitions = partitions;
        this.basePackage = basePackage;
        this.regionBasePackage = regionBasePackage;
    }

    @Override
    public TypeSpec poetClass() {
        TypeName mapOfRegionMetadata = ParameterizedTypeName.get(ClassName.get(Map.class),
                                                                 ClassName.get(regionBasePackage, "Region"),
                                                                 ClassName.get(regionBasePackage, "RegionMetadata"));
        return TypeSpec.classBuilder(className())
                       .addModifiers(PUBLIC)
                       .addSuperinterface(ClassName.get(regionBasePackage, "RegionMetadataProvider"))
                       .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                    .addMember("value", "$S", "software.amazon.awssdk:codegen")
                                                    .build())
                       .addAnnotation(SdkPublicApi.class)
                       .addModifiers(FINAL)
                       .addField(FieldSpec.builder(mapOfRegionMetadata, "REGION_METADATA")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(regions(partitions))
                                          .build())
                       .addMethod(getter())
                       .build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(regionBasePackage, "GeneratedRegionMetadataProvider");
    }

    private CodeBlock regions(Partitions partitions) {
        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<Region, RegionMetadata>builder()", ImmutableMap.class);

        partitions.getPartitions()
                  .forEach(p -> p.getRegions()
                                 .keySet()
                                 .forEach(r -> builder.add(".put(Region.$L, new $T())", regionClass(r), regionMetadataClass(r))));

        return builder.add(".build()").build();
    }

    private String regionClass(String region) {
        return region.replace("-", "_").toUpperCase(Locale.US);
    }

    private ClassName regionMetadataClass(String region) {
        return ClassName.get(basePackage, Stream.of(region.split("-")).map(Utils::capitalize).collect(Collectors.joining()));
    }

    private MethodSpec getter() {
        return MethodSpec.methodBuilder("regionMetadata")
                         .addModifiers(PUBLIC)
                         .addParameter(ClassName.get(regionBasePackage, "Region"), "region")
                         .returns(ClassName.get(regionBasePackage, "RegionMetadata"))
                         .addStatement("return $L.get($L)", "REGION_METADATA", "region")
                         .build();
    }
}
