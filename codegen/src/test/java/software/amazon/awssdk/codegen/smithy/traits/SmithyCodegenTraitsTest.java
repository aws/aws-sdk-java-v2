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
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
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
    void discoverModels_loadsTheTraitDefinitions() {
        Model model = Model.assembler(LOADER).discoverModels(LOADER).assemble().unwrap();

        for (ShapeId id : Arrays.asList(DeprecatedNameTrait.ID, AlternateBeanPropertyNameTrait.ID,
                                        EnumConstantNameTrait.ID, IgnoreDataTypeConversionFailuresTrait.ID,
                                        SyntheticTrait.ID)) {
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
                          + "@synthetic structure Payload { member: String }\n"))
            .isEmpty();
    }

    private static List<ValidationEvent> errors(String shapes) {
        String idl = "$version: \"2\"\nnamespace demo\n\n"
                     + "use software.amazon.awssdk.codegen#alternateBeanPropertyName\n"
                     + "use software.amazon.awssdk.codegen#deprecatedName\n"
                     + "use software.amazon.awssdk.codegen#enumConstantName\n"
                     + "use software.amazon.awssdk.codegen#ignoreDataTypeConversionFailures\n"
                     + "use software.amazon.awssdk.codegen#synthetic\n\n"
                     + shapes + "\n";
        return Model.assembler(LOADER)
                    .discoverModels(LOADER)
                    .addUnparsedModel("test.smithy", idl)
                    .assemble()
                    .getValidationEvents(Severity.ERROR);
    }
}
