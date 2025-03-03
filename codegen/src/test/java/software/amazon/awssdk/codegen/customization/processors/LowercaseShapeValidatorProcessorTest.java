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

package software.amazon.awssdk.codegen.customization.processors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class LowercaseShapeValidatorProcessorTest {

    private static ServiceModel serviceModel;
    private final LowercaseShapeValidatorProcessor processor = new LowercaseShapeValidatorProcessor();

    @BeforeAll
    public static void setUp() throws IOException {
        File serviceModelFile = new File(LowercaseShapeValidatorProcessorTest.class
                                             .getResource("/software/amazon/awssdk/codegen/poet/client/c2j/lower-case-shape"
                                                          + "-validator/service-2.json").getFile());
        serviceModel = ModelLoaderUtils.loadModel(ServiceModel.class, serviceModelFile);
    }

    @Test
    public void preprocess_serviceWithLowercaseShape_throwsException() {
        assertThatThrownBy(() -> processor.preprocess(serviceModel))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Shape name 'lowercaseshape' starts with a lowercase character");
    }
}
