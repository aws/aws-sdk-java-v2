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

package software.amazon.awssdk.codegen.poet.crac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ParameterHttpMapping;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.VariableModel;
import software.amazon.awssdk.codegen.model.service.ContextParam;
import software.amazon.awssdk.codegen.model.service.Location;

public class WarmUpOperationSelectorTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("selectionScenarios")
    public void selectsExpectedOperation(Scenario scenario) {
        Optional<OperationModel> selected = WarmUpOperationSelector.selectWarmUpOperation(scenario.model());

        if (scenario.expected == null) {
            assertThat(selected).isEmpty();
        } else {
            assertThat(selected).map(OperationModel::getOperationName).contains(scenario.expected);
        }
    }

    private static Stream<Scenario> selectionScenarios() {
        return Stream.of(
            // Filter: streaming, event-stream and deprecated operations.
            scenario("streamingInputOperationOnly_isNotSelected")
                .operation(op("PutObject").withStreamingInput())
                .expectNothing(),
            scenario("streamingOutputOperationOnly_isNotSelected")
                .operation(op("GetObject").withStreamingOutput())
                .expectNothing(),
            scenario("eventStreamInputOperationOnly_isNotSelected")
                .operation(op("StartConversation").withEventStreamInput())
                .expectNothing(),
            scenario("eventStreamOutputOperationOnly_isNotSelected")
                .operation(op("SubscribeToShard").withEventStreamOutput())
                .expectNothing(),
            scenario("deprecatedOperationOnly_isNotSelected")
                .operation(op("OldListThings").deprecated())
                .expectNothing(),
            scenario("serviceWithNoOperations_selectsNothing")
                .expectNothing(),

            // Preference 1: returns output, so the unmarshaller is primed too. An operation with no output shape
            // (e.g. S3's DeleteBucket) can win every other tier and still lose here.
            scenario("hasOutput_beatsVoidOutput_whenOtherwiseEqual")
                .operation(op("ListThings").withOutput())
                .operation(op("ListOthers"))
                .expect("ListThings"),
            scenario("hasOutput_outranksVerifiedSimpleMethod")
                .verifiedSimpleMethods("listThings")
                .operation(op("ListThings"))
                .operation(op("ListOthers").withOutput())
                .expect("ListOthers"),
            scenario("hasOutput_outranksEmptyRequest")
                .operation(op("ListThings"))
                .operation(op("ListOthers").withOutput().withRequiredMembers(1))
                .expect("ListOthers"),
            scenario("hasOutput_outranksFewestRequiredMembers")
                .operation(op("ListThings").withRequiredMembers(1))
                .operation(op("ListOthers").withOutput().withRequiredMembers(2))
                .expect("ListOthers"),
            scenario("hasOutput_outranksPreferredVerb")
                .operation(op("ListThings"))
                .operation(op("DescribeThings").withOutput())
                .expect("DescribeThings"),
            scenario("hasOutput_outranksEveryOtherPreferenceCombined")
                .verifiedSimpleMethods("deleteThings")
                .operation(op("DeleteThings"))
                .operation(op("ListOthers").withOutput().withRequiredMembers(1))
                .expect("ListOthers"),

            // Preference 2: is authenticated, so signing is primed too. Both operations have output so tier 1 ties.
            scenario("authenticated_beatsNoAuth_whenOtherwiseEqual")
                .operation(op("ListThings").withOutput())
                .operation(op("ListOthers").withOutput().withNoAuth())
                .expect("ListThings"),
            scenario("authenticated_outranksVerifiedSimpleMethod")
                .verifiedSimpleMethods("listOthers")
                .operation(op("ListThings").withOutput())
                .operation(op("ListOthers").withOutput().withNoAuth())
                .expect("ListThings"),

            // Preference 3: verified simple method. Both operations have output and are authenticated, so tiers
            // 1-2 tie.
            scenario("verifiedSimpleMethod_beatsNonVerified_whenOtherwiseEqual")
                .verifiedSimpleMethods("listThings")
                .operation(op("ListThings").withOutput())
                .operation(op("ListOthers").withOutput())
                .expect("ListThings"),
            scenario("verifiedSimpleMethod_outranksEmptyRequest")
                .verifiedSimpleMethods("listThings")
                .operation(op("ListThings").withOutput().withRequiredMembers(1))
                .operation(op("ListOthers").withOutput())
                .expect("ListThings"),

            // Preference 4: accepts an empty request. Both operations have output and are authenticated, and
            // neither is verified simple, so tiers 1-3 tie.
            scenario("emptyRequest_beatsRequiredMembers")
                .operation(op("ListThings").withOutput())
                .operation(op("ListOthers").withOutput().withRequiredMembers(1))
                .expect("ListThings"),

            // Preference 5: fewest required input members. Both operations have output and require input, so
            // tiers 1-4 tie.
            scenario("fewerRequiredMembers_beatsMore_whenBothRequireInput")
                .operation(op("ListThings").withOutput().withRequiredMembers(1))
                .operation(op("ListOthers").withOutput().withRequiredMembers(2))
                .expect("ListThings"),

            // Preference 6: read-only verb, List > Describe > Get. Fixtures are chosen so the alphabetical
            // tie-break would pick the loser.
            scenario("verb_listBeatsDescribe")
                .operation(op("DescribeThings").withOutput())
                .operation(op("ListThings").withOutput())
                .expect("ListThings"),
            scenario("verb_describeBeatsNonPreferred")
                .operation(op("BatchThings").withOutput())
                .operation(op("DescribeThings").withOutput())
                .expect("DescribeThings"),
            scenario("verb_getBeatsNonPreferred")
                .operation(op("CountThings").withOutput())
                .operation(op("GetThings").withOutput())
                .expect("GetThings"),
            scenario("verb_describeBeatsGet")
                .operation(op("DescribeThings").withOutput())
                .operation(op("GetThings").withOutput())
                .expect("DescribeThings"),

            // Preference 7: alphabetical tie-break.
            scenario("fullyTiedOperations_areBrokenAlphabetically")
                .operation(op("BravoOperation").withOutput())
                .operation(op("AlphaOperation").withOutput())
                .expect("AlphaOperation"),

            scenario("dynamoDbWorkedExample_selectsListTables")
                .verifiedSimpleMethods("listTables", "describeLimits")
                .operation(op("ListTables").withOutput())
                .operation(op("DescribeLimits").withOutput())
                .operation(op("GetItem").withOutput().withRequiredMembers(2))
                .operation(op("PutItem").withRequiredMembers(2))
                .expect("ListTables"),

            // Hard gate: a required URI/endpoint-bound member is dummy-fillable only when it is a string.
            scenario("requiredStringUriMember_isStillEligible")
                .operation(op("GetThing").withOutput().withRequiredUriMember("ThingName", "String"))
                .expect("GetThing"),
            scenario("requiredNonStringUriMemberOnly_isNotSelected")
                .operation(op("GetThing").withOutput().withRequiredUriMember("ThingVersion", "Integer"))
                .expectNothing(),
            scenario("requiredNonStringUriMember_fallsBackToNextBestOperation")
                .operation(op("GetThing").withOutput().withRequiredUriMember("ThingVersion", "Integer"))
                .operation(op("PutThing").withOutput().withRequiredMembers(2))
                .expect("PutThing"),
            // An ARN context param is a string, so it is fillable with an ARN-shaped dummy value.
            scenario("requiredArnContextParamMember_isStillEligible")
                .operation(op("GetResource").withOutput().withRequiredContextParamMember("ResourceArn", "String"))
                .expect("GetResource"),
            scenario("requiredNonArnContextParamMember_isStillEligible")
                .operation(op("ListGrants").withOutput().withRequiredContextParamMember("AccountId", "String"))
                .expect("ListGrants")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dummyValueScenarios")
    public void membersRequiringDummyValue_returnsOnlyUriAndEndpointBoundRequiredMembers(DummyValueScenario scenario) {
        List<String> memberNames = WarmUpOperationSelector.membersRequiringDummyValue(scenario.operation).stream()
                                                          .map(MemberModel::getName)
                                                          .collect(java.util.stream.Collectors.toList());
        assertThat(memberNames).containsExactlyElementsOf(scenario.expectedMemberNames);
    }

    private static Stream<DummyValueScenario> dummyValueScenarios() {
        return Stream.of(
            new DummyValueScenario("requiredUriMember_needsDummy",
                                   op("GetThing").withRequiredUriMember("ThingName", "String").build(),
                                   Collections.singletonList("ThingName")),
            new DummyValueScenario("requiredEndpointContextParamMember_needsDummy",
                                   op("ListGrants").withRequiredContextParamMember("AccountId", "String").build(),
                                   Collections.singletonList("AccountId")),
            new DummyValueScenario("requiredBodyMember_staysNull",
                                   op("ListThings").withRequiredMembers(2).build(),
                                   Collections.emptyList()),
            new DummyValueScenario("optionalUriMember_staysNull",
                                   op("GetThing").withOptionalUriMember("ThingName", "String").build(),
                                   Collections.emptyList()),
            new DummyValueScenario("noInputShape_needsNothing",
                                   op("ListThings").build(),
                                   Collections.emptyList())
        );
    }

    @ParameterizedTest(name = "isArnMember({0}) == {1}")
    @MethodSource("arnMemberScenarios")
    public void isArnMember_matchesOnlyConventionalArnSuffix(String memberName, boolean expected) {
        MemberModel member = new MemberModel();
        member.setName(memberName);
        member.setContextParam(new ContextParam());
        assertThat(WarmUpOperationSelector.isArnMember(member)).isEqualTo(expected);
    }

    private static Stream<Arguments> arnMemberScenarios() {
        return Stream.of(
            // Conventional ARN suffix, capitalized. Both the "ARN" and "Arn" forms occur in real models.
            Arguments.of("resourceARN", true),
            Arguments.of("resourceArn", true),
            Arguments.of("RoleArn", true),
            // Lookalikes: contain the letters "arn" but are not ARN members.
            Arguments.of("learn", false),
            Arguments.of("warning", false),
            Arguments.of("yarnConfig", false),
            Arguments.of("AccountId", false)
        );
    }

    @Test
    public void isArnMember_withoutContextParam_isFalse() {
        MemberModel member = new MemberModel();
        member.setName("resourceArn");
        assertThat(WarmUpOperationSelector.isArnMember(member)).isFalse();
    }

    private static final class DummyValueScenario {
        private final String name;
        private final OperationModel operation;
        private final List<String> expectedMemberNames;

        private DummyValueScenario(String name, OperationModel operation, List<String> expectedMemberNames) {
            this.name = name;
            this.operation = operation;
            this.expectedMemberNames = expectedMemberNames;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static Scenario scenario(String name) {
        return new Scenario(name);
    }

    private static OperationBuilder op(String operationName) {
        return new OperationBuilder(operationName);
    }

    private static final class Scenario {
        private final String name;
        private final List<OperationModel> operations = new ArrayList<>();
        private List<String> verifiedSimpleMethods = Collections.emptyList();
        private String expected;

        private Scenario(String name) {
            this.name = name;
        }

        private Scenario verifiedSimpleMethods(String... methodNames) {
            this.verifiedSimpleMethods = Arrays.asList(methodNames);
            return this;
        }

        private Scenario operation(OperationBuilder operation) {
            this.operations.add(operation.build());
            return this;
        }

        private Scenario expect(String operationName) {
            this.expected = operationName;
            return this;
        }

        private Scenario expectNothing() {
            this.expected = null;
            return this;
        }

        private IntermediateModel model() {
            Map<String, OperationModel> operationsByName = new HashMap<>();
            for (OperationModel operation : operations) {
                operationsByName.put(operation.getOperationName(), operation);
            }

            CustomizationConfig config = CustomizationConfig.create();
            config.setVerifiedSimpleMethods(verifiedSimpleMethods);

            Metadata metadata = new Metadata().withServiceName("TestService");
            return new IntermediateModel(metadata, operationsByName, Collections.emptyMap(), config);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class OperationBuilder {
        private final OperationModel operation = new OperationModel();

        private OperationBuilder(String operationName) {
            operation.setOperationName(operationName);
        }

        private OperationBuilder withOutput() {
            operation.setOutputShape(new ShapeModel());
            return this;
        }

        private OperationBuilder withRequiredMembers(int requiredCount) {
            ShapeModel shape = new ShapeModel();
            List<MemberModel> members = new ArrayList<>();
            for (int i = 0; i < requiredCount; i++) {
                MemberModel member = new MemberModel();
                member.setName("Member" + i);
                member.setRequired(true);
                members.add(member);
            }
            shape.setMembers(members);
            operation.setInputShape(shape);
            return this;
        }

        private OperationBuilder withRequiredUriMember(String memberName, String simpleType) {
            addMember(uriMember(memberName, simpleType, true));
            return this;
        }

        private OperationBuilder withOptionalUriMember(String memberName, String simpleType) {
            addMember(uriMember(memberName, simpleType, false));
            return this;
        }

        private OperationBuilder withRequiredContextParamMember(String memberName, String simpleType) {
            MemberModel member = member(memberName, simpleType, true);
            member.setContextParam(new ContextParam());
            addMember(member);
            return this;
        }

        private OperationBuilder withStreamingInput() {
            operation.setInputShape(streamingShape());
            return this;
        }

        private OperationBuilder withStreamingOutput() {
            operation.setOutputShape(streamingShape());
            return this;
        }

        private OperationBuilder withEventStreamInput() {
            operation.setInputShape(eventStreamShape());
            return this;
        }

        private OperationBuilder withEventStreamOutput() {
            operation.setOutputShape(eventStreamShape());
            return this;
        }

        private OperationBuilder deprecated() {
            operation.setDeprecated(true);
            return this;
        }

        private OperationBuilder withNoAuth() {
            operation.setIsAuthenticated(false);
            return this;
        }

        private OperationModel build() {
            return operation;
        }

        private void addMember(MemberModel member) {
            ShapeModel shape = operation.getInputShape();
            if (shape == null) {
                shape = new ShapeModel();
                shape.setMembers(new ArrayList<>());
                operation.setInputShape(shape);
            }
            shape.getMembers().add(member);
        }

        private static MemberModel uriMember(String memberName, String simpleType, boolean required) {
            MemberModel member = member(memberName, simpleType, required);
            ParameterHttpMapping http = new ParameterHttpMapping();
            http.setLocation(Location.URI);
            member.setHttp(http);
            return member;
        }

        private static MemberModel member(String memberName, String simpleType, boolean required) {
            MemberModel member = new MemberModel();
            member.setName(memberName);
            member.setRequired(required);
            member.setVariable(new VariableModel(memberName, simpleType));
            return member;
        }

        private static ShapeModel streamingShape() {
            ShapeModel shape = new ShapeModel();
            shape.setHasStreamingMember(true);
            return shape;
        }

        private static ShapeModel eventStreamShape() {
            ShapeModel eventStreamShape = new ShapeModel();
            eventStreamShape.withIsEventStream(true);

            MemberModel member = new MemberModel();
            member.setShape(eventStreamShape);

            ShapeModel shape = new ShapeModel();
            shape.setMembers(Collections.singletonList(member));
            return shape;
        }
    }
}
