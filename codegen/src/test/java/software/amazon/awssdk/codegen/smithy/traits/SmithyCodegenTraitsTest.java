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

package software.amazon.awssdk.codegen.smithy.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

class SmithyCodegenTraitsTest {

    private static final ClassLoader LOADER = SmithyCodegenTraitsTest.class.getClassLoader();
    private static final ShapeId TARGET = ShapeId.from("demo#Shape$member");
    private static final ShapeId OPERATION = ShapeId.from("demo#Op");

    static Stream<Arguments> traits() {
        return Stream.of(Arguments.of(DeprecatedNameTrait.ID, Node.from("Bucket"), DeprecatedNameTrait.class),
                         Arguments.of(AlternateBeanPropertyNameTrait.ID, Node.from("Null"),
                                      AlternateBeanPropertyNameTrait.class),
                         Arguments.of(EnumConstantNameTrait.ID, Node.from("FIVE_ONE"), EnumConstantNameTrait.class),
                         Arguments.of(IgnoreDataTypeConversionFailuresTrait.ID, Node.objectNode(),
                                      IgnoreDataTypeConversionFailuresTrait.class),
                         Arguments.of(SyntheticTrait.ID, Node.objectNode(), SyntheticTrait.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("traits")
    void traitService_createsTheTraitClass(ShapeId id, Node value, Class<? extends Trait> traitClass) {
        Trait trait = TraitFactory.createServiceFactory(LOADER).createTrait(id, TARGET, value).orElse(null);

        assertThat(trait).isInstanceOf(traitClass);
        assertThat(trait.toShapeId()).isEqualTo(id);
        if (trait instanceof StringTrait) {
            assertThat(((StringTrait) trait).getValue()).isEqualTo(value.expectStringNode().getValue());
        }
    }

    @Test
    void traitService_createsPaginatorOverrideTrait() {
        Node value = Node.objectNodeBuilder()
                         .withMember("inputToken", ArrayNode.fromStrings(Arrays.asList("KeyMarker", "UploadIdMarker")))
                         .withMember("outputToken", ArrayNode.fromStrings(Arrays.asList("NextKeyMarker",
                                                                                        "NextUploadIdMarker")))
                         .withMember("resultKey", ArrayNode.fromStrings(Arrays.asList("Uploads", "CommonPrefixes")))
                         .withMember("moreResults", "IsTruncated")
                         .withMember("limitKey", "MaxUploads")
                         .build();

        Trait trait = TraitFactory.createServiceFactory(LOADER).createTrait(PaginatorOverrideTrait.ID, OPERATION, value)
                                  .orElse(null);

        assertThat(trait).isInstanceOf(PaginatorOverrideTrait.class);
        PaginatorOverrideTrait override = (PaginatorOverrideTrait) trait;
        assertThat(override.isSkip()).isFalse();
        assertThat(override.getInputToken()).containsExactly("KeyMarker", "UploadIdMarker");
        assertThat(override.getOutputToken()).containsExactly("NextKeyMarker", "NextUploadIdMarker");
        assertThat(override.getResultKey()).hasValue(Arrays.asList("Uploads", "CommonPrefixes"));
        assertThat(override.getMoreResults()).hasValue("IsTruncated");
        assertThat(override.getLimitKey()).hasValue("MaxUploads");
        assertThat(override.toNode()).isEqualTo(value);
    }

    @Test
    void paginatorOverrideSkip_hasNoTokens() {
        PaginatorOverrideTrait override =
            new PaginatorOverrideTrait(Node.objectNode().withMember("skip", Node.from(true)));

        assertThat(override.isSkip()).isTrue();
        assertThat(override.getInputToken()).isEmpty();
        assertThat(override.getResultKey()).isEmpty();
    }

    static Stream<Arguments> invalidPaginatorOverrides() {
        return Stream.of(
            Arguments.of("skip with tokens", "{\"skip\": true, \"inputToken\": [\"a\"], \"outputToken\": [\"a\"]}",
                         "skip cannot be combined"),
            Arguments.of("no outputToken", "{\"inputToken\": [\"a\"]}", "needs inputToken and outputToken"),
            Arguments.of("skip false alone", "{\"skip\": false}", "Remove the trait to use @paginated"),
            Arguments.of("empty inputToken", "{\"inputToken\": [], \"outputToken\": [\"a\"]}",
                         "needs inputToken and outputToken"),
            Arguments.of("unknown member", "{\"inputToken\": [\"a\"], \"outputToken\": [\"a\"], \"items\": \"b\"}",
                         "items"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPaginatorOverrides")
    void invalidPaginatorOverride_isRejected(String description, String json, String message) {
        ObjectNode node = Node.parse(json).expectObjectNode();

        assertThatThrownBy(() -> new PaginatorOverrideTrait(node))
            .isInstanceOf(ExpectationNotMetException.class)
            .hasMessageContaining(message);
    }

    @Test
    void traitService_createsWaiterOverridesTrait() {
        Node value = Node.parse("{\"BucketExists\": {\"maxAttempts\": 20, \"delay\": 5, \"acceptors\": ["
                                + "{\"matcher\": \"status\", \"expected\": 301, \"state\": \"success\"},"
                                + "{\"matcher\": \"path\", \"argument\": \"a.b\", \"expected\": true, \"state\": \"retry\"}]},"
                                + "\"ObjectExists\": {\"maxAttempts\": 7},"
                                + "\"New\": {\"skip\": true}}");

        Trait trait = TraitFactory.createServiceFactory(LOADER).createTrait(WaiterOverridesTrait.ID, OPERATION, value)
                                  .orElse(null);

        assertThat(trait).isInstanceOf(WaiterOverridesTrait.class);
        Map<String, WaiterOverridesTrait.WaiterOverride> waiters = ((WaiterOverridesTrait) trait).getWaiters();
        assertThat(waiters).containsOnlyKeys("BucketExists", "ObjectExists", "New");
        WaiterOverridesTrait.WaiterOverride bucketExists = waiters.get("BucketExists");
        assertThat(bucketExists.getMaxAttempts()).hasValue(20);
        assertThat(bucketExists.getDelay()).hasValue(5);
        assertThat(bucketExists.getAcceptors().get())
            .extracting(a -> a.getMatcher(), a -> a.getArgument().orElse(null), a -> a.getState(),
                        a -> Node.printJson(a.getExpected()))
            .containsExactly(tuple("status", null, "success", "301"),
                             tuple("path", "a.b", "retry", "true"));
        assertThat(waiters.get("ObjectExists").getMaxAttempts()).hasValue(7);
        assertThat(waiters.get("ObjectExists").getAcceptors()).isEmpty();
        assertThat(waiters.get("New").isSkip()).isTrue();
        assertThat(trait.toNode()).isEqualTo(value);
    }

    static Stream<Arguments> invalidWaiterOverrides() {
        return Stream.of(
            Arguments.of("skip with maxAttempts", "{\"skip\": true, \"maxAttempts\": 3}", "skip cannot be combined"),
            Arguments.of("no maxAttempts", "{\"delay\": 5}", "needs maxAttempts of at least 1"),
            Arguments.of("zero maxAttempts", "{\"maxAttempts\": 0}", "needs maxAttempts of at least 1"),
            Arguments.of("delay without acceptors", "{\"maxAttempts\": 3, \"delay\": 5}", "give both or neither"),
            Arguments.of("empty acceptors", "{\"maxAttempts\": 3, \"delay\": 5, \"acceptors\": []}",
                         "acceptors cannot be empty"),
            Arguments.of("unknown matcher", "{\"maxAttempts\": 3, \"delay\": 5, \"acceptors\": [{\"matcher\": \"code\", "
                                            + "\"expected\": 1, \"state\": \"success\"}]}", "unknown acceptor matcher"),
            Arguments.of("unknown state", "{\"maxAttempts\": 3, \"delay\": 5, \"acceptors\": [{\"matcher\": \"status\", "
                                          + "\"expected\": 1, \"state\": \"done\"}]}", "unknown acceptor state"),
            Arguments.of("object expected", "{\"maxAttempts\": 3, \"delay\": 5, \"acceptors\": [{\"matcher\": \"path\", "
                                            + "\"expected\": {}, \"state\": \"success\"}]}",
                         "must be a string, number, or boolean"),
            Arguments.of("unknown member", "{\"maxAttempts\": 3, \"minDelay\": 5}", "minDelay"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidWaiterOverrides")
    void invalidWaiterOverride_isRejected(String description, String entry, String message) {
        ObjectNode node = Node.objectNode().withMember("Waiter", Node.parse(entry));

        assertThatThrownBy(() -> new WaiterOverridesTrait(node))
            .isInstanceOf(ExpectationNotMetException.class)
            .hasMessageContaining(message);
    }

    @Test
    void waiterOverridesOnStructure_isATraitTargetError() {
        assertThat(errors("@waiterOverrides(W: { maxAttempts: 3 }) structure Shape { member: String }"))
            .extracting(ValidationEvent::getId, e -> e.getShapeId().get())
            .containsExactly(tuple("TraitTarget", ShapeId.from("demo#Shape")));
    }

    @Test
    void paginatorOverrideOnStructure_isATraitTargetError() {
        assertThat(errors("@paginatorOverride(skip: true) structure Shape { member: String }"))
            .extracting(ValidationEvent::getId, e -> e.getShapeId().get())
            .containsExactly(tuple("TraitTarget", ShapeId.from("demo#Shape")));
    }

    @Test
    void discoverModels_loadsTheTraitDefinitions() {
        Model model = Model.assembler(LOADER).discoverModels(LOADER).assemble().unwrap();

        for (ShapeId id : Arrays.asList(DeprecatedNameTrait.ID, AlternateBeanPropertyNameTrait.ID,
                                        EnumConstantNameTrait.ID, IgnoreDataTypeConversionFailuresTrait.ID,
                                        SyntheticTrait.ID, PaginatorOverrideTrait.ID, WaiterOverridesTrait.ID)) {
            assertThat(model.getShape(id)).as("definition of %s", id).isPresent();
            assertThat(model.expectShape(id).hasTrait(TraitDefinition.class)).as("%s is a trait", id).isTrue();
        }
    }

    @Test
    void enumConstantNameOnStructureMember_isATraitTargetError() {
        assertThat(errors("structure Shape { @enumConstantName(\"X\") member: String }"))
            .extracting(ValidationEvent::getId, e -> e.getShapeId().get())
            .containsExactly(tuple("TraitTarget", TARGET));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"list Target { member: String }",
                            "map Target { key: String, value: String }",
                            "enum Target { A }",
                            "@enum([{value: \"a\"}]) string Target"})
    void deprecatedNameOnUnsupportedTarget_isATraitTargetError(String target) {
        assertThat(errors("structure Shape { @deprecatedName(\"Old\") member: Target }\n" + target))
            .extracting(ValidationEvent::getId, e -> e.getShapeId().get())
            .containsExactly(tuple("TraitTarget", TARGET));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"structure Shape { @deprecatedName(\"\") member: String }",
                            "structure Shape { @alternateBeanPropertyName(\"\") member: String }",
                            "enum Shape { @enumConstantName(\"\") member }"})
    void emptyStringTraitValue_isATraitValueError(String shapes) {
        assertThat(errors(shapes))
            .extracting(ValidationEvent::getId, e -> e.getShapeId().get())
            .containsExactly(tuple("TraitValue", TARGET));
    }

    @Test
    void traitsOnTheirSelectors_validate() {
        assertThat(errors("union Shape {\n"
                          + "  @deprecatedName(\"Old\") @alternateBeanPropertyName(\"Null\")\n"
                          + "  @ignoreDataTypeConversionFailures\n"
                          + "  member: String\n"
                          + "}\n"
                          + "@synthetic enum Kind {\n  @enumConstantName(\"FIVE_ONE\")\n  A = \"5.1\"\n}\n"
                          + "@synthetic string Name\n"
                          + "@synthetic structure Payload { member: String }\n"
                          + "@paginatorOverride(inputToken: [\"a\", \"b\"], outputToken: [\"c || d\"], "
                          + "resultKey: [\"e\"], moreResults: \"f\", limitKey: \"g\")\n"
                          + "@waiterOverrides(A: { maxAttempts: 3 }, B: { skip: true }, C: { maxAttempts: 20, delay: 5, "
                          + "acceptors: [{ matcher: \"status\", expected: 200, state: \"success\" }, "
                          + "{ matcher: \"path\", argument: \"a\", expected: true, state: \"retry\" }] })\n"
                          + "operation Op {}\n"))
            .isEmpty();
    }

    private static List<ValidationEvent> errors(String shapes) {
        String idl = "$version: \"2\"\nnamespace demo\n\n"
                     + "use software.amazon.awssdk.codegen#alternateBeanPropertyName\n"
                     + "use software.amazon.awssdk.codegen#deprecatedName\n"
                     + "use software.amazon.awssdk.codegen#enumConstantName\n"
                     + "use software.amazon.awssdk.codegen#ignoreDataTypeConversionFailures\n"
                     + "use software.amazon.awssdk.codegen#paginatorOverride\n"
                     + "use software.amazon.awssdk.codegen#synthetic\n"
                     + "use software.amazon.awssdk.codegen#waiterOverrides\n\n"
                     + shapes + "\n";
        return Model.assembler(LOADER)
                    .discoverModels(LOADER)
                    .addUnparsedModel("test.smithy", idl)
                    .assemble()
                    .getValidationEvents(Severity.ERROR);
    }
}
