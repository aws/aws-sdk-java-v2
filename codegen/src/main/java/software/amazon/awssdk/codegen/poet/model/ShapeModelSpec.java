/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.protocol.SdkField;
import software.amazon.awssdk.core.protocol.traits.DefaultValueTrait;
import software.amazon.awssdk.core.protocol.traits.JsonValueTrait;
import software.amazon.awssdk.core.protocol.traits.ListTrait;
import software.amazon.awssdk.core.protocol.traits.LocationTrait;
import software.amazon.awssdk.core.protocol.traits.MapTrait;
import software.amazon.awssdk.core.protocol.traits.PayloadTrait;
import software.amazon.awssdk.core.protocol.traits.TimestampFormatTrait;

/**
 * Provides Poet specs related to shape models.
 */
class ShapeModelSpec {

    private final ShapeModel shapeModel;
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;
    private final NamingStrategy namingStrategy;
    private final CustomizationConfig customizationConfig;

    ShapeModelSpec(ShapeModel shapeModel,
                   TypeProvider typeProvider,
                   PoetExtensions poetExtensions,
                   NamingStrategy namingStrategy,
                   CustomizationConfig customizationConfig) {
        this.shapeModel = shapeModel;
        this.typeProvider = typeProvider;
        this.poetExtensions = poetExtensions;
        this.namingStrategy = namingStrategy;
        this.customizationConfig = customizationConfig;
    }

    ClassName className() {
        return poetExtensions.getModelClass(shapeModel.getShapeName());
    }

    public List<FieldSpec> fields() {
        return fields(Modifier.PRIVATE, Modifier.FINAL);
    }

    public List<FieldSpec> fields(Modifier... modifiers) {
        return shapeModel.getNonStreamingMembers().stream()
                         // Exceptions can be members of event stream shapes, need to filter those out of the models
                         .filter(m -> m.getShape() == null || m.getShape().getShapeType() != ShapeType.Exception)
                         .map(m -> typeProvider.asField(m, modifiers))
                         .collect(Collectors.toList());
    }

    public Iterable<FieldSpec> staticFields(Modifier... modifiers) {
        List<FieldSpec> fields = new ArrayList<>();
        shapeModel.getNonStreamingMembers().stream()
                  // Exceptions can be members of event stream shapes, need to filter those out of the models
                  .filter(m -> m.getShape() == null || m.getShape().getShapeType() != ShapeType.Exception)
                  .forEach(m -> {
                      FieldSpec field = typeProvider.asField(m, modifiers);
                      ClassName sdkFieldType = ClassName.get(SdkField.class);
                      fields.add(FieldSpec.builder(ParameterizedTypeName.get(sdkFieldType, field.type),
                                                   namingStrategy.getSdkFieldFieldName(m),
                                                   Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                          .initializer(sdkFieldInitializer(m))
                                          .build());
                  });

        ParameterizedTypeName sdkFieldType = ParameterizedTypeName.get(ClassName.get(SdkField.class),
                                                                       WildcardTypeName.subtypeOf(ClassName.get(Object.class)));

        fields.add(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class),
                                                               sdkFieldType), "SDK_FIELDS",
                                     Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$T.unmodifiableList($T.asList($L))",
                                         ClassName.get(Collections.class),
                                         ClassName.get(Arrays.class),
                                         fields.stream()
                                               .map(f -> f.name)
                                               .collect(Collectors.joining(",")))
                            .build());
        return fields;
    }

    private CodeBlock sdkFieldInitializer(MemberModel m) {
        ClassName sdkFieldType = ClassName.get(SdkField.class);
        return CodeBlock.builder()
                        .add("$T.<$T>builder($T.$L)\n",
                             sdkFieldType, typeProvider.fieldType(m),
                             ClassName.get(MarshallingType.class), m.getMarshallingType())
                        .add(".getter(getter($T::$L))\n",
                             className(), m.getFluentGetterMethodName())
                        .add(".setter(setter($T::$L))\n",
                             className().nestedClass("Builder"), m.getFluentSetterMethodName())
                        .add(constructor(m))
                        .add(traits(m))
                        .add(".build()")
                        .build();
    }

    private CodeBlock containerSdkFieldInitializer(MemberModel m) {
        ClassName sdkFieldType = ClassName.get(SdkField.class);
        return CodeBlock.builder()
                        .add("$T.<$T>builder($T.$L)\n",
                             sdkFieldType, typeProvider.fieldType(m),
                             ClassName.get(MarshallingType.class), m.getMarshallingType())
                        .add(constructor(m))
                        .add(traits(m))
                        .add(".build()")
                        .build();
    }

    private CodeBlock traits(MemberModel m) {
        List<CodeBlock> traits = new ArrayList<>();

        traits.add(createLocationTrait(m));
        if (m.isList()) {
            traits.add(createListTrait(m));
        } else if (m.isMap()) {
            traits.add(createMapTrait(m));
        }
        if (m.getHttp().getIsPayload() || m.isEventPayload()) {
            traits.add(createPayloadTrait());
        }
        if (m.isJsonValue()) {
            traits.add(createJsonValueTrait());
        }
        if (m.isIdempotencyToken()) {
            traits.add(createIdempotencyTrait());
        }
        String customDefaultValueSupplier = customizationConfig.getModelMarshallerDefaultValueSupplier().get(m.getC2jName());
        if (customDefaultValueSupplier != null) {
            traits.add(createDefaultValueTrait(customDefaultValueSupplier));
        }
        if (m.getTimestampFormat() != null) {
            traits.add(createTimestampFormatTrait(m));
        }
        if (!traits.isEmpty()) {
            return CodeBlock.builder()
                            .add(".traits(" + traits.stream().map(t -> "$L").collect(Collectors.joining(", ")) + ")",
                                 traits.toArray())
                            .build();
        } else {
            return CodeBlock.builder().build();
        }
    }

    private CodeBlock createTimestampFormatTrait(MemberModel m) {
        TimestampFormatTrait.Format format = TimestampFormatTrait.Format.fromString(m.getTimestampFormat());
        ClassName traitClass = ClassName.get(TimestampFormatTrait.class);
        ClassName formatClass = ClassName.get(TimestampFormatTrait.Format.class);
        return CodeBlock.builder()
                        .add("$T.create($T.$L)", traitClass, formatClass, format.name())
                        .build();
    }

    private CodeBlock createLocationTrait(MemberModel m) {
        return CodeBlock.builder()
                        // TODO will marshall and unmarshall location name ever differ?
                        .add("$T.builder()\n"
                             + ".location($T.$L)"
                             + ".locationName($S)"
                             + ".build()", ClassName.get(LocationTrait.class), ClassName.get(MarshallLocation.class),
                             m.getHttp().getMarshallLocation(), m.getHttp().getMarshallLocationName())
                        .build();
    }

    private CodeBlock createIdempotencyTrait() {
        return CodeBlock.builder()
                        .add("$T.idempotencyToken()", ClassName.get(DefaultValueTrait.class))
                        .build();
    }

    private CodeBlock createDefaultValueTrait(String customDefaultValueSupplier) {
        return CodeBlock.builder()
                        .add("$T.create($T.getInstance())", ClassName.get(DefaultValueTrait.class),
                             ClassName.bestGuess(customDefaultValueSupplier))
                        .build();
    }

    private CodeBlock createJsonValueTrait() {
        return CodeBlock.builder()
                        .add("$T.create()", ClassName.get(JsonValueTrait.class))
                        .build();
    }

    private CodeBlock createPayloadTrait() {
        return CodeBlock.builder()
                        .add("$T.create()", ClassName.get(PayloadTrait.class))
                        .build();
    }

    private CodeBlock createMapTrait(MemberModel m) {
        return CodeBlock.builder()
                        .add("$T.builder()\n"
                             + ".keyLocationName($S)\n"
                             + ".valueLocationName($S)\n"
                             + ".valueFieldInfo($L)\n"
                             + ".build()", ClassName.get(MapTrait.class),
                             m.getMapModel().getKeyLocationName(),
                             m.getMapModel().getValueLocationName(),
                             containerSdkFieldInitializer(m.getMapModel().getValueModel()))
                        .build();
    }

    private CodeBlock createListTrait(MemberModel m) {
        return CodeBlock.builder()
                        .add("$T.builder()\n"
                             + ".memberLocationName($S)\n"
                             + ".memberFieldInfo($L)\n"
                             + ".build()", ClassName.get(ListTrait.class),
                             m.getListModel().getMemberLocationName(),
                             containerSdkFieldInitializer(m.getListModel().getListMemberModel()))
                        .build();
    }

    private CodeBlock constructor(MemberModel m) {
        if (!m.isSimple() && !m.isList() && !m.isMap()) {
            return CodeBlock.builder()
                            .add(".constructor($T::builder)\n", typeProvider.fieldType(m))
                            .build();
        } else {
            return CodeBlock.builder().build();
        }
    }

}
