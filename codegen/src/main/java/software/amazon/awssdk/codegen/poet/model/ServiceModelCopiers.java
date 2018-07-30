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
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.adapter.StandardMemberCopier;

public class ServiceModelCopiers {
    private final IntermediateModel intermediateModel;
    private final PoetExtensions poetExtensions;
    private final TypeProvider typeProvider;

    public ServiceModelCopiers(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.poetExtensions = new PoetExtensions(intermediateModel);
        this.typeProvider = new TypeProvider(intermediateModel);
    }

    public Collection<ClassSpec> copierSpecs() {
        Map<ClassName, ClassSpec> memberSpecs = new HashMap<>();
        allShapeMembers().values().stream()
                .filter(m -> !(canCopyReference(m) || canUseStandardCopier(m)))
                .map(m -> new MemberCopierSpec(m, this, typeProvider, poetExtensions))
                .forEach(spec -> memberSpecs.put(spec.className(), spec));

        return memberSpecs.values();
    }

    public boolean requiresBuilderCopier(MemberModel memberModel) {
        if (memberModel.isList()) {
            MemberModel type = memberModel.getListModel().getListMemberModel();
            return type != null && type.hasBuilder();
        }

        if (memberModel.isMap()) {
            MemberModel valueType = memberModel.getMapModel().getValueModel();
            return valueType != null && valueType.hasBuilder();
        }
        return false;
    }

    public Optional<ClassName> copierClassFor(MemberModel memberModel) {
        if (canCopyReference(memberModel)) {
            return Optional.empty();
        }

        if (canUseStandardCopier(memberModel)) {
            return Optional.of(ClassName.get(StandardMemberCopier.class));
        }

        // FIXME: Some services (Health) have shapes with names
        // that differ only in the casing of the first letter, and generating
        // classes for them breaks on case insensitive filesystems...
        String shapeName = memberModel.getC2jShape();
        if (shapeName.substring(0, 1).toLowerCase(Locale.ENGLISH).equals(shapeName.substring(0, 1))) {
            shapeName = "_" + shapeName;
        }

        return Optional.of(poetExtensions.getModelClass(shapeName + "Copier"));
    }

    public String copyMethodName() {
        return "copy";
    }

    public String enumToStringCopyMethodName() {
        return "copyEnumToString";
    }

    public String builderCopyMethodName() {
        return "copyFromBuilder";
    }

    private Map<String, MemberModel> allShapeMembers() {
        Map<String, MemberModel> shapeMembers = new HashMap<>();
        intermediateModel.getShapes().values().stream()
                .flatMap(s -> s.getMembersAsMap().values().stream())
                .forEach(m -> shapeMembers.put(m.getC2jShape(), m));

        // Step above only gives us the top level shape members.
        // List and Map members have members of their own, so find those too.
        Map<String, MemberModel> allMembers = new HashMap<>(shapeMembers);

        shapeMembers.values().forEach(m -> putMembersOfMember(m, allMembers));

        return allMembers;
    }

    private void putMembersOfMember(MemberModel memberModel, Map<String, MemberModel> allMembers) {
        if (memberModel.isList()) {
            MemberModel listMember = memberModel.getListModel().getListMemberModel();
            allMembers.put(listMember.getC2jShape(), listMember);
            putMembersOfMember(listMember, allMembers);
        } else if (memberModel.isMap()) {
            MapModel mapModel = memberModel.getMapModel();
            // NOTE: keys are always simple, so don't bother checking
            if (!mapModel.getValueModel().isSimple()) {
                MemberModel valueMember = mapModel.getValueModel();
                allMembers.put(valueMember.getC2jShape(), valueMember);
                putMembersOfMember(valueMember, allMembers);
            }
        }
    }

    private boolean canUseStandardCopier(MemberModel m) {
        if (m.isList() || m.isMap() || !m.isSimple()) {
            return false;
        }

        String simpleType = m.getVariable().getSimpleType();

        return "Date".equals(simpleType) || "SdkBytes".equals(simpleType);
    }

    private boolean canCopyReference(MemberModel m) {
        if (m.isList() || m.isMap()) {
            return false;
        }

        if (m.isSimple()) {
            String simpleType = m.getVariable().getSimpleType();
            switch (simpleType) {
                case "Date":
                case "SdkBytes":
                    return false;
                default:
                    return true;
            }
        }

        return true;
    }
}
