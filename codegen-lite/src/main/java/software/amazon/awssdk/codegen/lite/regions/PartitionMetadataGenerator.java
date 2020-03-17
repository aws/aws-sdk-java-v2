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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.codegen.lite.Utils;
import software.amazon.awssdk.codegen.lite.regions.model.Partition;

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
        return TypeSpec.classBuilder(className())
                       .addModifiers(FINAL, PUBLIC)
                       .addSuperinterface(ClassName.get(regionBasePackage, "PartitionMetadata"))
                       .addAnnotation(SdkPublicApi.class)
                       .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                    .addMember("value",
                                                               "$S",
                                                               "software.amazon.awssdk:codegen")
                                                    .build())
                       .addField(FieldSpec.builder(String.class, "DNS_SUFFIX")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer("$S", partition.getDnsSuffix())
                                          .build())
                       .addField(FieldSpec.builder(String.class, "HOSTNAME")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer("$S", partition.getDefaults().getHostname())
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
                       .addMethod(getter("dnsSuffix", "DNS_SUFFIX"))
                       .addMethod(getter("hostname", "HOSTNAME"))
                       .addMethod(getter("id", "ID"))
                       .addMethod(getter("name", "NAME"))
                       .addMethod(getter("regionRegex", "REGION_REGEX"))
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
}
