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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ListModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.SdkBytes;

/**
 * Helper class for resolving Poet {@link TypeName}s for use in model classes.
 */
public class TypeProvider {
    private final IntermediateModel intermediateModel;
    private final PoetExtensions poetExtensions;

    public TypeProvider(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.poetExtensions = new PoetExtensions(this.intermediateModel);
    }

    public ClassName listImplClassName() {
        return ClassName.get(ArrayList.class);
    }

    public boolean useAutoConstructLists() {
        return intermediateModel.getCustomizationConfig().isUseAutoConstructList();
    }

    public boolean useAutoConstructMaps() {
        return intermediateModel.getCustomizationConfig().isUseAutoConstructMap();
    }

    public TypeName enumReturnType(MemberModel memberModel) {
        return fieldType(memberModel, true);
    }

    public TypeName returnType(MemberModel memberModel) {
        if (memberModel.getVariable().getVariableType().endsWith("SdkBytes")) {
            return TypeName.get(SdkBytes.class);
        }
        return fieldType(memberModel, false);
    }

    public TypeName fieldType(MemberModel memberModel) {
        return fieldType(memberModel, false);
    }

    private TypeName fieldType(MemberModel memberModel, boolean preserveEnumType) {
        if (memberModel.isSimple()) {
            boolean isEnumMember = memberModel.getEnumType() != null;
            return preserveEnumType && isEnumMember ? poetExtensions.getModelClass(memberModel.getEnumType())
                                                    : getTypeNameForSimpleType(memberModel.getVariable().getVariableType());
        } else if (memberModel.isList()) {
            TypeName elementType = fieldType(memberModel.getListModel().getListMemberModel(), preserveEnumType);
            return ParameterizedTypeName.get(ClassName.get(List.class), elementType);
        } else if (memberModel.isMap()) {
            TypeName keyType = fieldType(memberModel.getMapModel().getKeyModel(), preserveEnumType);
            TypeName valueType = fieldType(memberModel.getMapModel().getValueModel(), preserveEnumType);
            return ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType);
        }
        return poetExtensions.getModelClass(memberModel.getC2jShape());
    }

    public TypeName parameterType(MemberModel memberModel) {
        return parameterType(memberModel, false);
    }

    public TypeName parameterType(MemberModel memberModel, boolean preserveEnum) {
        if (memberModel.isList()) {
            return listParameterType(memberModel.getListModel(), preserveEnum);
        }

        if (memberModel.isMap()) {
            MapModel mapModel = memberModel.getMapModel();
            TypeName keyType = mapKeyParameterType(mapModel, preserveEnum);
            TypeName valueType = mapValueParameterType(mapModel, preserveEnum);

            return ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType);
        }

        return fieldType(memberModel, preserveEnum);
    }

    public TypeName mapEntryWithConcreteTypes(MapModel mapModel) {
        TypeName keyType = fieldType(mapModel.getKeyModel());
        TypeName valueType = fieldType(mapModel.getValueModel());

        return ParameterizedTypeName.get(ClassName.get(Map.Entry.class), keyType, valueType);
    }

    public TypeName getTypeNameForSimpleType(String simpleType) {
        return Stream.of(String.class,
                Boolean.class,
                Integer.class,
                Long.class,
                Short.class,
                Byte.class,
                BigInteger.class,
                Double.class,
                Float.class,
                BigDecimal.class,
                SdkBytes.class,
                InputStream.class,
                Instant.class)
                .filter(cls -> cls.getName().equals(simpleType) || cls.getSimpleName().equals(simpleType))
                .map(ClassName::get)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported simple fieldType " + simpleType));
    }

    public FieldSpec asField(MemberModel memberModel, Modifier... modifiers) {
        FieldSpec.Builder builder = FieldSpec.builder(fieldType(memberModel),
                memberModel.getVariable().getVariableName());

        if (modifiers != null) {
            builder.addModifiers(modifiers);
        }

        return builder.build();
    }

    private TypeName listParameterType(ListModel listModel, boolean preserveEnum) {
        MemberModel elementModel = listModel.getListMemberModel();
        TypeName listElementType = parameterType(elementModel, preserveEnum);
        if (isContainerType(elementModel)) {
            listElementType = WildcardTypeName.subtypeOf(listElementType);
        }
        return ParameterizedTypeName.get(ClassName.get(Collection.class), listElementType);
    }

    private TypeName mapKeyParameterType(MapModel mapModel, boolean preserveEnum) {
        TypeName keyType;
        MemberModel keyModel = mapModel.getKeyModel();
        if (mapModel.getKeyModel().isSimple()) {
            if (preserveEnum && keyModel.getEnumType() != null) {
                keyType = poetExtensions.getModelClass(keyModel.getEnumType());
            } else {
                keyType = getTypeNameForSimpleType(keyModel.getVariable().getVariableType());
            }
        } else {
            keyType = parameterType(keyModel, preserveEnum);

        }

        return keyType;
    }

    private TypeName mapValueParameterType(MapModel mapModel, boolean preserveEnum) {
        TypeName valueType = parameterType(mapModel.getValueModel(), preserveEnum);
        if (mapModel.getValueModel().isList()) {
            valueType = WildcardTypeName.subtypeOf(valueType);
        }

        return valueType;
    }

    private static boolean isContainerType(MemberModel m) {
        return m.isList() || m.isMap();
    }
}
