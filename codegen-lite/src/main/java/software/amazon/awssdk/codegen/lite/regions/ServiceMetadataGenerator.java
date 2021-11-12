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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
import software.amazon.awssdk.codegen.lite.regions.model.Endpoint;
import software.amazon.awssdk.codegen.lite.regions.model.Partition;
import software.amazon.awssdk.codegen.lite.regions.model.Partitions;
import software.amazon.awssdk.codegen.lite.regions.model.Service;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;

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

        removeBadRegions();
    }

    private void removeBadRegions() {
        partitions.getPartitions().forEach(partition -> {
            partition.getServices().values().forEach(service -> {
                Iterator<Map.Entry<String, Endpoint>> endpointIterator = service.getEndpoints().entrySet().iterator();
                while (endpointIterator.hasNext()) {
                    Map.Entry<String, Endpoint> entry = endpointIterator.next();
                    String endpointName = entry.getKey();
                    Endpoint endpoint = entry.getValue();

                    if (!RegionValidationUtil.validRegion(endpointName, partition.getRegionRegex()) ||
                        !RegionValidationUtil.validEndpoint(endpointName, endpoint)) {
                        endpointIterator.remove();
                    }

                }
            });
        });
    }

    @Override
    public TypeSpec poetClass() {
        TypeName listOfRegions = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(regionBasePackage, "Region"));
        TypeName mapByServiceEndpointKey = ParameterizedTypeName.get(ClassName.get(Map.class),
                                                                     serviceEndpointKeyClass(),
                                                                     ClassName.get(String.class));
        TypeName mapByPair = ParameterizedTypeName.get(ClassName.get(Map.class),
                                                       byPartitionKeyClass(),
                                                       ClassName.get(String.class));
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
                       .addField(FieldSpec.builder(listOfRegions, "REGIONS")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(regionsField(partitions))
                                          .build())
                       .addField(FieldSpec.builder(listOfServicePartitionMetadata, "PARTITIONS")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(servicePartitions(partitions))
                                          .build())
                       .addField(FieldSpec.builder(mapByServiceEndpointKey, "SIGNING_REGIONS_BY_REGION")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(signingRegionsByRegion(partitions))
                                          .build())
                       .addField(FieldSpec.builder(mapByPair, "SIGNING_REGIONS_BY_PARTITION")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(signingRegionsByPartition(partitions))
                                          .build())
                       .addField(FieldSpec.builder(mapByServiceEndpointKey, "DNS_SUFFIXES_BY_REGION")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(dnsSuffixesByRegion(partitions))
                                          .build())
                       .addField(FieldSpec.builder(mapByPair, "DNS_SUFFIXES_BY_PARTITION")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(dnsSuffixesByPartition(partitions))
                                          .build())
                       .addField(FieldSpec.builder(mapByServiceEndpointKey, "HOSTNAMES_BY_REGION")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(hostnamesByRegion(partitions))
                                          .build())
                       .addField(FieldSpec.builder(mapByPair, "HOSTNAMES_BY_PARTITION")
                                          .addModifiers(PRIVATE, FINAL, STATIC)
                                          .initializer(hostnamesByPartition(partitions))
                                          .build())
                       .addMethod(regions())
                       .addMethod(partitions(listOfServicePartitionMetadata))
                       .addMethod(endpointFor())
                       .addMethod(signingRegion())
                       .build();
    }

    @Override
    public ClassName className() {
        String sanitizedServiceName = serviceEndpointPrefix.replace(".", "-");
        return ClassName.get(basePackage, Stream.of(sanitizedServiceName.split("-"))
                                                .map(Utils::capitalize)
                                                .collect(Collectors.joining()) + "ServiceMetadata");
    }

    private CodeBlock signingRegionsByRegion(Partitions partitions) {
        Map<Partition, Service> services = getServiceData(partitions);

        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<$T, $T>builder()",
                                                            ImmutableMap.class, serviceEndpointKeyClass(), String.class);

        services.forEach((partition, service) -> {
            service.getEndpoints().forEach((region, endpoint) -> {
                if (endpoint.getCredentialScope() != null && endpoint.getCredentialScope().getRegion() != null) {
                    builder.add(".put(")
                           .add(serviceEndpointKey(region, emptyList()))
                           .add(", $S)\n", endpoint.getCredentialScope().getRegion());

                    endpoint.getVariants().forEach(variant -> {
                        builder.add(".put(")
                               .add(serviceEndpointKey(region, variant.getTags()))
                               .add(", $S)\n", endpoint.getCredentialScope().getRegion());
                    });
                }
            });
        });

        return builder.add(".build()").build();
    }

    private CodeBlock signingRegionsByPartition(Partitions partitions) {
        Map<Partition, Service> services = getServiceData(partitions);

        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<$T, $T>builder()",
                                                            ImmutableMap.class, byPartitionKeyClass(), String.class);

        services.forEach((partition, service) -> {
            Endpoint partitionDefaults = service.getDefaults();
            if (partitionDefaults != null &&
                partitionDefaults.getCredentialScope() != null &&
                partitionDefaults.getCredentialScope().getRegion() != null) {
                builder.add(".put($T.of($S, ", Pair.class, partition.getPartition())
                       .add(partitionEndpointKey(emptyList()))
                       .add("), $S)\n", partitionDefaults.getCredentialScope().getRegion());

                partitionDefaults.getVariants().forEach(variant -> {
                    builder.add(".put($T.of($S, ", Pair.class, partition.getPartition())
                           .add(partitionEndpointKey(variant.getTags()))
                           .add("), $S)\n", partitionDefaults.getCredentialScope().getRegion());
                });
            }
        });

        return builder.add(".build()").build();
    }

    private CodeBlock dnsSuffixesByRegion(Partitions partitions) {
        Map<Partition, Service> services = getServiceData(partitions);

        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<$T, $T>builder()",
                                                            ImmutableMap.class, serviceEndpointKeyClass(), String.class);

        services.forEach((partition, service) -> {
            service.getEndpoints().forEach((region, endpoint) -> {
                endpoint.getVariants().forEach(variant -> {
                    if (variant.getDnsSuffix() != null) {
                        builder.add(".put(")
                               .add(serviceEndpointKey(region, variant.getTags()))
                               .add(", $S)\n", variant.getDnsSuffix());
                    }
                });
            });
        });

        return builder.add(".build()").build();
    }

    private CodeBlock dnsSuffixesByPartition(Partitions partitions) {
        Map<Partition, Service> services = getServiceData(partitions);

        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<$T, $T>builder()",
                                                            ImmutableMap.class, byPartitionKeyClass(), String.class);

        services.forEach((partition, service) -> {
            if (service.getDefaults() != null) {
                service.getDefaults().getVariants().forEach(variant -> {
                    if (variant.getDnsSuffix() != null) {
                        builder.add(".put($T.of($S, ", Pair.class, partition.getPartition())
                               .add(partitionEndpointKey(variant.getTags()))
                               .add("), $S)\n", variant.getDnsSuffix());
                    }
                });
            }
        });

        return builder.add(".build()").build();
    }

    private CodeBlock hostnamesByRegion(Partitions partitions) {
        Map<Partition, Service> services = getServiceData(partitions);

        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<$T, $T>builder()",
                                                            ImmutableMap.class, serviceEndpointKeyClass(), String.class);

        services.forEach((partition, service) -> {
            service.getEndpoints().forEach((region, endpoint) -> {
                if (endpoint.getHostname() != null) {
                    builder.add(".put(")
                           .add(serviceEndpointKey(region, emptyList()))
                           .add(", $S)\n", endpoint.getHostname());
                }

                endpoint.getVariants().forEach(variant -> {
                    if (variant.getHostname() != null) {
                        builder.add(".put(")
                               .add(serviceEndpointKey(region, variant.getTags()))
                               .add(", $S)\n", variant.getHostname());
                    }
                });
            });
        });

        return builder.add(".build()").build();
    }

    private CodeBlock hostnamesByPartition(Partitions partitions) {
        Map<Partition, Service> services = getServiceData(partitions);

        CodeBlock.Builder builder = CodeBlock.builder().add("$T.<$T, $T>builder()",
                                                            ImmutableMap.class, byPartitionKeyClass(), String.class);

        services.forEach((partition, service) -> {
            if (service.getDefaults() != null) {
                if (service.getDefaults().getHostname() != null) {
                    builder.add(".put($T.of($S, ", Pair.class, partition.getPartition())
                           .add(partitionEndpointKey(emptyList()))
                           .add(", $S)\n", service.getDefaults().getHostname());
                }
                service.getDefaults().getVariants().forEach(variant -> {
                    if (variant.getHostname() != null) {
                        builder.add(".put($T.of($S, ", Pair.class, partition.getPartition())
                               .add(partitionEndpointKey(variant.getTags()))
                               .add("), $S)\n", variant.getHostname());
                    }
                });
            }
        });

        return builder.add(".build()").build();
    }

    private CodeBlock serviceEndpointKey(String region, Collection<String> tags) {
        Validate.paramNotBlank(region, "region");

        CodeBlock.Builder result = CodeBlock.builder();
        result.add("$T.builder()", serviceEndpointKeyClass())
              .add(".region($T.of($S))", regionClass(), region);

        if (!tags.isEmpty()) {
            CodeBlock tagsParameter = tags.stream()
                                          .map(tag -> CodeBlock.of("$T.of($S)", endpointTagClass(), tag))
                                          .collect(CodeBlock.joining(", "));

            result.add(".tags(").add(tagsParameter).add(")");
        }

        result.add(".build()");
        return result.build();
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

    private CodeBlock regionsField(Partitions partitions) {
        ClassName regionClass = ClassName.get(regionBasePackage, "Region");
        CodeBlock.Builder builder = CodeBlock.builder().add("$T.unmodifiableList($T.asList(", Collections.class, Arrays.class);

        ArrayList<String> regions = new ArrayList<>();

        partitions.getPartitions()
                  .stream()
                  .filter(p -> p.getServices().containsKey(serviceEndpointPrefix))
                  .forEach(p -> regions.addAll(p.getServices().get(serviceEndpointPrefix).getEndpoints().keySet()));

        for (int i = 0; i < regions.size(); i++) {
            builder.add("$T.of($S)", regionClass, regions.get(i));
            if (i != regions.size() - 1) {
                builder.add(",");
            }
        }

        return builder.add("))").build();
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
                         .addParameter(serviceEndpointKeyClass(), "key")
                         .addAnnotation(Override.class)
                         .returns(URI.class)
                         .addCode("return $T.endpointFor(", serviceMetadataUtilsClass())
                         .addCode("$T.hostname(key, HOSTNAMES_BY_REGION, HOSTNAMES_BY_PARTITION), ", serviceMetadataUtilsClass())
                         .addCode("ENDPOINT_PREFIX, ")
                         .addCode("key.region().id(), ")
                         .addCode("$T.dnsSuffix(key, DNS_SUFFIXES_BY_REGION, DNS_SUFFIXES_BY_PARTITION));",
                                  serviceMetadataUtilsClass())
                         .build();
    }

    private MethodSpec signingRegion() {
        return MethodSpec.methodBuilder("signingRegion")
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(serviceEndpointKeyClass(), "key")
                         .addAnnotation(Override.class)
                         .returns(ClassName.get(regionBasePackage, "Region"))
                         .addStatement("return $T.signingRegion(key, SIGNING_REGIONS_BY_REGION, SIGNING_REGIONS_BY_PARTITION)",
                                       serviceMetadataUtilsClass())
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

    private ClassName serviceEndpointKeyClass() {
        return ClassName.get(regionBasePackage, "ServiceEndpointKey");
    }

    private ClassName partitionEndpointKeyClass() {
        return ClassName.get(regionBasePackage, "PartitionEndpointKey");
    }

    private ClassName regionClass() {
        return ClassName.get(regionBasePackage, "Region");
    }

    private ClassName endpointTagClass() {
        return ClassName.get(regionBasePackage, "EndpointTag");
    }

    private TypeName byPartitionKeyClass() {
        return ParameterizedTypeName.get(ClassName.get(Pair.class),
                                         ClassName.get(String.class),
                                         partitionEndpointKeyClass());
    }

    private ClassName serviceMetadataUtilsClass() {
        return ClassName.get(regionBasePackage + ".internal.util", "ServiceMetadataUtils");
    }
}
