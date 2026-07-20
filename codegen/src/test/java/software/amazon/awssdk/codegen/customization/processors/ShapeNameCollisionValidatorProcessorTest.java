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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.awssdk.codegen.validation.ModelInvalidException;
import software.amazon.awssdk.codegen.validation.ValidationEntry;
import software.amazon.awssdk.codegen.validation.ValidationErrorId;
import software.amazon.awssdk.codegen.validation.ValidationErrorSeverity;

public class ShapeNameCollisionValidatorProcessorTest {

    private final ShapeNameCollisionValidatorProcessor processor = new ShapeNameCollisionValidatorProcessor();

    private static ServiceModel loadModel(String resource) throws IOException {
        File file = new File(ShapeNameCollisionValidatorProcessorTest.class
                                 .getResource("/software/amazon/awssdk/codegen/poet/client/c2j/" + resource).getFile());
        return ModelLoaderUtils.loadModel(ServiceModel.class, file);
    }

    @Test
    public void preprocess_shapesCollideByCase_throwsException() throws IOException {
        ServiceModel serviceModel = loadModel("collision/case-insensitive-service-2.json");
        assertThatThrownBy(() -> processor.preprocess(serviceModel))
            .isInstanceOf(ModelInvalidException.class)
            .hasMessageContaining("ReservationType")
            .hasMessageContaining("reservationType")
            .matches(e -> {
                ModelInvalidException modelInvalid = (ModelInvalidException) e;
                ValidationEntry entry = modelInvalid.validationEntries().get(0);
                return entry.getErrorId() == ValidationErrorId.INVALID_IDENTIFIER_NAME &&
                       entry.getSeverity() == ValidationErrorSeverity.DANGER;
            }, "Validation entry details are correct");
    }

    @Test
    public void preprocess_shapesOfSameTypeCollideByCase_throwsException() throws IOException {
        ServiceModel serviceModel = loadModel("shape-name-collision-validator/same-type-service-2.json");
        assertThatThrownBy(() -> processor.preprocess(serviceModel))
            .isInstanceOf(ModelInvalidException.class)
            .hasMessageContaining("Token")
            .hasMessageContaining("token");
    }

    @Test
    public void preprocess_noCaseCollision_doesNotThrow() throws IOException {
        ServiceModel serviceModel = loadModel("shape-name-collision-validator/valid-service-2.json");
        assertThatCode(() -> processor.preprocess(serviceModel)).doesNotThrowAnyException();
    }
}
