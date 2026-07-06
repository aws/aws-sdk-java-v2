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

package software.amazon.awssdk.codegen.parity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Runs the parity checker across representative services. Compares the
 * {@code IntermediateModel} produced by the C2J pipeline against the one
 * produced by the Smithy pipeline and fails on any unexpected diff.
 */
class IntermediateModelParityTest {

    private final IntermediateModelParityChecker checker = new IntermediateModelParityChecker();

    @ParameterizedTest(name = "parity for {0}")
    @ValueSource(strings = {
        "lambda",
        "sqs",
        "ec2",
        "route53",
        "kinesis"
    })
    void c2jAndSmithy_produceEqualIntermediateModels(String service) throws IOException {
        IntermediateModel c2j = Fixtures.buildFromC2j(service);
        IntermediateModel smithy = Fixtures.buildFromSmithy(service);

        List<ParityAllowlistEntry> allowlist = loadAllowlistFor(service);

        ParityResult result = checker.compare(service, c2j, smithy, allowlist);

        assertThat(result.unexpectedDiffs())
            .as(result.summary())
            .isEmpty();
    }

    private List<ParityAllowlistEntry> loadAllowlistFor(String service) throws IOException {
        String resource = "/software/amazon/awssdk/codegen/parity/" + service + "-expected-diffs.json";
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) {
                return Collections.emptyList();
            }
            return checker.loadAllowlist(in);
        }
    }
}
