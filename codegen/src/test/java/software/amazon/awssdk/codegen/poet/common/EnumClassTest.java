/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.common;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.io.IOException;

import org.junit.Test;

import software.amazon.awssdk.codegen.model.intermediate.EnumModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;

public class EnumClassTest {

    @Test
    public void basicEnumTest() throws IOException {

        ShapeModel m = new ShapeModel("TestEnumClass");
        m.setType(ShapeType.Enum);
        m.setShapeName("TestEnumClass");
        m.setEnums(asList(new EnumModel("Available", "available"), new EnumModel("PermanentFailure", "permanent-failure")));
        m.setDocumentation("Some comment on the class itself");

        EnumClass sut = new EnumClass("software.amazon.awssdk.codegen.poet.common.model", m);

        assertThat(sut, generatesTo("test-enum-class.java"));
    }
}
