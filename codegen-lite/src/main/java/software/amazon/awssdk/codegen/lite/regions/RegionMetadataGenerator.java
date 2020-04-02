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
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.codegen.lite.Utils;
import software.amazon.awssdk.codegen.lite.regions.model.Partition;

public class RegionMetadataGenerator implements PoetClass {

    private final Partition partition;
    private final String region;
    private final String regionDescription;
    private final String basePackage;
    private final String regionBasePackage;

    public RegionMetadataGenerator(Partition partition,
                                   String region,
                                   String regionDescription,
                                   String basePackage,
                                   String regionBasePackage) {
        this.partition = partition;
        this.region = region;
        this.regionDescription = regionDescription;
        this.basePackage = basePackage;
        this.regionBasePackage = regionBasePackage;
    }

    @Override
    public TypeSpec poetClass() {
        return TypeSpec.classBuilder(className())
                       .addModifiers(Modifier.PUBLIC)
                       .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                    .addMember("value", "$S", "software.amazon.awssdk:codegen")
                                                    .build())
                       .addAnnotation(SdkPublicApi.class)
                       .addModifiers(FINAL)
                       .addSuperinterface(ClassName.get(regionBasePackage, "RegionMetadata"))
                       .addField(staticFinalField("ID", region))
                       .addField(staticFinalField("DOMAIN", partition.getDnsSuffix()))
                       .addField(staticFinalField("DESCRIPTION", regionDescription))
                       .addField(staticFinalField("PARTITION_ID", partition.getPartition()))
                       .addMethod(getter("id", "ID"))
                       .addMethod(getter("domain", "DOMAIN"))
                       .addMethod(getter("description", "DESCRIPTION"))
                       .addMethod(partition())
                       .build();
    }

    private MethodSpec partition() {
        ClassName regionMetadataClass = ClassName.get("software.amazon.awssdk.regions", "PartitionMetadata");
        return MethodSpec.methodBuilder("partition")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(regionMetadataClass)
                         .addStatement("return $T.of(PARTITION_ID)", regionMetadataClass)
                         .build();
    }

    @Override
    public ClassName className() {
        return ClassName.get(basePackage, Stream.of(region.split("-")).map(Utils::capitalize).collect(Collectors.joining()));
    }

    private FieldSpec staticFinalField(String fieldName, String fieldValue) {
        return FieldSpec.builder(String.class, fieldName)
                        .addModifiers(PRIVATE, FINAL, STATIC)
                        .initializer("$S", fieldValue)
                        .build();
    }

    private MethodSpec getter(String getterName, String fieldName) {
        return MethodSpec.methodBuilder(getterName)
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(String.class)
                         .addStatement("return $L", fieldName.toUpperCase(Locale.US))
                         .build();
    }
}
