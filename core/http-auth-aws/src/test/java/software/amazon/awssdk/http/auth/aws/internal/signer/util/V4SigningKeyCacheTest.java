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

package software.amazon.awssdk.http.auth.aws.internal.signer.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.spi.signer.HttpSigner.SIGNING_CLOCK;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

class V4SigningKeyCacheTest {
    private static final Instant DAY_ONE = Instant.parse("2026-11-20T00:00:00Z");
    private static final Instant DAY_ONE_LATE = Instant.parse("2026-11-20T23:59:59.999Z");
    private static final Instant DAY_TWO = Instant.parse("2026-11-21T00:00:00Z");

    @Test
    void get_whenKeyWasNeverPut_returnsNull() {
        V4SigningKeyCache cache = new V4SigningKeyCache(10);

        assertThat(cache.get(key("secret"), DAY_ONE)).isNull();
    }

    @Test
    void get_onSameUtcDay_returnsCachedKey() {
        V4SigningKeyCache cache = new V4SigningKeyCache(10);
        byte[] signingKey = {1, 2, 3};

        cache.put(key("secret"), signingKey, DAY_ONE);

        assertThat(cache.get(key("secret"), DAY_ONE_LATE)).containsExactly(1, 2, 3).isNotSameAs(signingKey);
    }

    @Test
    void put_whenCallerModifiesArrayAfterPut_doesNotChangeCachedKey() {
        V4SigningKeyCache cache = new V4SigningKeyCache(10);
        byte[] signingKey = {1, 2, 3};
        cache.put(key("secret"), signingKey, DAY_ONE);

        signingKey[0] = 9;

        assertThat(cache.get(key("secret"), DAY_ONE)).containsExactly(1, 2, 3);
    }

    @Test
    void get_whenCallerModifiesReturnedArray_doesNotChangeCachedKey() {
        V4SigningKeyCache cache = new V4SigningKeyCache(10);
        cache.put(key("secret"), new byte[] {1, 2, 3}, DAY_ONE);

        cache.get(key("secret"), DAY_ONE)[0] = 9;

        assertThat(cache.get(key("secret"), DAY_ONE)).containsExactly(1, 2, 3);
    }

    @ParameterizedTest(name = "put at {0}, get at {1}")
    @CsvSource({
        "1969-12-31T23:59:59Z, 1970-01-01T00:00:00Z",
        "1969-12-30T12:00:00Z, 1969-12-31T00:00:00Z"
    })
    void get_acrossUtcDayBoundaryBeforeEpoch_returnsNull(String putInstant, String getInstant) {
        V4SigningKeyCache cache = new V4SigningKeyCache(10);

        cache.put(key("secret"), new byte[] {1}, Instant.parse(putInstant));

        assertThat(cache.get(key("secret"), Instant.parse(getInstant))).isNull();
    }

    @Test
    void get_onSameUtcDayBeforeEpoch_returnsCachedKey() {
        V4SigningKeyCache cache = new V4SigningKeyCache(10);

        cache.put(key("secret"), new byte[] {1}, Instant.parse("1969-12-31T00:00:00Z"));

        assertThat(cache.get(key("secret"), Instant.parse("1969-12-31T23:59:59Z"))).containsExactly(1);
    }

    @Test
    void get_onDifferentUtcDay_returnsNull() {
        V4SigningKeyCache cache = new V4SigningKeyCache(10);

        cache.put(key("secret"), new byte[] {1}, DAY_ONE);

        assertThat(cache.get(key("secret"), DAY_TWO)).isNull();
    }

    @Test
    void get_withDifferentKeyComponent_returnsNull() {
        V4SigningKeyCache cache = new V4SigningKeyCache(10);

        cache.put(new V4SigningKeyCache.CacheKey("secret", "us-east-1", "dynamodb"), new byte[] {1}, DAY_ONE);

        assertThat(cache.get(new V4SigningKeyCache.CacheKey("other", "us-east-1", "dynamodb"), DAY_ONE)).isNull();
        assertThat(cache.get(new V4SigningKeyCache.CacheKey("secret", "us-west-2", "dynamodb"), DAY_ONE)).isNull();
        assertThat(cache.get(new V4SigningKeyCache.CacheKey("secret", "us-east-1", "s3"), DAY_ONE)).isNull();
    }

    @Test
    void put_whenFull_evictsOldestEntry() {
        V4SigningKeyCache cache = new V4SigningKeyCache(2);

        cache.put(key("first"), new byte[] {1}, DAY_ONE);
        cache.put(key("second"), new byte[] {2}, DAY_ONE);
        cache.put(key("third"), new byte[] {3}, DAY_ONE);

        assertThat(cache.get(key("first"), DAY_ONE)).isNull();
        assertThat(cache.get(key("second"), DAY_ONE)).containsExactly(2);
        assertThat(cache.get(key("third"), DAY_ONE)).containsExactly(3);
    }

    @Test
    void put_withExistingKey_replacesEntry() {
        V4SigningKeyCache cache = new V4SigningKeyCache(10);

        cache.put(key("secret"), new byte[] {1}, DAY_ONE);
        cache.put(key("secret"), new byte[] {2}, DAY_TWO);

        assertThat(cache.get(key("secret"), DAY_ONE)).isNull();
        assertThat(cache.get(key("secret"), DAY_TWO)).containsExactly(2);
    }

    @Test
    void constructor_withZeroMaxSize_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new V4SigningKeyCache(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxSize must be positive");
    }

    @Test
    void cacheKey_equalsAndHashCode_useAllComponents() {
        EqualsVerifier.forClass(V4SigningKeyCache.CacheKey.class)
                      .withNonnullFields("secretKey", "region", "service")
                      .withCachedHashCode("hashCode", "computeHashCode", key("secret"))
                      .verify();
    }

    @Test
    void sign_calledTwiceWithSameCredentials_doesNotMutateCachedSigningKey() {
        String secret = "V4SigningKeyCacheTest-sign-twice-secret";
        SignRequest<AwsCredentialsIdentity> request =
            SignRequest.builder(AwsCredentialsIdentity.create("akid", secret))
                       .request(SdkHttpRequest.builder()
                                              .method(SdkHttpMethod.GET)
                                              .uri(URI.create("https://dynamodb.us-east-1.amazonaws.com/"))
                                              .build())
                       .putProperty(REGION_NAME, "us-east-1")
                       .putProperty(SERVICE_SIGNING_NAME, "dynamodb")
                       .putProperty(SIGNING_CLOCK, Clock.fixed(DAY_ONE, ZoneOffset.UTC))
                       .build();
        V4SigningKeyCache.CacheKey cacheKey = new V4SigningKeyCache.CacheKey(secret, "us-east-1", "dynamodb");
        AwsV4HttpSigner signer = AwsV4HttpSigner.create();

        signer.sign(request);
        byte[] cachedAfterFirstSign = V4SigningKeyCache.sharedGet(cacheKey, DAY_ONE);
        assertThat(cachedAfterFirstSign).isNotNull();
        byte[] contentsAfterFirstSign = cachedAfterFirstSign.clone();
        signer.sign(request);

        assertThat(V4SigningKeyCache.sharedGet(cacheKey, DAY_ONE)).containsExactly(contentsAfterFirstSign);
    }

    @Test
    void cacheKey_withSameComponents_isEqualWithSameHashCode() {
        V4SigningKeyCache.CacheKey a = key("secret");
        V4SigningKeyCache.CacheKey b = key("secret");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(key("other"));
    }

    @Test
    void deriveSigningKey_calledTwiceOnSameDay_returnsEqualKeyInNewArray() {
        AwsCredentialsIdentity credentials = AwsCredentialsIdentity.create("akid", "V4SigningKeyCacheTest-secret");
        CredentialScope scope = new CredentialScope("us-east-1", "dynamodb", DAY_ONE);

        byte[] first = SignerUtils.deriveSigningKey(credentials, scope);
        byte[] second = SignerUtils.deriveSigningKey(credentials, new CredentialScope("us-east-1", "dynamodb", DAY_ONE_LATE));

        assertThat(second).isEqualTo(first).isNotSameAs(first);
    }

    @Test
    void deriveSigningKey_whenCallerModifiesKeyFromCacheMiss_doesNotChangeLaterKeys() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("akid", "V4SigningKeyCacheTest-cache-miss-secret");
        CredentialScope scope = new CredentialScope("us-east-1", "dynamodb", DAY_ONE);
        byte[] first = SignerUtils.deriveSigningKey(credentials, scope);
        byte[] expected = first.clone();

        first[0]++;

        assertThat(SignerUtils.deriveSigningKey(credentials, scope)).containsExactly(expected);
    }

    private static V4SigningKeyCache.CacheKey key(String secret) {
        return new V4SigningKeyCache.CacheKey(secret, "us-east-1", "dynamodb");
    }
}
