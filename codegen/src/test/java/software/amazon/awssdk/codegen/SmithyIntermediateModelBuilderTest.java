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

package software.amazon.awssdk.codegen;

import java.io.File;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

public class SmithyIntermediateModelBuilderTest {

    @Test
    public void testTranslate() {
        final File modelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/smithy/basic-smithy-model.json").getFile());
        SmithyModelWithCustomizations smithyModel = SmithyModelWithCustomizations.builder()
            .smithyModel(modelFile.toPath())
            .build();
        IntermediateModel im = new SmithyIntermediateModelBuilder(smithyModel).build();
    }
}
