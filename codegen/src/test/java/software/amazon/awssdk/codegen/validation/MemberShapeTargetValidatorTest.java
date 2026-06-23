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

package software.amazon.awssdk.codegen.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ListModel;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class MemberShapeTargetValidatorTest {

    @Test
    void validate_enumMemberWithDanglingTarget_throwsWithShapeMemberAndTarget() {
        String owningShape = "GetRequestAuthorizationDetailsResponse";
        MemberModel enumMember = new MemberModel()
            .withC2jName("AuthorizationDetails")
            .withC2jShape("AuthDetailType")
            .withVariable(new VariableModel("authorizationDetails", "String"))
            .withEnumType("AuthDetailType");
        IntermediateModel model = modelWithShape(owningShape, enumMember);

        assertThatThrownBy(() -> MemberShapeTargetValidator.validate(model))
            .isInstanceOf(ModelInvalidException.class)
            .matches(MemberShapeTargetValidatorTest::isUnknownShapeMemberDanger)
            .hasMessageContaining(owningShape)
            .hasMessageContaining("AuthorizationDetails")
            .hasMessageContaining("AuthDetailType");
    }

    @Test
    void validate_structureMemberWithDanglingTarget_throwsWithShapeMemberAndTarget() {
        String owningShape = "ContainerShape";
        MemberModel structureMember = new MemberModel()
            .withC2jName("Nested")
            .withC2jShape("MissingStruct")
            .withVariable(new VariableModel("nested", "MissingStruct"));
        IntermediateModel model = modelWithShape(owningShape, structureMember);

        assertThatThrownBy(() -> MemberShapeTargetValidator.validate(model))
            .isInstanceOf(ModelInvalidException.class)
            .matches(MemberShapeTargetValidatorTest::isUnknownShapeMemberDanger)
            .hasMessageContaining(owningShape)
            .hasMessageContaining("Nested")
            .hasMessageContaining("MissingStruct");
    }

    @Test
    void validate_listElementWithDanglingTarget_throwsIdentifyingContainerMember() {
        String owningShape = "ListContainerShape";
        MemberModel elementMember = new MemberModel()
            .withC2jName("member")
            .withC2jShape("MissingElement")
            .withVariable(new VariableModel("member", "MissingElement"));
        MemberModel listMember = new MemberModel()
            .withC2jName("Items")
            .withC2jShape("ItemList")
            .withVariable(new VariableModel("items", "java.util.List"))
            .withListModel(new ListModel("MissingElement", null, "java.util.ArrayList", "java.util.List", elementMember));
        IntermediateModel model = modelWithShape(owningShape, listMember);

        assertThatThrownBy(() -> MemberShapeTargetValidator.validate(model))
            .isInstanceOf(ModelInvalidException.class)
            .matches(MemberShapeTargetValidatorTest::isUnknownShapeMemberDanger)
            .hasMessageContaining(owningShape)
            .hasMessageContaining("Items")
            .hasMessageContaining("MissingElement");
    }

    @Test
    void validate_mapValueWithDanglingTarget_throwsIdentifyingContainerMember() {
        String owningShape = "MapContainerShape";
        MemberModel keyMember = new MemberModel()
            .withC2jName("key")
            .withC2jShape("String")
            .withVariable(new VariableModel("key", "String"));
        MemberModel valueMember = new MemberModel()
            .withC2jName("value")
            .withC2jShape("MissingValue")
            .withVariable(new VariableModel("value", "MissingValue"));
        MemberModel mapMember = new MemberModel()
            .withC2jName("Attributes")
            .withC2jShape("AttributeMap")
            .withVariable(new VariableModel("attributes", "java.util.Map"))
            .withMapModel(new MapModel("java.util.HashMap", "java.util.Map", "key", keyMember, "value", valueMember));
        IntermediateModel model = modelWithShape(owningShape, mapMember);

        assertThatThrownBy(() -> MemberShapeTargetValidator.validate(model))
            .isInstanceOf(ModelInvalidException.class)
            .matches(MemberShapeTargetValidatorTest::isUnknownShapeMemberDanger)
            .hasMessageContaining(owningShape)
            .hasMessageContaining("Attributes")
            .hasMessageContaining("MissingValue");
    }

    @Test
    void validate_multipleDanglingMembers_aggregatesAllIntoSingleException() {
        MemberModel enumMember = new MemberModel()
            .withC2jName("AuthorizationDetails")
            .withC2jShape("AuthDetailType")
            .withVariable(new VariableModel("authorizationDetails", "String"))
            .withEnumType("AuthDetailType");
        ShapeModel enumOwner = shape("ResponseShape");
        enumOwner.setMembers(Arrays.asList(enumMember));

        MemberModel structureMember = new MemberModel()
            .withC2jName("Nested")
            .withC2jShape("MissingStruct")
            .withVariable(new VariableModel("nested", "MissingStruct"));
        ShapeModel structOwner = shape("RequestShape");
        structOwner.setMembers(Arrays.asList(structureMember));

        Map<String, ShapeModel> shapes = new HashMap<>();
        shapes.put("ResponseShape", enumOwner);
        shapes.put("RequestShape", structOwner);
        IntermediateModel model = new IntermediateModel();
        model.setShapes(shapes);

        assertThatThrownBy(() -> MemberShapeTargetValidator.validate(model))
            .isInstanceOf(ModelInvalidException.class)
            .matches(t -> allUnknownShapeMemberDanger(t, 2), "two UNKNOWN_SHAPE_MEMBER / DANGER entries")
            .matches(t -> entryMessages(t).stream().anyMatch(m -> m.contains("ResponseShape")
                                                                  && m.contains("AuthorizationDetails")
                                                                  && m.contains("AuthDetailType")),
                     "entry for the enum member names its own shape/member/target")
            .matches(t -> entryMessages(t).stream().anyMatch(m -> m.contains("RequestShape")
                                                                  && m.contains("Nested")
                                                                  && m.contains("MissingStruct")),
                     "entry for the structure member names its own shape/member/target");
    }

    @Test
    void validate_mapKeyAndValueBothDangling_reportsDistinctEntryPerTarget() {
        MemberModel keyMember = new MemberModel()
            .withC2jName("key")
            .withC2jShape("MissingKey")
            .withVariable(new VariableModel("key", "MissingKey"));
        MemberModel valueMember = new MemberModel()
            .withC2jName("value")
            .withC2jShape("MissingValue")
            .withVariable(new VariableModel("value", "MissingValue"));
        MemberModel mapMember = new MemberModel()
            .withC2jName("Attributes")
            .withC2jShape("AttributeMap")
            .withVariable(new VariableModel("attributes", "java.util.Map"))
            .withMapModel(new MapModel("java.util.HashMap", "java.util.Map", "key", keyMember, "value", valueMember));
        IntermediateModel model = modelWithShape("MapContainerShape", mapMember);

        assertThatThrownBy(() -> MemberShapeTargetValidator.validate(model))
            .isInstanceOf(ModelInvalidException.class)
            .matches(t -> allUnknownShapeMemberDanger(t, 2), "two UNKNOWN_SHAPE_MEMBER / DANGER entries")
            .matches(t -> entryMessages(t).stream().anyMatch(m -> m.contains("MissingKey")), "key target reported")
            .matches(t -> entryMessages(t).stream().anyMatch(m -> m.contains("MissingValue")), "value target reported");
    }

    @Test
    void validate_sameTargetReachableTwice_reportedOnce() {
        MemberModel innerKey = new MemberModel()
            .withC2jName("key")
            .withC2jShape("MissingShared")
            .withVariable(new VariableModel("key", "MissingShared"));
        MemberModel innerValue = new MemberModel()
            .withC2jName("value")
            .withC2jShape("MissingShared")
            .withVariable(new VariableModel("value", "MissingShared"));
        MemberModel mapElement = new MemberModel()
            .withC2jName("member")
            .withC2jShape("SharedMap")
            .withVariable(new VariableModel("member", "java.util.Map"))
            .withMapModel(new MapModel("java.util.HashMap", "java.util.Map", "key", innerKey, "value", innerValue));
        MemberModel listMember = new MemberModel()
            .withC2jName("Items")
            .withC2jShape("ItemList")
            .withVariable(new VariableModel("items", "java.util.List"))
            .withListModel(new ListModel("java.util.Map", null, "java.util.ArrayList", "java.util.List", mapElement));
        IntermediateModel model = modelWithShape("ListOfMapShape", listMember);

        assertThatThrownBy(() -> MemberShapeTargetValidator.validate(model))
            .isInstanceOf(ModelInvalidException.class)
            .matches(t -> allUnknownShapeMemberDanger(t, 1), "single deduped UNKNOWN_SHAPE_MEMBER / DANGER entry")
            .matches(t -> entryMessages(t).get(0).contains("Items") && entryMessages(t).get(0).contains("MissingShared"),
                     "the single entry names the container member and shared target");
    }

    @Test
    void validate_scalarMembersWithNullShape_doesNotThrow() {
        MemberModel scalarMember = new MemberModel()
            .withC2jName("Name")
            .withC2jShape("String")
            .withVariable(new VariableModel("name", "String"));
        IntermediateModel model = modelWithShape("ScalarShape", scalarMember);

        assertThatCode(() -> MemberShapeTargetValidator.validate(model)).doesNotThrowAnyException();
    }

    @Test
    void validate_linkedEnumAndStructureMembers_doesNotThrow() {
        ShapeModel enumShape = shape("AuthDetailType");
        ShapeModel structShape = shape("NestedStruct");

        MemberModel enumMember = new MemberModel()
            .withC2jName("AuthorizationDetails")
            .withC2jShape("AuthDetailType")
            .withVariable(new VariableModel("authorizationDetails", "String"))
            .withEnumType("AuthDetailType");
        enumMember.setShape(enumShape);

        MemberModel structureMember = new MemberModel()
            .withC2jName("Nested")
            .withC2jShape("NestedStruct")
            .withVariable(new VariableModel("nested", "NestedStruct"));
        structureMember.setShape(structShape);

        ShapeModel owning = shape("OwningShape");
        owning.setMembers(Arrays.asList(enumMember, structureMember));

        Map<String, ShapeModel> shapes = new HashMap<>();
        shapes.put("OwningShape", owning);
        shapes.put("AuthDetailType", enumShape);
        shapes.put("NestedStruct", structShape);

        IntermediateModel model = new IntermediateModel();
        model.setShapes(shapes);

        assertThatCode(() -> MemberShapeTargetValidator.validate(model)).doesNotThrowAnyException();
    }

    @Test
    void validate_wellFormedAwsJsonModel_doesNotThrow() {
        assertThatCode(() -> MemberShapeTargetValidator.validate(ClientTestModels.awsJsonServiceModels()))
            .doesNotThrowAnyException();
    }

    private static IntermediateModel modelWithShape(String shapeName, MemberModel... members) {
        ShapeModel shape = shape(shapeName);
        shape.setMembers(Arrays.asList(members));

        Map<String, ShapeModel> shapes = new HashMap<>();
        shapes.put(shapeName, shape);

        IntermediateModel model = new IntermediateModel();
        model.setShapes(shapes);
        return model;
    }

    private static ShapeModel shape(String name) {
        ShapeModel shape = new ShapeModel(name);
        shape.setShapeName(name);
        shape.setMembers(Collections.emptyList());
        return shape;
    }

    private static boolean isUnknownShapeMemberDanger(Throwable t) {
        return allUnknownShapeMemberDanger(t, 1);
    }

    private static boolean allUnknownShapeMemberDanger(Throwable t, int expectedCount) {
        List<ValidationEntry> entries = ((ModelInvalidException) t).validationEntries();
        return entries.size() == expectedCount
               && entries.stream().allMatch(e -> e.getErrorId() == ValidationErrorId.UNKNOWN_SHAPE_MEMBER
                                                 && e.getSeverity() == ValidationErrorSeverity.DANGER);
    }

    private static List<String> entryMessages(Throwable t) {
        return ((ModelInvalidException) t).validationEntries().stream()
                                          .map(ValidationEntry::getDetailMessage)
                                          .collect(Collectors.toList());
    }
}
