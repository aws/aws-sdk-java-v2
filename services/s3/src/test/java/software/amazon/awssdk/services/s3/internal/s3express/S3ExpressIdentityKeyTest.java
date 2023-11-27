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

package software.amazon.awssdk.services.s3.internal.s3express;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.internal.DefaultAwsCredentialsIdentity;

@ExtendWith(MockitoExtension.class)
class S3ExpressIdentityKeyTest {

    @Mock
    SdkClient sdkClient1;

    @Mock
    SdkClient sdkClient2;

    AwsCredentialsIdentity identity1 = DefaultAwsCredentialsIdentity.builder().accessKeyId("key1").secretAccessKey("secret1").build();
    AwsCredentialsIdentity identity2 = DefaultAwsCredentialsIdentity.builder().accessKeyId("key2").secretAccessKey("secret2").build();

    @Test
    void equalsHashcode() {
        EqualsVerifier.forClass(S3ExpressIdentityKey.class)
                      .withOnlyTheseFields("bucket", "identity")
                      .verify();
    }

    @Test
    void differentClients_DoNotAffectHashcode() {
        Map<S3ExpressIdentityKey, String> map = new HashMap<>();
        S3ExpressIdentityKey key1 = key("a", sdkClient1, identity1);
        S3ExpressIdentityKey key2 = key("a", sdkClient2, identity1);
        map.put(key1, "one");
        map.put(key2, "two");
        assertThat(map).hasSize(1);
        assertThat(map.get(key1)).isEqualTo("two");
    }

    @Test
    void differentProviders_DoNotAffectHashcode() {
        Map<S3ExpressIdentityKey, String> map = new HashMap<>();
        S3ExpressIdentityKey key1 = key("a", sdkClient1, identity1);
        S3ExpressIdentityKey key2 = key("a", sdkClient1, identity1);
        map.put(key1, "one");
        map.put(key2, "two");
        assertThat(map).hasSize(1);
        assertThat(map.get(key1)).isEqualTo("two");
    }

    @Test
    void differentBuckets_ProduceDifferentHashCodes() {
        Map<S3ExpressIdentityKey, String> map = new HashMap<>();
        S3ExpressIdentityKey key1 = key("a", sdkClient1, identity1);
        S3ExpressIdentityKey key2 = key("b", sdkClient1, identity1);
        map.put(key1, "one");
        map.put(key2, "two");
        assertThat(map).hasSize(2);
        assertThat(map.get(key1)).isEqualTo("one");
        assertThat(map.get(key2)).isEqualTo("two");
    }

    @Test
    void differentIdentities_ProduceDifferentHashCodes() {
        Map<S3ExpressIdentityKey, String> map = new HashMap<>();
        S3ExpressIdentityKey key1 = key("a", sdkClient1, identity1);
        S3ExpressIdentityKey key2 = key("a", sdkClient1, identity2);
        map.put(key1, "one");
        map.put(key2, "two");
        assertThat(map).hasSize(2);
        assertThat(map.get(key1)).isEqualTo("one");
        assertThat(map.get(key2)).isEqualTo("two");
    }

    private S3ExpressIdentityKey key(String bucket, SdkClient client,
                                     AwsCredentialsIdentity identity) {
        return S3ExpressIdentityKey.builder()
                                   .bucket(bucket)
                                   .client(client)
                                   .identity(identity)
                                   .build();
    }
}