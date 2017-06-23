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

package software.amazon.awssdk.protocol.asserts.marshalling;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

/**
 * Asserts on the body (expected to be JSON) of the marshalled request.
 */
public class JsonBodyAssertion extends MarshallingAssertion {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String jsonEquals;

    public JsonBodyAssertion(String jsonEquals) {
        this.jsonEquals = jsonEquals;
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        JsonNode expected = MAPPER.readTree(jsonEquals);
        JsonNode actualJson = MAPPER.readTree(actual.getBodyAsString());
        assertEquals(expected, actualJson);
    }

}
