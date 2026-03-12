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

package software.amazon.awssdk.codegen.poet.transform;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.ClientTestModels.rpcv2ServiceModels;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.util.Collection;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

public class TransformMarshallerSpecTest {

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("testCases")
    public void runtCase(TestCase testCase) {
        IntermediateModel model = testCase.model();
        model.getShapes().values().stream()
             .filter(shape -> "Request".equals(shape.getType()) || shape.isEvent())
             .map(shape -> new Object[] {shape}).collect(toList());

        String expectedFileName = String.format("%s/%s-transformer.java",
                                                model.getMetadata().getProtocol().toString().toLowerCase(Locale.ENGLISH),
                                                testCase.shapeModel().getShapeName().toLowerCase(Locale.ENGLISH));

        assertThat(new MarshallerSpec(model, testCase.shapeModel()), generatesTo(expectedFileName));

    }

    public static Collection<TestCase> testCases() {
        IntermediateModel model = rpcv2ServiceModels();
        return
            model.getShapes().values().stream()
                 .filter(shape -> "Request".equals(shape.getType()) || shape.isEvent())
                 .map(s -> new TestCase(model, s))
                 .collect(toList());
    }

    static class TestCase {
        private final IntermediateModel model;
        private final ShapeModel shapeModel;

        TestCase(IntermediateModel model, ShapeModel shapeModel) {
            this.model = model;
            this.shapeModel = shapeModel;
        }

        public IntermediateModel model() {
            return model;
        }

        public ShapeModel shapeModel() {
            return shapeModel;
        }

        @Override
        public String toString() {
            return this.shapeModel.getShapeName();
        }
    }

    public IntermediateModel getModel() {
        return rpcv2ServiceModels();
    }
}
