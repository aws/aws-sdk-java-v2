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

package software.amazon.awssdk.protocol.asserts.marshalling;

import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Asserts on the body (expected to be CBOR encoded) of the marshalled request.
 */
public class EncodedBodyAssertion extends MarshallingAssertion {
    private final String encodedEquals;

    public EncodedBodyAssertion(String encodedEquals) {
        this.encodedEquals = encodedEquals;
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        byte[] actualBytes = Base64.getEncoder().encode(actual.getBody());
        String actualValue = new String(actualBytes, StandardCharsets.UTF_8);
        assertEquals(encodedEquals, actualValue);
    }
}
