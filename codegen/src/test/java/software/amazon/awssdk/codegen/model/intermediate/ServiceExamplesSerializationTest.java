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
package software.amazon.awssdk.codegen.model.intermediate;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;

public class ServiceExamplesSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Tests that deserializing and serializing service examples yields the same results.
     */
    @Test
    public void roundTripSerialization() throws IOException {
        JsonNode actualJson = mapper.readTree(getClass().getResource("dummy-example.json"));
        ServiceExamples examples = mapper
                .treeToValue(actualJson, ServiceExamples.class);
        JsonNode roundTrippedJson = mapper.readTree(mapper.writeValueAsString(examples));
        assertEquals(roundTrippedJson, actualJson);
    }
}
