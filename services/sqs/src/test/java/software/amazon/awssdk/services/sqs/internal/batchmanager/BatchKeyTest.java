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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;

class BatchKeyTest {

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/a";

    private static AwsRequestOverrideConfiguration configWithCredentials(String accessKeyId) {
        return AwsRequestOverrideConfiguration.builder()
                                              .credentialsProvider(StaticCredentialsProvider.create(
                                                  AwsBasicCredentials.create(accessKeyId, "secret")))
                                              .build();
    }

    @Test
    void equalQueueUrlAndEqualOverrideConfiguration_areEqual() {
        AwsRequestOverrideConfiguration config = configWithCredentials("akid");
        AwsRequestOverrideConfiguration equalConfig = config.toBuilder().build();
        assertThat(equalConfig).isEqualTo(config);

        BatchKey key = BatchKey.create(QUEUE_URL, config);
        BatchKey equalKey = BatchKey.create(QUEUE_URL, equalConfig);

        assertThat(equalKey).isEqualTo(key);
        assertThat(equalKey.hashCode()).isEqualTo(key.hashCode());
    }

    @Test
    void equalQueueUrlAndNonEqualOverrideConfigurations_areNotEqual() {
        BatchKey key = BatchKey.create(QUEUE_URL, configWithCredentials("akid1"));
        BatchKey otherKey = BatchKey.create(QUEUE_URL, configWithCredentials("akid2"));

        assertThat(otherKey).isNotEqualTo(key);
    }

    @Test
    void differentQueueUrlsAndEqualOverrideConfiguration_areNotEqual() {
        AwsRequestOverrideConfiguration config = configWithCredentials("akid");

        BatchKey key = BatchKey.create(QUEUE_URL, config);
        BatchKey otherKey = BatchKey.create(QUEUE_URL + "-other", config);

        assertThat(otherKey).isNotEqualTo(key);
    }

    @Test
    void noOverrideConfigurationAndPresentOverrideConfiguration_areNotEqual() {
        BatchKey withoutConfig = BatchKey.create(QUEUE_URL, null);
        BatchKey withConfig = BatchKey.create(QUEUE_URL, configWithCredentials("akid"));

        assertThat(withConfig).isNotEqualTo(withoutConfig);
        assertThat(withoutConfig.overrideConfiguration()).isEmpty();
        assertThat(withConfig.overrideConfiguration()).isPresent();
    }

    @Test
    void queueUrlAndOverrideConfigurationAreSeparateFields_soConcatenationCannotAlias() {
        AwsRequestOverrideConfiguration config = configWithCredentials("akid");

        // The string key this fix replaces was queueUrl + config.hashCode(), which made these two requests collide.
        BatchKey keyWithConfig = BatchKey.create(QUEUE_URL, config);
        BatchKey aliasedKeyWithoutConfig = BatchKey.create(QUEUE_URL + config.hashCode(), null);

        assertThat(aliasedKeyWithoutConfig).isNotEqualTo(keyWithConfig);
    }

    @Test
    void emptyOverrideConfiguration_isNotNormalizedToNoOverrideConfiguration() {
        BatchKey emptyConfigKey = BatchKey.create(QUEUE_URL, AwsRequestOverrideConfiguration.builder().build());
        BatchKey noConfigKey = BatchKey.create(QUEUE_URL, null);

        assertThat(emptyConfigKey).isNotEqualTo(noConfigKey);
    }

    @Test
    void nullQueueUrl_throws() {
        assertThatThrownBy(() -> BatchKey.create(null, configWithCredentials("akid")))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("queueUrl");
    }

    @Test
    void queueUrlIsRetained() {
        assertThat(BatchKey.create(QUEUE_URL, null).queueUrl()).isEqualTo(QUEUE_URL);
    }

    @Test
    void toString_doesNotExposeOverrideConfiguration() {
        AwsRequestOverrideConfiguration config = AwsRequestOverrideConfiguration.builder()
                                                                               .apiCallTimeout(Duration.ofSeconds(3))
                                                                               .putHeader("secret-header", "secret-value")
                                                                               .build();

        String batchKeyString = BatchKey.create(QUEUE_URL, config).toString();

        assertThat(batchKeyString).contains(QUEUE_URL)
                                  .contains("hasOverrideConfiguration=true")
                                  .doesNotContain(config.toString())
                                  .doesNotContain("secret-value");
    }
}
