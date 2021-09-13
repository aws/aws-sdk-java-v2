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

import static software.amazon.awssdk.codegen.poet.model.TypeProvider.ShapeTransformation.NONE;
import static software.amazon.awssdk.codegen.poet.model.TypeProvider.ShapeTransformation.USE_BUILDER;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.StaticImport;
import software.amazon.awssdk.codegen.poet.model.TypeProvider.TypeNameOptions;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

class MemberCopierSpec implements ClassSpec {
    private final MemberModel memberModel;
    private final ServiceModelCopiers serviceModelCopiers;
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;

    private enum EnumTransform {
        /** Copy enums as strings */
        STRING_TO_ENUM,
        /** Copy strings as enums */
        ENUM_TO_STRING,
        /** Copy without a transformation */
        NONE
    }

    private enum BuilderTransform {
        BUILDER_TO_BUILDABLE,
        BUILDABLE_TO_BUILDER,
        NONE
    }

    MemberCopierSpec(MemberModel memberModel,
                     ServiceModelCopiers serviceModelCopiers,
                     TypeProvider typeProvider,
                     PoetExtensions poetExtensions) {
        this.memberModel = memberModel;
        this.serviceModelCopiers = serviceModelCopiers;
        this.typeProvider = typeProvider;
        this.poetExtensions = poetExtensions;
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(className())
                .addModifiers(Modifier.FINAL)
                .addAnnotation(PoetUtils.generatedAnnotation())
                .addMethod(copyMethod());

        if (memberModel.containsBuildable()) {
            builder.addMethod(copyFromBuilderMethod());
            builder.addMethod(copyToBuilderMethod());
        }

        // If this is a collection, and it contains enums, or recursively
        // contains enums, add extra methods for copying the elements from an
        // enum to string and vice versa
        if (isEnumCopyAvailable(memberModel)) {
            builder.addMethod(enumToStringCopyMethod());
            builder.addMethod(stringToEnumCopyMethod());
        }

        return builder.build();
    }

    @Override
    public ClassName className() {
        return serviceModelCopiers.copierClassFor(memberModel).get();
    }

    @Override
    public Iterable<StaticImport> staticImports() {
        if (memberModel.isList()) {
            return Collections.singletonList(StaticImport.staticMethodImport(Collectors.class, "toList"));
        }

        if (memberModel.isMap()) {
            return Collections.singletonList(StaticImport.staticMethodImport(Collectors.class, "toMap"));
        }

        return Collections.emptyList();
    }

    public static boolean isEnumCopyAvailable(MemberModel memberModel) {
        if (!(memberModel.isMap() || memberModel.isList())) {
            return false;
        }

        if (memberModel.isMap()) {
            MapModel mapModel = memberModel.getMapModel();
            MemberModel keyModel = mapModel.getKeyModel();
            MemberModel valueModel = mapModel.getValueModel();
            if (keyModel.getEnumType() != null || valueModel.getEnumType() != null) {
                return true;
            }

            if (valueModel.isList() || valueModel.isMap()) {
                return isEnumCopyAvailable(valueModel);
            }
            // Keys are always simple, don't need to check
        } else {
            MemberModel element = memberModel.getListModel().getListMemberModel();
            if (element.getEnumType() != null) {
                return true;
            }
            if (element.isList() || element.isMap()) {
                return isEnumCopyAvailable(element);
            }
        }

        return false;
    }

    private MethodSpec copyMethod() {
        return MethodSpec.methodBuilder(serviceModelCopiers.copyMethodName())
                         .addModifiers(Modifier.STATIC)
                         .addParameter(typeName(memberModel, true, true, BuilderTransform.NONE, EnumTransform.NONE),
                                       memberParamName())
                         .returns(typeName(memberModel, false, false, BuilderTransform.NONE, EnumTransform.NONE))
                         .addCode(copyMethodBody(BuilderTransform.NONE, EnumTransform.NONE))
                         .build();
    }

    private MethodSpec enumToStringCopyMethod() {
        return MethodSpec.methodBuilder(serviceModelCopiers.enumToStringCopyMethodName())
                         .addModifiers(Modifier.STATIC)
                         .addParameter(typeName(memberModel, true, true, BuilderTransform.NONE, EnumTransform.ENUM_TO_STRING),
                                       memberParamName())
                         .returns(typeName(memberModel, false, false, BuilderTransform.NONE, EnumTransform.ENUM_TO_STRING))
                         .addCode(copyMethodBody(BuilderTransform.NONE, EnumTransform.ENUM_TO_STRING))
                         .build();
    }

    private MethodSpec stringToEnumCopyMethod() {
        return MethodSpec.methodBuilder(serviceModelCopiers.stringToEnumCopyMethodName())
                         .addModifiers(Modifier.STATIC)
                         .addParameter(typeName(memberModel, true, true, BuilderTransform.NONE, EnumTransform.STRING_TO_ENUM),
                                       memberParamName())
                         .returns(typeName(memberModel, false, false, BuilderTransform.NONE, EnumTransform.STRING_TO_ENUM))
                         .addCode(copyMethodBody(BuilderTransform.NONE, EnumTransform.STRING_TO_ENUM))
                         .build();
    }

    private MethodSpec copyFromBuilderMethod() {
        return MethodSpec.methodBuilder(serviceModelCopiers.copyFromBuilderMethodName())
                         .addModifiers(Modifier.STATIC)
                         .returns(typeName(memberModel, false, false, BuilderTransform.BUILDER_TO_BUILDABLE,
                                           EnumTransform.NONE))
                         .addParameter(typeName(memberModel, true, true, BuilderTransform.BUILDER_TO_BUILDABLE,
                                                EnumTransform.NONE),
                                       memberParamName())
                         .addCode(copyMethodBody(BuilderTransform.BUILDER_TO_BUILDABLE, EnumTransform.NONE))
                         .build();
    }

    private MethodSpec copyToBuilderMethod() {
        return MethodSpec.methodBuilder(serviceModelCopiers.copyToBuilderMethodName())
                         .addModifiers(Modifier.STATIC)
                         .returns(typeName(memberModel, false, false, BuilderTransform.BUILDABLE_TO_BUILDER, EnumTransform.NONE))
                         .addParameter(typeName(memberModel, true, true, BuilderTransform.BUILDABLE_TO_BUILDER,
                                                EnumTransform.NONE),
                                       memberParamName())
                         .addCode(copyMethodBody(BuilderTransform.BUILDABLE_TO_BUILDER, EnumTransform.NONE))
                         .build();
    }

    private CodeBlock copyMethodBody(BuilderTransform builderTransform, EnumTransform enumTransform) {
        CodeBlock.Builder code = CodeBlock.builder();

        if (!memberModel.getAutoConstructClassIfExists().isPresent()) {
            code.add("if ($N == null) {", memberParamName())
                .add("return null;")
                .add("}");
        }

        String outputVariable = copyMethodBody(code, builderTransform, new UniqueVariableSource(),
                                               enumTransform, memberParamName(), memberModel);

        code.add("return $N;", outputVariable);

        return code.build();
    }

    private String copyMethodBody(CodeBlock.Builder code, BuilderTransform builderTransform, UniqueVariableSource variableSource,
                                  EnumTransform enumTransform, String inputVariableName, MemberModel inputMember) {
        if (inputMember.getEnumType() != null) {
            String outputVariableName = variableSource.getNew("result");
            ClassName enumType = poetExtensions.getModelClass(inputMember.getEnumType());
            switch (enumTransform) {
                case NONE:
                    return inputVariableName;
                case ENUM_TO_STRING:
                    code.add("$T $N = $N.toString();", String.class, outputVariableName, inputVariableName);
                    return outputVariableName;
                case STRING_TO_ENUM:
                    code.add("$1T $2N = $1T.fromValue($3N);", enumType, outputVariableName, inputVariableName);
                    return outputVariableName;
                default:
                    throw new IllegalStateException();
            }
        }

        if (inputMember.isSimple()) {
            return inputVariableName;
        }

        if (inputMember.hasBuilder()) {
            switch (builderTransform) {
                case NONE:
                    return inputVariableName;
                case BUILDER_TO_BUILDABLE:
                    String buildableOutput = variableSource.getNew("member");
                    TypeName buildableOutputType = typeName(inputMember, false, false, builderTransform, enumTransform);
                    code.add("$T $N = $N.build();", buildableOutputType, buildableOutput, inputVariableName);
                    return buildableOutput;
                case BUILDABLE_TO_BUILDER:
                    String builderOutput = variableSource.getNew("member");
                    TypeName builderOutputType = typeName(inputMember, false, false, builderTransform, enumTransform);
                    code.add("$T $N = $N.toBuilder();", builderOutputType, builderOutput, inputVariableName);
                    return builderOutput;
                default:
                    throw new IllegalStateException();
            }
        }

        if (inputMember.isList()) {
            String outputVariableName = variableSource.getNew("list");
            String modifiableVariableName = variableSource.getNew("modifiableList");

            MemberModel listEntryModel = inputMember.getListModel().getListMemberModel();
            TypeName listType = typeName(inputMember, false, false, builderTransform, enumTransform);

            code.add("$T $N;", listType, outputVariableName)
                .add("if ($1N == null || $1N instanceof $2T) {", inputVariableName, SdkAutoConstructList.class)
                .add("$N = $T.getInstance();", outputVariableName, DefaultSdkAutoConstructList.class)
                .add("} else {")
                .add("$T $N = new $T<>();", listType, modifiableVariableName, ArrayList.class);

            String entryInputVariable = variableSource.getNew("entry");
            code.add("$N.forEach($N -> {", inputVariableName, entryInputVariable);

            String entryOutputVariable =
                copyMethodBody(code, builderTransform, variableSource, enumTransform, entryInputVariable, listEntryModel);

            code.add("$N.add($N);", modifiableVariableName, entryOutputVariable)
                .add("});")
                .add("$N = $T.unmodifiableList($N);", outputVariableName, Collections.class, modifiableVariableName)
                .add("}");

            return outputVariableName;
        }

        if (inputMember.isMap()) {
            String outputVariableName = variableSource.getNew("map");
            String modifiableVariableName = variableSource.getNew("modifiableMap");

            MemberModel keyModel = inputMember.getMapModel().getKeyModel();
            MemberModel valueModel = inputMember.getMapModel().getValueModel();
            TypeName keyType = typeName(keyModel, false, false, builderTransform, enumTransform);
            TypeName outputMapType = typeName(inputMember, false, false, builderTransform, enumTransform);

            code.add("$T $N;", outputMapType, outputVariableName)
                .add("if ($1N == null || $1N instanceof $2T) {", inputVariableName, SdkAutoConstructMap.class)
                .add("$N = $T.getInstance();", outputVariableName, DefaultSdkAutoConstructMap.class)
                .add("} else {")
                .add("$T $N = new $T<>();", outputMapType, modifiableVariableName, LinkedHashMap.class);

            String keyInputVariable = variableSource.getNew("key");
            String valueInputVariable = variableSource.getNew("value");
            code.add("$N.forEach(($N, $N) -> {", inputVariableName, keyInputVariable, valueInputVariable);

            String keyOutputVariable =
                copyMethodBody(code, builderTransform, variableSource, enumTransform, keyInputVariable, keyModel);

            String valueOutputVariable =
                copyMethodBody(code, builderTransform, variableSource, enumTransform, valueInputVariable, valueModel);

            if (keyModel.getEnumType() != null && !keyType.toString().equals("java.lang.String")) {
                // When enums are used as keys, drop any entries with unknown keys
                code.add("if ($N != $T.UNKNOWN_TO_SDK_VERSION) {", keyOutputVariable, keyType)
                    .add("$N.put($N, $N);", modifiableVariableName, keyOutputVariable, valueOutputVariable)
                    .add("}");
            } else {
                code.add("$N.put($N, $N);", modifiableVariableName, keyOutputVariable, valueOutputVariable);
            }

            code.add("});")
                .add("$N = $T.unmodifiableMap($N);", outputVariableName, Collections.class, modifiableVariableName)
                .add("}");

            return outputVariableName;
        }

        throw new UnsupportedOperationException("Unable to generate copier for member '" + inputMember + "'");
    }

    private TypeName typeName(MemberModel model, boolean isInputType, boolean useCollectionForList,
                              BuilderTransform builderTransform, EnumTransform enumTransform) {

        boolean useEnumTypes = (isInputType && enumTransform == EnumTransform.ENUM_TO_STRING) ||
                               (!isInputType && enumTransform == EnumTransform.STRING_TO_ENUM);

        boolean useBuilderTypes = (isInputType && builderTransform == BuilderTransform.BUILDER_TO_BUILDABLE) ||
                                  (!isInputType && builderTransform == BuilderTransform.BUILDABLE_TO_BUILDER);

        return typeProvider.typeName(model, new TypeNameOptions().useEnumTypes(useEnumTypes)
                                                                 .shapeTransformation(useBuilderTypes ? USE_BUILDER : NONE)
                                                                 .useSubtypeWildcardsForCollections(isInputType)
                                                                 .useSubtypeWildcardsForBuilders(isInputType)
                                                                 .useCollectionForList(useCollectionForList));
    }

    private String memberParamName() {
        if (memberModel.isSimple()) {
            return Utils.unCapitalize(memberModel.getVariable().getSimpleType()) + "Param";
        }
        return Utils.unCapitalize(memberModel.getC2jShape()) + "Param";
    }

    private static final class UniqueVariableSource {
        private final Map<String, Integer> suffixes = new HashMap<>();

        private String getNew(String prefix) {
            return prefix + suffix(prefix);
        }

        private String suffix(String prefix) {
            Integer suffixNumber = suffixes.compute(prefix, (k, v) -> v == null ? 0 : v + 1);
            return suffixNumber == 0 ? "" : suffixNumber.toString();
        }
    }
}
