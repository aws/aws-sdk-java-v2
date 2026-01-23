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
import static org.junit.jupiter.api.Assertions.assertThrows;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

class AwsBasicCredentialsTest {

    private static final String ACCESS_KEY_ID = "accessKeyId";
    private static final String SECRET_ACCESS_KEY = "secretAccessKey";
    private static final String PROVIDER_NAME = "StaticCredentialsProvider";

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(AwsBasicCredentials.class)
                      .withIgnoredFields("validateCredentials")
                      .withIgnoredFields("providerName")
                      .verify();
    }

    @Test
    void emptyBuilder_ThrowsException() {
        assertThrows(NullPointerException.class, () -> AwsBasicCredentials.builder().build());
    }

    @Test
    void builderMissingAccessKeyId_ThrowsException() {
        assertThrows(NullPointerException.class, () -> AwsBasicCredentials.builder()
                                                                            .secretAccessKey(SECRET_ACCESS_KEY)
                                                                            .build());
    }

    @Test
    void create_isSuccessful() {
        AwsBasicCredentials identity = AwsBasicCredentials.create(ACCESS_KEY_ID,
                                                                  SECRET_ACCESS_KEY);
        assertEquals(ACCESS_KEY_ID, identity.accessKeyId());
        assertEquals(SECRET_ACCESS_KEY, identity.secretAccessKey());
    }

    @Test
    void build_isSuccessful() {
        AwsBasicCredentials identity = AwsBasicCredentials.builder()
                                                              .accessKeyId(ACCESS_KEY_ID)
                                                              .secretAccessKey(SECRET_ACCESS_KEY)
                                                              .build();
        assertEquals(ACCESS_KEY_ID, identity.accessKeyId());
        assertEquals(SECRET_ACCESS_KEY, identity.secretAccessKey());
    }

    @Test
    void copy_isSuccessful() {
        AwsBasicCredentials identity = AwsBasicCredentials.builder()
                                                              .accessKeyId(ACCESS_KEY_ID)
                                                              .secretAccessKey(SECRET_ACCESS_KEY)
                                                              .build();
        AwsBasicCredentials copy = identity.copy(c -> c.providerName(PROVIDER_NAME));
        assertEquals(ACCESS_KEY_ID, copy.accessKeyId());
        assertEquals(SECRET_ACCESS_KEY, copy.secretAccessKey());
        assertEquals(PROVIDER_NAME, copy.providerName().get());
    }
}
