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

package software.amazon.awssdk.auth.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AwsBearerTokenTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    public static final String SAMPLE_TOKEN_STRING = "mJ_9.B5f-4.1Jmv";
    public static final Instant SAMPLE_EXPIRATION_TIME = Instant.ofEpochMilli(1642108606L);

    @Test
    public void constructAwsBearerTokenWithoutExpirationTime(){
        TestBearerToken bearerToken = TestBearerToken.create(SAMPLE_TOKEN_STRING);
        assertEquals(bearerToken.token(), SAMPLE_TOKEN_STRING);
        assertNull(bearerToken.expirationTime());
    }

    @Test
    public void constructAwsBearerTokenWithExpirationTime(){
        TestBearerToken bearerToken = TestBearerToken.create(SAMPLE_TOKEN_STRING, null);
        assertEquals(bearerToken.token(), SAMPLE_TOKEN_STRING);
        assertNull(bearerToken.expirationTime());
        bearerToken = TestBearerToken.create(SAMPLE_TOKEN_STRING, SAMPLE_EXPIRATION_TIME);
        assertEquals(SAMPLE_TOKEN_STRING, bearerToken.token());
        assertEquals(SAMPLE_EXPIRATION_TIME, bearerToken.expirationTime());
    }

    @Test
    public void equalsHashCodeTest() {
        EqualsVerifier.forClass(TestBearerToken.class)
                      .withNonnullFields("token", "expirationTime")
                      .verify();
    }

    @Test
    public void constructAwsBearerTokenWithInvalidTokenString(){
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Token cannot be blank.");
        TestBearerToken.create(null);
        thrown.expectMessage("Token cannot be blank.");
        TestBearerToken.create("");
    }
}
