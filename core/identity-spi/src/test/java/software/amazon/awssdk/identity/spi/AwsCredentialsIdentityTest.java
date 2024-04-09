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

package software.amazon.awssdk.identity.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.internal.DefaultAwsCredentialsIdentity;

public class AwsCredentialsIdentityTest {
    private static final String ACCESS_KEY_ID = "accessKeyId";
    private static final String SECRET_ACCESS_KEY = "secretAccessKey";

    @Test
    public void equalsHashcode() {
        EqualsVerifier.forClass(DefaultAwsCredentialsIdentity.class)
                      .withIgnoredFields("providerName")
                      .verify();
    }

    @Test
    public void emptyBuilder_ThrowsException() {
        assertThrows(NullPointerException.class, () -> AwsCredentialsIdentity.builder().build());
    }

    @Test
    public void builderMissingSecretAccessKey_ThrowsException() {
        assertThrows(NullPointerException.class, () -> AwsCredentialsIdentity.builder().accessKeyId(ACCESS_KEY_ID).build());
    }

    @Test
    public void builderMissingAccessKeyId_ThrowsException() {
        assertThrows(NullPointerException.class, () -> AwsCredentialsIdentity.builder().secretAccessKey(SECRET_ACCESS_KEY).build());
    }

    @Test
    public void create_isSuccessful() {
        AwsCredentialsIdentity identity = AwsCredentialsIdentity.create(ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        assertEquals(ACCESS_KEY_ID, identity.accessKeyId());
        assertEquals(SECRET_ACCESS_KEY, identity.secretAccessKey());
    }

    @Test
    public void build_isSuccessful() {
        AwsCredentialsIdentity identity = AwsCredentialsIdentity.builder()
                                                                .accessKeyId(ACCESS_KEY_ID)
                                                                .secretAccessKey(SECRET_ACCESS_KEY)
                                                                .build();
        assertEquals(ACCESS_KEY_ID, identity.accessKeyId());
        assertEquals(SECRET_ACCESS_KEY, identity.secretAccessKey());
    }
}
