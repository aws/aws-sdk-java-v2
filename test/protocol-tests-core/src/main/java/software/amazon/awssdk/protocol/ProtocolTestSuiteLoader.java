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

package software.amazon.awssdk.protocol;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.protocol.model.TestCase;
import software.amazon.awssdk.protocol.model.TestSuite;

/**
 * Loads the test specification from it's JSON representation. Assumes the JSON files are in the
 * AwsDrSharedSdk package under /test/protocols.
 */
public final class ProtocolTestSuiteLoader {

    private static final String RESOURCE_PREFIX = "/software/amazon/awssdk/protocol/suites/";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    public List<TestCase> load(String suitePath) throws IOException {
        return loadTestSuite(suitePath).getTestCases().stream()
                                       .flatMap(this::loadTestCases)
                                       .collect(Collectors.toList());
    }

    private TestSuite loadTestSuite(String suitePath) throws IOException {
        return MAPPER.readValue(getClass().getResource(RESOURCE_PREFIX + suitePath),
                                TestSuite.class);
    }

    private Stream<? extends TestCase> loadTestCases(String testCase) {
        try {
            final List<TestCase> testCases = MAPPER
                    .readValue(getClass().getResource(RESOURCE_PREFIX + testCase), new ListTypeReference());
            return testCases.stream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ListTypeReference extends TypeReference<List<TestCase>> {
    }
}
