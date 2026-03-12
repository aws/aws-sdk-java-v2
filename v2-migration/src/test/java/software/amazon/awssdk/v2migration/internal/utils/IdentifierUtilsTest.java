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

package software.amazon.awssdk.v2migration.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.utils.Pair;

public class IdentifierUtilsTest {
    @Test
    public void makeId_setsCorrectNameAndType() {
        String name = "MyId";
        JavaType.Unknown type = JavaType.Unknown.getInstance();

        J.Identifier identifier = IdentifierUtils.makeId(name, type);

        assertThat(identifier.getSimpleName()).isEqualTo(name);
        assertThat(identifier.getType()).isSameAs(type);
    }

    @ParameterizedTest
    @MethodSource("simpleName_parameterizedTypeCases")
    public void simpleName_generatesCorrectName(Pair<JavaType.Parameterized, String> testCase) {
        assertThat(IdentifierUtils.simpleName(testCase.left())).isEqualTo(testCase.right());
    }

    @Test
    public void simpleName_typeParamNotFullyQualified_throws() {
        JavaType.FullyQualified genericType = TypeUtils.asFullyQualified(JavaType.buildType("foo.bar.BazGeneric"));
        JavaType.Parameterized parameterized =
            new JavaType.Parameterized(null, genericType,
                                       Collections.singletonList(JavaType.Unknown.getInstance()));
        assertThatThrownBy(() -> IdentifierUtils.simpleName(parameterized))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("non fully qualified");
    }

    private static List<Pair> simpleName_parameterizedTypeCases() {
        JavaType.FullyQualified genericType = TypeUtils.asFullyQualified(JavaType.buildType("foo.bar.BazGeneric"));
        JavaType.FullyQualified stringType = TypeUtils.asFullyQualified(JavaType.buildType("java.lang.String"));

        return Arrays.asList(
            Pair.of(new JavaType.Parameterized(null, genericType, Collections.singletonList(stringType)),
                    "BazGeneric<String>"),
            Pair.of(new JavaType.Parameterized(null, genericType, Arrays.asList(stringType, stringType)),
                    "BazGeneric<String, String>"),
            Pair.of(new JavaType.Parameterized(null, genericType, Collections.emptyList()), "BazGeneric<>")

        );
    }
}
