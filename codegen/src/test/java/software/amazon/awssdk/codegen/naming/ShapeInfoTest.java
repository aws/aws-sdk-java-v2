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
}
