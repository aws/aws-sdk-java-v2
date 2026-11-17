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

package software.amazon.awssdk.codegen.smithy.customization.cloudwatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

class CloudWatchMergeErrorShapesTest {
    private static final String NAMESPACE = "com.amazonaws.cloudwatch#";
    private static final String ERRORS =
        "@error(\"client\") structure LimitExceededException {}\n"
        + "@error(\"client\") structure LimitExceededFault {}\n"
        + "@error(\"client\") structure ResourceNotFound {}\n"
        + "@error(\"client\") structure ResourceNotFoundException {}\n"
        + "@error(\"client\") structure InvalidParameterValueException {}\n";

    @Test
    void errorReferences_pointAtTheKeptShape_andTheOtherShapeIsRemoved() {
        Model model = merge("service GraniteServiceVersion20100801 {\n"
                            + "  version: \"2010-08-01\"\n"
                            + "  operations: [EnableInsightRules, PutMetricAlarm, DeleteAlarms, GetDataset]\n"
                            + "}\n"
                            + "operation EnableInsightRules { errors: [InvalidParameterValueException, "
                            + "LimitExceededException] }\n"
                            + "operation PutMetricAlarm { errors: [LimitExceededFault] }\n"
                            + "operation DeleteAlarms { errors: [ResourceNotFound, LimitExceededFault] }\n"
                            + "operation GetDataset { errors: [ResourceNotFoundException] }\n"
                            + ERRORS);

        assertThat(errors(model, "EnableInsightRules")).containsExactly(id("InvalidParameterValueException"),
                                                                        id("LimitExceededFault"));
        assertThat(errors(model, "PutMetricAlarm")).containsExactly(id("LimitExceededFault"));
        assertThat(errors(model, "DeleteAlarms")).containsExactly(id("ResourceNotFoundException"),
                                                                  id("LimitExceededFault"));
        assertThat(errors(model, "GetDataset")).containsExactly(id("ResourceNotFoundException"));
        assertThat(model.getShape(id("LimitExceededException"))).isEmpty();
        assertThat(model.getShape(id("ResourceNotFound"))).isEmpty();
    }

    @Test
    void anOperationListingBothShapes_listsTheKeptShapeOnce() {
        Model model = merge("service GraniteServiceVersion20100801 {\n"
                            + "  version: \"2010-08-01\"\n"
                            + "  operations: [PutInsightRule]\n"
                            + "}\n"
                            + "operation PutInsightRule { errors: [LimitExceededException, LimitExceededFault] }\n"
                            + ERRORS);

        assertThat(errors(model, "PutInsightRule")).containsExactly(id("LimitExceededFault"));
    }

    @Test
    void aPairWhoseShapeIsGone_isSkipped() {
        Model model = merge("service GraniteServiceVersion20100801 {\n"
                            + "  version: \"2010-08-01\"\n"
                            + "  operations: [DeleteAlarms]\n"
                            + "}\n"
                            + "operation DeleteAlarms { errors: [ResourceNotFound] }\n"
                            + "@error(\"client\") structure ResourceNotFound {}\n"
                            + "@error(\"client\") structure ResourceNotFoundException {}\n");

        assertThat(errors(model, "DeleteAlarms")).containsExactly(id("ResourceNotFoundException"));
    }

    @Test
    void aMemberTargetingARemovedShape_fails() {
        assertThatThrownBy(() -> merge("service GraniteServiceVersion20100801 { version: \"2010-08-01\" }\n"
                                       + "structure Holder { error: LimitExceededException }\n"
                                       + ERRORS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Holder$error");
    }

    private static Model merge(String smithy) {
        Model model = Model.assembler()
                           .addUnparsedModel("cloudwatch.smithy",
                                             "$version: \"2.0\"\nnamespace com.amazonaws.cloudwatch\n" + smithy)
                           .assemble()
                           .unwrap();
        return new CloudWatchMergeErrorShapes().preprocess(model, CustomizationConfig.create());
    }

    private static List<ShapeId> errors(Model model, String operation) {
        return model.expectShape(id(operation), OperationShape.class).getErrors();
    }

    private static ShapeId id(String name) {
        return ShapeId.from(NAMESPACE + name);
    }
}
