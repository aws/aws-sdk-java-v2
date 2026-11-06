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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;

class SqsBatchKeyTest {

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/q";

    @Test
    void sameUrlAndNoOverride_areEqual() {
        SqsBatchKey a = SqsBatchKey.create(QUEUE_URL, Optional.empty());
        SqsBatchKey b = SqsBatchKey.create(QUEUE_URL, Optional.empty());

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(a.overrideConfiguration()).isEmpty();
    }

    @Test
    void sameUrlAndEqualOverride_areEqual() {
        AwsRequestOverrideConfiguration config = configWith(new CollidingCredentialsProvider("a"));
        SqsBatchKey a = SqsBatchKey.create(QUEUE_URL, Optional.of(config));
        SqsBatchKey b = SqsBatchKey.create(QUEUE_URL, Optional.of(config.toBuilder().build()));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.overrideConfiguration()).contains(config);
    }

    @Test
    void differentUrl_areNotEqual() {
        SqsBatchKey a = SqsBatchKey.create(QUEUE_URL, Optional.empty());
        SqsBatchKey b = SqsBatchKey.create(QUEUE_URL + "2", Optional.empty());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void overrideVersusNoOverride_areNotEqual() {
        SqsBatchKey a = SqsBatchKey.create(QUEUE_URL, Optional.empty());
        SqsBatchKey b = SqsBatchKey.create(QUEUE_URL,
                                           Optional.of(configWith(new CollidingCredentialsProvider("a"))));

        assertThat(a).isNotEqualTo(b);
        assertThat(b).isNotEqualTo(a);
    }

    @Test
    void unequalOverridesWithCollidingHashCodes_areNotEqual() {
        AwsRequestOverrideConfiguration configA = configWith(new CollidingCredentialsProvider("a"));
        AwsRequestOverrideConfiguration configB = configWith(new CollidingCredentialsProvider("b"));
        assertThat(configA.hashCode()).isEqualTo(configB.hashCode());
        assertThat(configA).isNotEqualTo(configB);

        SqsBatchKey a = SqsBatchKey.create(QUEUE_URL, Optional.of(configA));
        SqsBatchKey b = SqsBatchKey.create(QUEUE_URL, Optional.of(configB));

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void nullQueueUrl_isAccepted() {
        SqsBatchKey a = SqsBatchKey.create(null, Optional.empty());
        SqsBatchKey b = SqsBatchKey.create(null, Optional.empty());

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.queueUrl()).isNull();
    }

    @Test
    void toString_doesNotIncludeCredentials() {
        SqsBatchKey key = SqsBatchKey.create(QUEUE_URL,
                                             Optional.of(configWith(new CollidingCredentialsProvider("secret-a"))));

        assertThat(key.toString()).contains(QUEUE_URL).doesNotContain("secret-a");
    }

    static AwsRequestOverrideConfiguration configWith(AwsCredentialsProvider provider) {
        return AwsRequestOverrideConfiguration.builder().credentialsProvider(provider).build();
    }

    /**
     * A credentials provider whose hash code is constant and whose equality is identity, so two instances are never
     * equal but always collide.
     */
    static final class CollidingCredentialsProvider implements AwsCredentialsProvider {
        private final String secret;

        CollidingCredentialsProvider(String secret) {
            this.secret = secret;
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return AwsBasicCredentials.create("akid", secret);
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public String toString() {
            return "CollidingCredentialsProvider(" + secret + ")";
        }
    }
}
