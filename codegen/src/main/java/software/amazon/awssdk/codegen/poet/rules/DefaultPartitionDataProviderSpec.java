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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.ClassLoaderHelper;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.Validate;

public class DefaultPartitionDataProviderSpec implements ClassSpec {
    private static final String PARTITIONS_FILE_CLASSPATH_LOCATION = "software/amazon/awssdk/global/partitions.json";

    // partitions
    private static final String VERSION = "version";
    private static final String PARTITIONS = "partitions";
    // partition
    private static final String ID = "id";
    private static final String REGION_REGEX = "regionRegex";
    private static final String REGIONS = "regions";
    private static final String OUTPUTS = "outputs";
    // outputs
    private static final String DNS_SUFFIX = "dnsSuffix";
    private static final String DUAL_STACK_DNS_SUFFIX = "dualStackDnsSuffix";
    private static final String SUPPORTS_FIPS = "supportsFIPS";
    private static final String SUPPORTS_DUAL_STACK = "supportsDualStack";
    private static final String IMPLICIT_GLOBAL_REGION = "implicitGlobalRegion";

    private final IntermediateModel model;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final ClassName partitionsClass;
    private final ClassName partitionClass;
    private final ClassName regionOverrideClass;
    private final ClassName outputsClass;

    public DefaultPartitionDataProviderSpec(IntermediateModel model) {
        this.model = model;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
        this.partitionsClass = endpointRulesSpecUtils.rulesRuntimeClassName("Partitions");
        this.partitionClass = endpointRulesSpecUtils.rulesRuntimeClassName("Partition");
        this.regionOverrideClass = endpointRulesSpecUtils.rulesRuntimeClassName("RegionOverride");
        this.outputsClass = endpointRulesSpecUtils.rulesRuntimeClassName("Outputs");
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = PoetUtils.createClassBuilder(className())
                                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                            .addAnnotation(SdkInternalApi.class)
                                            .addSuperinterface(
                                                endpointRulesSpecUtils.rulesRuntimeClassName("PartitionDataProvider"));

        builder.addField(jsonNodeParserField());
        builder.addType(lazyPartitionsContainer());
        builder.addMethod(loadPartitionsMethod());
        builder.addMethod(systemSettingPartitionsFileMethod());
        builder.addMethod(classpathPartitionsFileMethod());
        builder.addMethod(readPartitionsFileMethod());
        return builder.build();
    }

    private FieldSpec jsonNodeParserField() {
        return FieldSpec.builder(JsonNodeParser.class, "PARSER", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.create()", JsonNodeParser.class)
                        .build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.rulesRuntimeClassName("DefaultPartitionDataProvider");
    }

    private MethodSpec loadPartitionsMethod() {
        return MethodSpec.methodBuilder("loadPartitions")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC)
                         .returns(partitionsClass)
                         .addCode("return $T.firstPresent(systemSettingPartitionsFile(),", OptionalUtils.class)
                         .addCode("    this::classpathPartitionsFile,")
                         .addCode("    () -> Optional.of(LazyPartitionsContainer.PARTITIONS))")
                         .addCode(".orElseThrow(() -> new $T(\"Unable to find partition metadata.\"));",
                                  IllegalStateException.class)
                         .build();
    }

    private MethodSpec systemSettingPartitionsFileMethod() {
        return MethodSpec.methodBuilder("systemSettingPartitionsFile")
                         .addModifiers(Modifier.PRIVATE)
                         .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), partitionsClass))
                         .addCode("return $T.AWS_PARTITIONS_FILE.getStringValue()", SdkSystemSetting.class)
                         .addCode(".map($T::get)", Paths.class)
                         .addCode(".map(p -> $T.invokeSafely(() -> $T.newInputStream(p)))",
                                  FunctionalUtils.class, Files.class)
                         .addCode(".map(this::readPartitionsFile);")
                         .build();
    }

    private MethodSpec classpathPartitionsFileMethod() {
        return MethodSpec.methodBuilder("classpathPartitionsFile")
                         .addModifiers(Modifier.PRIVATE)
                         .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), partitionsClass))
                         .addCode("return $T.ofNullable($T.classLoader(getClass()).getResourceAsStream($S))",
                                  Optional.class, ClassLoaderHelper.class, PARTITIONS_FILE_CLASSPATH_LOCATION)
                         .addCode(".map(this::readPartitionsFile);")
                         .build();
    }

    private MethodSpec readPartitionsFileMethod() {
        return MethodSpec.methodBuilder("readPartitionsFile")
                         .addModifiers(Modifier.PRIVATE)
                         .returns(partitionsClass)
                         .addParameter(InputStream.class, "partitionsFile")
                         .addCode("try {")
                         .addCode("return $T.fromNode(PARSER.parse(partitionsFile));", partitionsClass)
                         .addCode("} finally {")
                         .addCode("$T.closeQuietly(partitionsFile, null);", IoUtils.class)
                         .addCode("}")
                         .build();
    }

    private TypeSpec lazyPartitionsContainer() {
        CodeBlock.Builder builder = CodeBlock.builder();
        JsonNode node = JsonNode.parser().parse(readPartitionsJson());
        codegenPartitions(builder, node);
        return TypeSpec.classBuilder("LazyPartitionsContainer")
                       .addModifiers(Modifier.STATIC)
                       .addField(FieldSpec.builder(partitionsClass, "PARTITIONS", Modifier.STATIC, Modifier.FINAL)
                                          .initializer(builder.build())
                                          .build())
                       .build();
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

    private void codegenPartitions(CodeBlock.Builder builder, JsonNode node) {
        builder.add("$T.builder()", partitionsClass);
        Map<String, JsonNode> objNode = node.asObject();

        JsonNode version = objNode.get(VERSION);
        if (version != null) {
            builder.add(".version(");
            builder.add("$S", version.asString());
            builder.add(")");
        }

        JsonNode partitions = objNode.get(PARTITIONS);
        if (partitions != null) {
            partitions.asArray().forEach(partNode -> {
                builder.add(".addPartition(");
                codegenPartition(builder, partNode);
                builder.add(")");
            });
        }
        builder.add(".build()");
    }

    private void codegenPartition(CodeBlock.Builder builder, JsonNode node) {
        builder.add("$T.builder()", partitionClass);
        Map<String, JsonNode> objNode = node.asObject();

        JsonNode id = objNode.get(ID);
        if (id != null) {
            builder.add(".id(");
            builder.add("$S", id.asString());
            builder.add(")");
        }

        JsonNode regionRegex = objNode.get(REGION_REGEX);
        if (regionRegex != null) {
            builder.add(".regionRegex(");
            builder.add("$S", regionRegex.asString());
            builder.add(")");
        }

        JsonNode regions = objNode.get(REGIONS);
        if (regions != null) {
            // At the moment `RegionOverride.fromNode` does nothing. We need to fix it here **and** if we keep the
            // loading from textual JSON also fix `RegionOverride.fromNode`.
            Map<String, JsonNode> regionsObj = regions.asObject();
            regionsObj.forEach((k, v) -> {
                builder.add(".putRegion($S, ", k);
                codegenRegionOverride(builder);
                builder.add(")");
            });
        }

        JsonNode outputs = objNode.get(OUTPUTS);
        if (outputs != null) {
            builder.add(".outputs(");
            codegenOutputs(builder, outputs);
            builder.add(")");
        }
        builder.add(".build()");
    }

    private void codegenRegionOverride(CodeBlock.Builder builder) {
        builder.add("$T.builder().build()", regionOverrideClass);
    }

    private void codegenOutputs(CodeBlock.Builder builder, JsonNode node) {
        builder.add("$T.builder()", outputsClass);
        Map<String, JsonNode> objNode = node.asObject();

        JsonNode dnsSuffix = objNode.get(DNS_SUFFIX);
        if (dnsSuffix != null) {
            builder.add(".dnsSuffix(");
            builder.add("$S", dnsSuffix.asString());
            builder.add(")");
        }

        JsonNode dualStackDnsSuffix = objNode.get(DUAL_STACK_DNS_SUFFIX);
        if (dualStackDnsSuffix != null) {
            builder.add(".dualStackDnsSuffix(");
            builder.add("$S", dualStackDnsSuffix.asString());
            builder.add(")");
        }

        JsonNode supportsFips = objNode.get(SUPPORTS_FIPS);
        if (supportsFips != null) {
            builder.add(".supportsFips(");
            builder.add("$L", supportsFips.asBoolean());
            builder.add(")");
        }

        JsonNode supportsDualStack = objNode.get(SUPPORTS_DUAL_STACK);
        if (supportsDualStack != null) {
            builder.add(".supportsDualStack(");
            builder.add("$L", supportsDualStack.asBoolean());
            builder.add(")");
        }

        JsonNode implicitGlobalRegion = objNode.get(IMPLICIT_GLOBAL_REGION);
        if (implicitGlobalRegion != null) {
            builder.add(".implicitGlobalRegion(");
            builder.add("$S", implicitGlobalRegion.asString());
            builder.add(")");
        }
        builder.add(".build()");
    }
}

