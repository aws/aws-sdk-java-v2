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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codegen.lite.PoetClass;
import software.amazon.awssdk.codegen.lite.Utils;
import software.amazon.awssdk.codegen.lite.regions.model.Partition;
import software.amazon.awssdk.codegen.lite.regions.model.Partitions;
import software.amazon.awssdk.codegen.lite.regions.model.Service;
import software.amazon.awssdk.utils.ImmutableMap;

public class ServiceMetadataGenerator implements PoetClass {

    private final Partitions partitions;
    private final String serviceEndpointPrefix;
    private final String basePackage;
    private final String regionBasePackage;

    public ServiceMetadataGenerator(Partitions partitions,
                                    String serviceEndpointPrefix,
                                    String basePackage,
                                    String regionBasePackage) {
        this.partitions = partitions;
        this.serviceEndpointPrefix = serviceEndpointPrefix;
        this.basePackage = basePackage;
        this.regionBasePackage = regionBasePackage;
    }

    @Override
    public TypeSpec poetClass() {
        TypeName listOfRegions = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(regionBasePackage, "Region"));
        TypeName mapOfStringString = ParameterizedTypeName.get(Map.class, String.class, String.class);
        TypeName listOfServicePartitionMetadata =
            ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(regionBasePackage, "ServicePartitionMetadata"));

        return TypeSpec.classBuilder(className())
                       .addModifiers(Modifier.PUBLIC)
                       .addAnnotation(AnnotationSpec.builder(Generated.class)
                                                    .addMember("value", "$S", "software.amazon.awssdk:codegen")
                                                    .build())
                       .addAnnotation(SdkPublicApi.class)
                       .addModifiers(FINAL)
                       .addSuperinterface(ClassName.get(regionBasePackage, "ServiceMetadata"))
                       .addField(FieldSpec.builder(String.class, "ENDPOINT_PREFIX")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer("$S", serviceEndpointPrefix)
                                          .build())
                       .addField(FieldSpec.builder(mapOfStringString, "PARTITION_OVERRIDDEN_ENDPOINTS")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(partitionEndpoints(partitions))
                                          .build())
                       .addField(FieldSpec.builder(mapOfStringString, "REGION_OVERRIDDEN_ENDPOINTS")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(serviceEndpoints(partitions))
                                          .build())
                       .addField(FieldSpec.builder(listOfRegions, "REGIONS")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(regionsField(partitions))
                                          .build())
                       .addField(FieldSpec.builder(mapOfStringString, "SIGNING_REGION_OVERRIDES")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(signingRegionOverrides(partitions))
                                          .build())
                       .addField(FieldSpec.builder(listOfServicePartitionMetadata, "PARTITIONS")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(servicePartitions(partitions))
                                          .build())
                       .addMethod(regions())
                       .addMethod(endpointFor())
                       .addMethod(signingRegion())
                       .addMethod(partitions(listOfServicePartitionMetadata))
                       .build();
    }

    @Override
    public ClassName className() {
        String sanitizedServiceName = serviceEndpointPrefix.replace(".", "-");
        return ClassName.get(basePackage, Stream.of(sanitizedServiceName.split("-"))
                                                .map(Utils::capitalize)
                                                .collect(Collectors.joining()) + "ServiceMetadata");
    }

    private CodeBlock partitionEndpoints(Partitions partitions) {
        Map<Partition, Service> services = getServiceData(partitions);

        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<String, String>builder()", ImmutableMap.class);

        services.forEach((key, value) -> {
            if (value.getDefaults() != null && value.getDefaults().getHostname() != null) {
                builder.add(".put($S, $S)", key.getPartition(), value.getDefaults().getHostname());
            }
        });

        return builder.add(".build()").build();
    }

    private CodeBlock serviceEndpoints(Partitions partitions) {
        Map<Partition, Service> services = getServiceData(partitions);

        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<String, String>builder()", ImmutableMap.class);

        services.entrySet()
                .forEach(s -> s.getValue().getEndpoints()
                               .entrySet()
                               .stream()
                               .filter(e -> e.getValue().getHostname() != null)
                               .filter(r -> RegionValidationUtil.validRegion(r.getKey(), s.getKey().getRegionRegex()))
                               .forEach(e -> builder.add(".put(\"" + e.getKey() + "\", " +
                                                         "\"" + e.getValue().getHostname() + "\")")));

        return builder.add(".build()").build();
    }

    private CodeBlock regionsField(Partitions partitions) {
        ClassName regionClass = ClassName.get(regionBasePackage, "Region");
        CodeBlock.Builder builder = CodeBlock.builder().add("$T.unmodifiableList($T.asList(", Collections.class, Arrays.class);

        ArrayList<String> regions = new ArrayList<>();

        partitions.getPartitions()
                  .stream()
                  .filter(p -> p.getServices().containsKey(serviceEndpointPrefix))
                  .forEach(p -> regions.addAll(p.getServices().get(serviceEndpointPrefix).getEndpoints().keySet()
                                                .stream()
                                                .filter(r -> RegionValidationUtil.validRegion(r, p.getRegionRegex()))
                                                .collect(Collectors.toList())));

        for (int i = 0; i < regions.size(); i++) {
            builder.add("$T.of($S)", regionClass, regions.get(i));
            if (i != regions.size() - 1) {
                builder.add(",");
            }
        }

        return builder.add("))").build();
    }

    private CodeBlock signingRegionOverrides(Partitions partitions) {
        Map<Partition, Service> serviceData = getServiceData(partitions);

        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<String, String>builder()", ImmutableMap.class);

        serviceData.entrySet()
                   .forEach(s -> s.getValue().getEndpoints()
                                  .entrySet()
                                  .stream()
                                  .filter(e -> e.getValue().getCredentialScope() != null)
                                  .filter(e -> e.getValue().getCredentialScope().getRegion() != null)
                                  .filter(r -> RegionValidationUtil.validRegion(r.getKey(), s.getKey().getRegionRegex()))
                                  .forEach(fm ->
                                               builder.add(".put(\"" + fm.getKey() + "\", \"" +
                                                           fm.getValue().getCredentialScope().getRegion() + "\")")));

        return builder.add(".build()").build();
    }

    private CodeBlock servicePartitions(Partitions partitions) {
        return CodeBlock.builder()
                        .add("$T.unmodifiableList($T.asList(", Collections.class, Arrays.class)
                        .add(commaSeparatedServicePartitions(partitions))
                        .add("))")
                        .build();
    }

    private CodeBlock commaSeparatedServicePartitions(Partitions partitions) {
        ClassName defaultServicePartitionMetadata = ClassName.get(regionBasePackage + ".internal",
                                                                  "DefaultServicePartitionMetadata");
        return partitions.getPartitions()
                  .stream()
                  .filter(p -> p.getServices().containsKey(serviceEndpointPrefix))
                  .map(p -> CodeBlock.of("new $T($S, $L)",
                                         defaultServicePartitionMetadata,
                                         p.getPartition(),
                                         globalRegion(p)))
                  .collect(CodeBlock.joining(","));
    }

    private CodeBlock globalRegion(Partition partition) {
        ClassName region = ClassName.get(regionBasePackage, "Region");
        Service service = partition.getServices().get(this.serviceEndpointPrefix);
        boolean hasGlobalRegionForPartition = service.isRegionalized() != null &&
                                              !service.isRegionalized() &&
                                              service.isPartitionWideEndpointAvailable();
        String globalRegionForPartition = hasGlobalRegionForPartition ? service.getPartitionEndpoint() : null;
        return globalRegionForPartition == null
                                    ? CodeBlock.of("null")
                                    : CodeBlock.of("$T.of($S)", region, globalRegionForPartition);
    }

    private MethodSpec regions() {
        TypeName listOfRegions = ParameterizedTypeName.get(ClassName.get(List.class),
                                                           ClassName.get(regionBasePackage, "Region"));
        return MethodSpec.methodBuilder("regions")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(listOfRegions)
                         .addStatement("return $L", "REGIONS")
                         .build();
    }

    private MethodSpec endpointFor() {
        return MethodSpec.methodBuilder("endpointFor")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ClassName.get(regionBasePackage, "Region"), "region")
                         .addAnnotation(Override.class)
                         .returns(URI.class)
                         .addStatement("return $T.create(REGION_OVERRIDDEN_ENDPOINTS.containsKey(region.id()) ? "
                                       + "REGION_OVERRIDDEN_ENDPOINTS.get(region.id()) : "
                                       + "computeEndpoint(ENDPOINT_PREFIX, PARTITION_OVERRIDDEN_ENDPOINTS, region))",
                                       URI.class)
                         .build();
    }

    private MethodSpec signingRegion() {
        return MethodSpec.methodBuilder("signingRegion")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(ClassName.get(regionBasePackage, "Region"), "region")
                         .addAnnotation(Override.class)
                         .returns(ClassName.get(regionBasePackage, "Region"))
                         .addStatement("return Region.of(SIGNING_REGION_OVERRIDES.getOrDefault(region.id(), region.id()))")
                         .build();
    }

    private MethodSpec partitions(TypeName listOfServicePartitionMetadata) {
        return MethodSpec.methodBuilder("servicePartitions")
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(listOfServicePartitionMetadata)
                         .addStatement("return $L", "PARTITIONS")
                         .build();
    }

    private Map<Partition, Service> getServiceData(Partitions partitions) {
        Map<Partition, Service> serviceData = new TreeMap<>(Comparator.comparing(Partition::getPartition));

        partitions.getPartitions()
                  .forEach(p -> p.getServices()
                                 .entrySet()
                                 .stream()
                                 .filter(s -> s.getKey().equalsIgnoreCase(serviceEndpointPrefix))
                                 .forEach(s -> serviceData.put(p, s.getValue())));

        return serviceData;
    }
}
