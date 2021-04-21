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
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

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
                .filter(m -> !canCopyReference(m))
                .map(m -> new MemberCopierSpec(m, this, typeProvider, poetExtensions))
                .forEach(spec -> memberSpecs.put(spec.className(), spec));

        return memberSpecs.values();
    }

    public Optional<ClassName> copierClassFor(MemberModel memberModel) {
        if (canCopyReference(memberModel)) {
            return Optional.empty();
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

    public String stringToEnumCopyMethodName() {
        return "copyStringToEnum";
    }

    public String copyFromBuilderMethodName() {
        return "copyFromBuilder";
    }

    public String copyToBuilderMethodName() {
        return "copyToBuilder";
    }

    private Map<String, MemberModel> allShapeMembers() {
        Map<String, MemberModel> shapeMembers = new HashMap<>();
        intermediateModel.getShapes().values().stream()
                .flatMap(s -> s.getMembersAsMap().values().stream())
                .forEach(m -> shapeMembers.put(m.getC2jShape(), m));
        return shapeMembers;
    }

    private boolean canCopyReference(MemberModel m) {
        return !m.isList() && !m.isMap();
    }
}
