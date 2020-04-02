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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.codegen.lite.regions.model.Partitions;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public class RegionGenerator implements PoetClass {

    private final Partitions partitions;
    private final String basePackage;

    public RegionGenerator(Partitions partitions,
                           String basePackage) {
        this.partitions = partitions;
        this.basePackage = basePackage;
    }

    @Override
    public TypeSpec poetClass() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(className())
                                           .addModifiers(FINAL, PUBLIC)
                                           .addJavadoc(documentation())
                                           .addAnnotation(SdkPublicApi.class)
                                           .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                                        .addMember("value",
                                                                                   "$S",
                                                                                   "software.amazon.awssdk:codegen")
                                                                        .build())
                                           .addMethod(MethodSpec.constructorBuilder()
                                                                .addModifiers(PRIVATE)
                                                                .addParameter(String.class, "id")
                                                                .addParameter(boolean.class, "isGlobalRegion")
                                                                .addStatement("this.id = id")
                                                                .addStatement("this.isGlobalRegion = isGlobalRegion")
                                                                .build());
        regions(builder);

        builder.addField(FieldSpec.builder(boolean.class, "isGlobalRegion")
                                  .addModifiers(FINAL, PRIVATE)
                                  .build())
               .addField(FieldSpec.builder(String.class, "id")
                                  .addModifiers(FINAL, PRIVATE)
                                  .build())
               .addMethod(regionOf())
               .addMethod(regionOfGlobal())
               .addMethod(regionsGetter())
               .addMethod(id())
               .addMethod(metadata())
               .addMethod(isGlobalRegion())
               .addMethod(regionToString());

        return builder.addType(cache()).build();
    }

    private void regions(TypeSpec.Builder builder) {
        Set<String> regions = partitions.getPartitions()
                                        .stream()
                                        .flatMap(p -> p.getRegions().keySet().stream())
                                        .collect(Collectors.toSet());

        CodeBlock.Builder regionsArray = CodeBlock.builder()
                                          .add("$T.unmodifiableList($T.asList(", Collections.class, Arrays.class);

        String regionsCodeBlock = regions.stream().map(r -> {
            builder.addField(FieldSpec.builder(className(), regionName(r))
                                      .addModifiers(PUBLIC, STATIC, FINAL)
                                      .initializer("$T.of($S)", className(), r)
                                      .build());
            return regionName(r);
        }).collect(Collectors.joining(", "));

        addGlobalRegions(builder);

        regionsArray.add(regionsCodeBlock + ", ")
                    .add("AWS_GLOBAL, ")
                    .add("AWS_CN_GLOBAL, ")
                    .add("AWS_US_GOV_GLOBAL, ")
                    .add("AWS_ISO_GLOBAL, ")
                    .add("AWS_ISO_B_GLOBAL");
        regionsArray.add("))");

        TypeName listOfRegions = ParameterizedTypeName.get(ClassName.get(List.class), className());
        builder.addField(FieldSpec.builder(listOfRegions, "REGIONS")
                                  .addModifiers(PRIVATE, STATIC, FINAL)
                                  .initializer(regionsArray.build()).build());
    }

    private void addGlobalRegions(TypeSpec.Builder builder) {
        builder.addField(FieldSpec.builder(className(), "AWS_GLOBAL")
                                  .addModifiers(PUBLIC, STATIC, FINAL)
                                  .initializer("$T.of($S, true)", className(), "aws-global")
                                  .build())
               .addField(FieldSpec.builder(className(), "AWS_CN_GLOBAL")
                                  .addModifiers(PUBLIC, STATIC, FINAL)
                                  .initializer("$T.of($S, true)", className(), "aws-cn-global")
                                  .build())
               .addField(FieldSpec.builder(className(), "AWS_US_GOV_GLOBAL")
                                  .addModifiers(PUBLIC, STATIC, FINAL)
                                  .initializer("$T.of($S, true)", className(), "aws-us-gov-global")
                                  .build())
               .addField(FieldSpec.builder(className(), "AWS_ISO_GLOBAL")
                                  .addModifiers(PUBLIC, STATIC, FINAL)
                                  .initializer("$T.of($S, true)", className(), "aws-iso-global")
                                  .build())
               .addField(FieldSpec.builder(className(), "AWS_ISO_B_GLOBAL")
                                  .addModifiers(PUBLIC, STATIC, FINAL)
                                  .initializer("$T.of($S, true)", className(), "aws-iso-b-global")
                                  .build());
    }

    private String regionName(String region) {
        return region.replace("-", "_").toUpperCase(Locale.US);
    }

    private MethodSpec regionOf() {
        return MethodSpec.methodBuilder("of")
                         .addModifiers(PUBLIC, STATIC)
                         .addParameter(String.class, "value")
                         .returns(className())
                         .addStatement("return of($L, false)", "value")
                         .build();

    }

    private MethodSpec regionOfGlobal() {
        return MethodSpec.methodBuilder("of")
                         .addModifiers(PRIVATE, STATIC)
                         .addParameter(String.class, "value")
                         .addParameter(boolean.class, "isGlobalRegion")
                         .returns(className())
                         .addStatement("$T.paramNotBlank($L, $S)", Validate.class, "value", "region")
                         .addStatement("$T $L = $T.urlEncode($L)",
                                       String.class, "urlEncodedValue", SdkHttpUtils.class, "value")
                         .addStatement("return $L.put($L, $L)", "RegionCache", "urlEncodedValue", "isGlobalRegion")
                         .build();

    }

    private MethodSpec id() {
        return MethodSpec.methodBuilder("id")
                         .addModifiers(PUBLIC)
                         .returns(String.class)
                         .addStatement("return this.id")
                         .build();
    }

    private MethodSpec metadata() {
        ClassName regionMetadataClass = ClassName.get("software.amazon.awssdk.regions", "RegionMetadata");
        return MethodSpec.methodBuilder("metadata")
                         .addModifiers(PUBLIC)
                         .returns(regionMetadataClass)
                         .addStatement("return $T.of(this)", regionMetadataClass)
                         .build();
    }

    private MethodSpec regionsGetter() {
        return MethodSpec.methodBuilder("regions")
                         .addModifiers(PUBLIC, STATIC)
                         .returns(ParameterizedTypeName.get(ClassName.get(List.class),
                                                            className()))
                         .addStatement("return $L", "REGIONS")
                         .build();
    }

    private MethodSpec isGlobalRegion() {
        return MethodSpec.methodBuilder("isGlobalRegion")
                         .addModifiers(PUBLIC)
                         .returns(boolean.class)
                         .addStatement("return $L", "isGlobalRegion")
                         .build();
    }

    private MethodSpec regionToString() {
        return MethodSpec.methodBuilder("toString")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC)
                         .returns(String.class)
                         .addStatement("return $L", "id")
                         .build();
    }

    private TypeSpec cache() {
        ParameterizedTypeName mapOfStringRegion = ParameterizedTypeName.get(ClassName.get(ConcurrentHashMap.class),
                                                                            ClassName.get(String.class),
                                                                            className());

        return TypeSpec.classBuilder("RegionCache")
                       .addModifiers(PRIVATE, STATIC)
                       .addField(FieldSpec.builder(mapOfStringRegion, "VALUES")
                                          .addModifiers(PRIVATE, STATIC, FINAL)
                                          .initializer("new $T<>()", ConcurrentHashMap.class)
                                          .build())

                       .addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build())
                       .addMethod(MethodSpec.methodBuilder("put")
                                            .addModifiers(PRIVATE, STATIC)
                                            .addParameter(String.class, "value")
                                            .addParameter(boolean.class, "isGlobalRegion")
                                            .returns(className())
                                            .addStatement("return $L.computeIfAbsent(value, v -> new $T(value, isGlobalRegion))",
                                                          "VALUES",
                                                          className())
                                            .build())
                       .build();
    }

    private CodeBlock documentation() {
        return CodeBlock.builder()
                        .add("An Amazon Web Services region that hosts a set of Amazon services.")
                        .add(System.lineSeparator())
                        .add("<p>An instance of this class can be retrieved by referencing one of the static constants defined in"
                             + " this class (eg. {@link Region#US_EAST_1}) or by using the {@link Region#of(String)} method if "
                             + "the region you want is not included in this release of the SDK.</p>")
                        .add(System.lineSeparator())
                        .add("<p>Each AWS region corresponds to a separate geographical location where a set of Amazon services "
                             + "is deployed. These regions (except for the special {@link #AWS_GLOBAL} and {@link #AWS_CN_GLOBAL}"
                             + " regions) are separate from each other, with their own set of resources. This means a resource "
                             + "created in one region (eg. an SQS queue) is not available in another region.</p>")
                        .add(System.lineSeparator())
                        .add("<p>To programmatically determine whether a particular service is deployed to a region, you can use "
                             + "the {@code serviceMetadata} method on the service's client interface. Additional metadata about "
                             + "a region can be discovered using {@link RegionMetadata#of(Region)}.</p>")
                        .add(System.lineSeparator())
                        .add("<p>The {@link Region#id()} will be used as the signing region for all requests to AWS services "
                             + "unless an explicit region override is available in {@link RegionMetadata}. This id will also be "
                             + "used to construct the endpoint for accessing a service unless an explicit endpoint is available "
                             + "for that region in {@link RegionMetadata}.</p>")
                        .build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(basePackage, "Region");
    }
}
