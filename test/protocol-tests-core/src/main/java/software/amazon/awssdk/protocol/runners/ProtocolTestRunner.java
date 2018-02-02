/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.runners;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.protocol.model.TestCase;
import software.amazon.awssdk.protocol.reflect.ClientReflector;
import software.amazon.awssdk.protocol.wiremock.WireMockUtils;

/**
 * Runs a list of test cases (either marshalling or unmarshalling).
 */
public final class ProtocolTestRunner {

    private static final Logger log = LoggerFactory.getLogger(ProtocolTestRunner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonParser.Feature.ALLOW_COMMENTS);

    private final ClientReflector clientReflector;
    private final MarshallingTestRunner marshallingTestRunner;
    private final UnmarshallingTestRunner unmarshallingTestRunner;

    public ProtocolTestRunner(String intermediateModelLocation) {
        WireMockUtils.startWireMockServer();
        final IntermediateModel model = loadModel(intermediateModelLocation);
        this.clientReflector = new ClientReflector(model);
        this.marshallingTestRunner = new MarshallingTestRunner(model, clientReflector);
        this.unmarshallingTestRunner = new UnmarshallingTestRunner(model, clientReflector);
        IdempotentUtils.setGenerator(() -> "00000000-0000-4000-8000-000000000000");
    }

    private IntermediateModel loadModel(String intermedidateModelLocation) {
        try {
            return MAPPER.readValue(getClass().getResource(intermedidateModelLocation),
                                    IntermediateModel.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runTests(List<TestCase> tests) throws Exception {
        for (TestCase testCase : tests) {
            log.info("Running test: {}", testCase.getDescription());
            switch (testCase.getWhen().getAction()) {
                case MARSHALL:
                    marshallingTestRunner.runTest(testCase);
                    break;
                case UNMARSHALL:
                    unmarshallingTestRunner.runTest(testCase);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unsupported action " + testCase.getWhen().getAction());
            }
        }
    }
}
