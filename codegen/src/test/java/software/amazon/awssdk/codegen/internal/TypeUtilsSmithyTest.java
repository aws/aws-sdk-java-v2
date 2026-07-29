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

package software.amazon.awssdk.codegen.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Verifies that {@link TypeUtils#getJavaDataType(Model, software.amazon.smithy.model.shapes.Shape)}
 * produces exactly the same Java type strings the C2J overload produces for
 * equivalent shapes, so downstream templates see identical output regardless
 * of which model format the codegen was fed.
 */
class TypeUtilsSmithyTest {

    private TypeUtils typeUtils;

    @BeforeEach
    void setUp() {
        NamingStrategy naming = mock(NamingStrategy.class);
        when(naming.getShapeClassName("MyStruct")).thenReturn("MyStruct");
        when(naming.getShapeClassName("MyUnion")).thenReturn("MyUnion");
        when(naming.getShapeClassName("Status")).thenReturn("Status");
        when(naming.getShapeClassName("Priority")).thenReturn("Priority");
        typeUtils = new TypeUtils(naming);
    }

    private static Model modelOf(String body) {
        return Model.assembler()
                    .addUnparsedModel("test.smithy", "$version: \"2.0\"\nnamespace demo\n\n" + body)
                    .assemble()
                    .unwrap();
    }

    // Scalars ---------------------------------------------------------------

    @Test
    void scalarShapes_returnMatchingJavaTypes() {
        Model model = modelOf(
            "structure S {\n"
            + "    s: String\n"
            + "    b: Boolean\n"
            + "    y: Byte\n"
            + "    sh: Short\n"
            + "    i: Integer\n"
            + "    l: Long\n"
            + "    f: Float\n"
            + "    d: Double\n"
            + "    bi: BigInteger\n"
            + "    bd: BigDecimal\n"
            + "    t: Timestamp\n"
            + "    bl: Blob\n"
            + "}\n");

        // Scalars targeted by S's members live in smithy.api. Resolve them explicitly.
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#String"))))
            .isEqualTo("String");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#Boolean"))))
            .isEqualTo("Boolean");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#Byte"))))
            .isEqualTo("Byte");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#Short"))))
            .isEqualTo("Short");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#Integer"))))
            .isEqualTo("Integer");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#Long"))))
            .isEqualTo("Long");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#Float"))))
            .isEqualTo("Float");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#Double"))))
            .isEqualTo("Double");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#BigInteger"))))
            .isEqualTo("java.math.BigInteger");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#BigDecimal"))))
            .isEqualTo("java.math.BigDecimal");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#Timestamp"))))
            .isEqualTo("java.time.Instant");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("smithy.api#Blob"))))
            .isEqualTo("software.amazon.awssdk.core.SdkBytes");
    }

    // Blob streaming --------------------------------------------------------

    @Test
    void streamingBlob_returnsInputStreamFqn() {
        Model model = modelOf(
            "@streaming blob Stream\n");

        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("demo#Stream"))))
            .isEqualTo("java.io.InputStream");
    }

    // Structure / union -----------------------------------------------------

    @Test
    void structureAndUnion_returnJavaClassNameFromNamingStrategy() {
        Model model = modelOf(
            "structure MyStruct { name: String }\n"
            + "union MyUnion { a: String }\n");

        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("demo#MyStruct"))))
            .isEqualTo("MyStruct");
        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("demo#MyUnion"))))
            .isEqualTo("MyUnion");
    }

    // List / map ------------------------------------------------------------

    @Test
    void list_returnsFullyQualifiedGenericSignature() {
        Model model = modelOf(
            "list StringList { member: String }\n");

        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("demo#StringList"))))
            .isEqualTo("java.util.List<String>");
    }

    @Test
    void map_returnsFullyQualifiedGenericSignature_noSpaceAfterComma() {
        // C2J emits "java.util.Map<String,String>" with no space; parity requires exact match.
        Model model = modelOf(
            "map StringMap { key: String, value: String }\n");

        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("demo#StringMap"))))
            .isEqualTo("java.util.Map<String,String>");
    }

    @Test
    void nestedListOfMap_recursesCorrectly() {
        Model model = modelOf(
            "map StringMap { key: String, value: String }\n"
            + "list ListOfMaps { member: StringMap }\n");

        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("demo#ListOfMaps"))))
            .isEqualTo("java.util.List<java.util.Map<String,String>>");
    }

    // Enums -----------------------------------------------------------------

    @Test
    void enumShape_returnsStringNotEnumClass() {
        // The member's variable type is "String"; the enum class is tracked separately
        // via MemberModel.enumType. This matches C2J's convention.
        Model model = modelOf(
            "enum Status { A, B }\n");

        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("demo#Status"))))
            .isEqualTo("String");
    }

    @Test
    void intEnumShape_returnsIntegerNotEnumClass() {
        Model model = modelOf(
            "intEnum Priority {\n"
            + "    LOW = 1\n"
            + "    HIGH = 2\n"
            + "}\n");

        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("demo#Priority"))))
            .isEqualTo("Integer");
    }

    // Document --------------------------------------------------------------

    @Test
    void documentShape_returnsSdkDocumentFqn() {
        Model model = modelOf(
            "document AnyDoc\n");

        assertThat(typeUtils.getJavaDataType(model, model.expectShape(ShapeId.from("demo#AnyDoc"))))
            .isEqualTo("software.amazon.awssdk.core.document.Document");
    }
}
