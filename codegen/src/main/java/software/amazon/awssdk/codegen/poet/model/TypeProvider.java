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
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.document.Document;

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

    public TypeName enumReturnType(MemberModel memberModel) {
        return typeName(memberModel, new TypeNameOptions().useEnumTypes(true));
    }

    public TypeName returnType(MemberModel memberModel) {
        return typeName(memberModel, new TypeNameOptions().useEnumTypes(false));
    }

    public TypeName fieldType(MemberModel memberModel) {
        return typeName(memberModel, new TypeNameOptions().useEnumTypes(false));
    }

    public TypeName parameterType(MemberModel memberModel) {
        return parameterType(memberModel, false);
    }

    public TypeName parameterType(MemberModel memberModel, boolean preserveEnum) {
        return typeName(memberModel, new TypeNameOptions().useCollectionForList(true)
                                                          .useSubtypeWildcardsForCollections(true)
                                                          .useEnumTypes(preserveEnum));
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
                Instant.class,
                Document.class)
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

    private static boolean isContainerType(MemberModel m) {
        return m.isList() || m.isMap();
    }

    public TypeName typeName(MemberModel model) {
        return typeName(model, new TypeNameOptions());
    }

    public TypeName typeName(MemberModel model, TypeNameOptions options) {
        if (model.isSdkBytesType() && options.useByteBufferTypes) {
            return ClassName.get(ByteBuffer.class);
        }

        if (model.getEnumType() != null && options.useEnumTypes) {
            return poetExtensions.getModelClass(model.getEnumType());
        }

        if (model.isSimple()) {
            return getTypeNameForSimpleType(model.getVariable().getVariableType());
        }

        if (model.isList()) {
            MemberModel entryModel = model.getListModel().getListMemberModel();
            TypeName entryType = typeName(entryModel, options);

            if (options.useSubtypeWildcardsForCollections && isContainerType(entryModel) ||
                options.useSubtypeWildcardsForBuilders && entryModel.hasBuilder()) {
                entryType = WildcardTypeName.subtypeOf(entryType);
            }

            Class<?> collectionType = options.useCollectionForList ? Collection.class : List.class;

            return ParameterizedTypeName.get(ClassName.get(collectionType), entryType);
        }

        if (model.isMap()) {
            MemberModel keyModel = model.getMapModel().getKeyModel();
            MemberModel valueModel = model.getMapModel().getValueModel();
            TypeName keyType = typeName(keyModel, options);
            TypeName valueType = typeName(valueModel, options);

            if (options.useSubtypeWildcardsForCollections && isContainerType(keyModel) ||
                options.useSubtypeWildcardsForBuilders && keyModel.hasBuilder()) {
                keyType = WildcardTypeName.subtypeOf(keyType);
            }

            if (options.useSubtypeWildcardsForCollections && isContainerType(valueModel) ||
                options.useSubtypeWildcardsForBuilders && valueModel.hasBuilder()) {
                valueType = WildcardTypeName.subtypeOf(valueType);
            }

            return ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType);
        }

        if (model.hasBuilder()) {
            ClassName shapeClass = poetExtensions.getModelClass(model.getC2jShape());
            switch (options.shapeTransformation) {
                case NONE: return shapeClass;
                case USE_BUILDER: return shapeClass.nestedClass("Builder");
                case USE_BUILDER_IMPL: return shapeClass.nestedClass("BuilderImpl");
                default: throw new IllegalStateException();
            }
        }

        throw new IllegalArgumentException("Unsupported member model: " + model);
    }

    public enum ShapeTransformation {
        USE_BUILDER,
        USE_BUILDER_IMPL,
        NONE
    }

    public static final class TypeNameOptions {
        private ShapeTransformation shapeTransformation = ShapeTransformation.NONE;
        private boolean useCollectionForList = false;
        private boolean useSubtypeWildcardsForCollections = false;
        private boolean useByteBufferTypes = false;
        private boolean useEnumTypes = false;
        private boolean useSubtypeWildcardsForBuilders = false;

        public TypeNameOptions shapeTransformation(ShapeTransformation shapeTransformation) {
            this.shapeTransformation = shapeTransformation;
            return this;
        }

        public TypeNameOptions useCollectionForList(boolean useCollectionForList) {
            this.useCollectionForList = useCollectionForList;
            return this;
        }

        public TypeNameOptions useSubtypeWildcardsForCollections(boolean useSubtypeWildcardsForCollections) {
            this.useSubtypeWildcardsForCollections = useSubtypeWildcardsForCollections;
            return this;
        }

        public TypeNameOptions useSubtypeWildcardsForBuilders(boolean useSubtypeWildcardsForBuilders) {
            this.useSubtypeWildcardsForBuilders = useSubtypeWildcardsForBuilders;
            return this;
        }

        public TypeNameOptions useEnumTypes(boolean useEnumTypes) {
            this.useEnumTypes = useEnumTypes;
            return this;
        }

        public TypeNameOptions useByteBufferTypes(boolean useByteBufferTypes) {
            this.useByteBufferTypes = useByteBufferTypes;
            return this;
        }
    }
}
