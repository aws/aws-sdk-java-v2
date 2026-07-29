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

package software.amazon.awssdk.codegen.naming;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.Shape;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

class ShapeInfoTest {

    private static Shape shape(String type) {
        Shape s = new Shape();
        s.setType(type);
        return s;
    }

    private static Shape enumShape() {
        Shape s = shape("string");
        s.setEnumValues(asList("A", "B"));
        return s;
    }

    private static Member memberTargeting(String targetShapeName) {
        Member m = new Member();
        m.setShape(targetShapeName);
        return m;
    }

    private static Shape listOf(String memberShapeName) {
        Shape s = shape("list");
        s.setListMember(memberTargeting(memberShapeName));
        return s;
    }

    private static Shape mapOf(String keyShapeName, String valueShapeName) {
        Shape s = shape("map");
        s.setMapKeyType(memberTargeting(keyShapeName));
        s.setMapValueType(memberTargeting(valueShapeName));
        return s;
    }

    @Test
    void unionAndExceptionPredicates_wireToBackingShape() {
        Shape union = shape("structure");
        union.setUnion(true);
        Shape exception = shape("structure");
        exception.setException(true);

        assertThat(ShapeInfo.ofC2j(union, emptyMap()).isUnion()).isTrue();
        assertThat(ShapeInfo.ofC2j(union, emptyMap()).isException()).isFalse();
        assertThat(ShapeInfo.ofC2j(exception, emptyMap()).isException()).isTrue();
        assertThat(ShapeInfo.ofC2j(exception, emptyMap()).isUnion()).isFalse();
    }

    @Test
    void listAndMapPredicates_notSwapped() {
        Map<String, Shape> shapes = singletonMap("String", shape("string"));

        ShapeInfo list = ShapeInfo.ofC2j(listOf("String"), shapes);
        ShapeInfo map = ShapeInfo.ofC2j(mapOf("String", "String"), shapes);

        assertThat(list.isList()).isTrue();
        assertThat(list.isMap()).isFalse();
        assertThat(map.isMap()).isTrue();
        assertThat(map.isList()).isFalse();
    }

    @Test
    void isOrContainsEnum_recursesThroughAllShapes() {
        Map<String, Shape> shapes = new HashMap<>();
        shapes.put("Status", enumShape());
        shapes.put("Name", shape("string"));

        assertThat(ShapeInfo.ofC2j(listOf("Status"), shapes).isOrContainsEnum()).isTrue();
        assertThat(ShapeInfo.ofC2j(mapOf("Name", "Status"), shapes).isOrContainsEnum()).isTrue();

        assertThat(ShapeInfo.ofC2j(listOf("Name"), shapes).isOrContainsEnum()).isFalse();
    }

    // ---- ofSmithy ---------------------------------------------------------

    private static Model smithyModel(String idl) {
        return Model.assembler()
                    .addUnparsedModel("test.smithy", "$version: \"2.0\"\nnamespace demo\n\n" + idl)
                    .assemble()
                    .unwrap();
    }

    @Test
    void ofSmithy_unionAndExceptionPredicates_wireToBackingShape() {
        Model model = smithyModel(
            "union U { a: String }\n"
            + "@error(\"client\") structure E { message: String }\n"
            + "structure S { name: String }\n");

        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#U")), model).isUnion())
            .isTrue();
        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#U")), model).isException())
            .isFalse();
        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#E")), model).isException())
            .isTrue();
        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#S")), model).isUnion())
            .isFalse();
    }

    @Test
    void ofSmithy_listAndMapPredicates_notSwapped() {
        Model model = smithyModel(
            "list StringList { member: String }\n"
            + "map StringMap { key: String, value: String }\n");

        ShapeInfo list = ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#StringList")), model);
        ShapeInfo map = ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#StringMap")), model);

        assertThat(list.isList()).isTrue();
        assertThat(list.isMap()).isFalse();
        assertThat(map.isMap()).isTrue();
        assertThat(map.isList()).isFalse();
    }

    @Test
    void ofSmithy_isOrContainsEnum_recursesThroughListAndMap() {
        Model model = smithyModel(
            "enum Status { A, B }\n"
            + "list StatusList { member: Status }\n"
            + "map NameToStatus { key: String, value: Status }\n"
            + "list StringList { member: String }\n");

        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#Status")), model).isOrContainsEnum())
            .isTrue();
        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#StatusList")), model).isOrContainsEnum())
            .isTrue();
        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#NameToStatus")), model).isOrContainsEnum())
            .isTrue();
        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#StringList")), model).isOrContainsEnum())
            .isFalse();
    }

    @Test
    void ofSmithy_intEnumIsAlsoOrContainsEnum() {
        Model model = smithyModel(
            "intEnum Priority {\n"
            + "    LOW = 1\n"
            + "    HIGH = 2\n"
            + "}\n"
            + "list Priorities { member: Priority }\n");

        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#Priority")), model).isOrContainsEnum())
            .isTrue();
        assertThat(ShapeInfo.ofSmithy(model.expectShape(ShapeId.from("demo#Priorities")), model).isOrContainsEnum())
            .isTrue();
    }
}
