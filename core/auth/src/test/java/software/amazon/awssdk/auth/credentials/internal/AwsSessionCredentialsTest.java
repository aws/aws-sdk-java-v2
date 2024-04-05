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

package software.amazon.awssdk.auth.credentials.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

class AwsSessionCredentialsTest {

    private static final String ACCESS_KEY_ID = "accessKeyId";
    private static final String SECRET_ACCESS_KEY = "secretAccessKey";
    private static final String SESSION_TOKEN = "sessionToken";
    private static final String PROVIDER_NAME = "StaticCredentialsProvider";
    private static final String ACCOUNT_ID = "accountId";

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(AwsSessionCredentials.class)
                      .withIgnoredFields("providerName")
                      .verify();
    }

    @Test
    void emptyBuilder_ThrowsException() {
        assertThrows(NullPointerException.class, () -> AwsSessionCredentials.builder().build());
    }

    @Test
    void builderMissingSessionToken_ThrowsException() {
        assertThrows(NullPointerException.class, () -> AwsSessionCredentials.builder()
                                                                            .accessKeyId(ACCESS_KEY_ID)
                                                                            .secretAccessKey(SECRET_ACCESS_KEY)
                                                                            .build());
    }

    @Test
    void builderMissingAccessKeyId_ThrowsException() {
        assertThrows(NullPointerException.class, () -> AwsSessionCredentials.builder()
                                                                            .secretAccessKey(SECRET_ACCESS_KEY)
                                                                            .sessionToken(SESSION_TOKEN)
                                                                            .build());
    }

    @Test
    void create_isSuccessful() {
        AwsSessionCredentials identity = AwsSessionCredentials.create(ACCESS_KEY_ID,
                                                                      SECRET_ACCESS_KEY,
                                                                      SESSION_TOKEN);
        assertEquals(ACCESS_KEY_ID, identity.accessKeyId());
        assertEquals(SECRET_ACCESS_KEY, identity.secretAccessKey());
        assertEquals(SESSION_TOKEN, identity.sessionToken());
        assertFalse(identity.accountId().isPresent());
    }

    @Test
    void build_isSuccessful() {
        AwsSessionCredentials identity = AwsSessionCredentials.builder()
                                                              .accessKeyId(ACCESS_KEY_ID)
                                                              .secretAccessKey(SECRET_ACCESS_KEY)
                                                              .sessionToken(SESSION_TOKEN)
                                                              .accountId(ACCOUNT_ID)
                                                              .build();
        assertEquals(ACCESS_KEY_ID, identity.accessKeyId());
        assertEquals(SECRET_ACCESS_KEY, identity.secretAccessKey());
        assertEquals(SESSION_TOKEN, identity.sessionToken());
        assertTrue(identity.accountId().isPresent());
        assertEquals(ACCOUNT_ID, identity.accountId().get());
    }

    @Test
    void copy_isSuccessful() {
        AwsSessionCredentials identity = AwsSessionCredentials.builder()
                                                              .accessKeyId(ACCESS_KEY_ID)
                                                              .secretAccessKey(SECRET_ACCESS_KEY)
                                                              .sessionToken(SESSION_TOKEN)
                                                              .build();
        AwsSessionCredentials copy = identity.copy(c -> c.providerName(PROVIDER_NAME));
        assertEquals(ACCESS_KEY_ID, copy.accessKeyId());
        assertEquals(SECRET_ACCESS_KEY, copy.secretAccessKey());
        assertEquals(SESSION_TOKEN, copy.sessionToken());
        assertEquals(PROVIDER_NAME, copy.providerName().get());
    }
}
