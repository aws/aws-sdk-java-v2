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

package software.amazon.awssdk.services.defaultsmode;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.defaultretrymode.DefaultRetryModeAsyncClient;
import software.amazon.awssdk.services.defaultretrymode.DefaultRetryModeClient;

public class DefaultRetryModeTest {

    @Test
    void defaultRetryModeSyncClient_whenSetInCustomization_shouldBeStandardByDefault() {
        DefaultRetryModeClient client = DefaultRetryModeClient.builder()
                                                              .region(Region.US_WEST_2)
                                                              .build();
        assertThat(client.serviceClientConfiguration().overrideConfiguration().retryStrategy().get())
            .isInstanceOf(StandardRetryStrategy.class);
    }

    @Test
    void defaultRetryModeAsyncClient_whenSetInCustomization_shouldBeStandardByDefault() {
        DefaultRetryModeAsyncClient client = DefaultRetryModeAsyncClient.builder()
                                                                        .region(Region.US_WEST_2)
                                                                        .build();
        assertThat(client.serviceClientConfiguration().overrideConfiguration().retryStrategy().get())
            .isInstanceOf(StandardRetryStrategy.class);
    }

    @Test
    void syncClient_overrideRetryStrategy_shouldOverride() {
        DefaultRetryModeClient client =
            DefaultRetryModeClient.builder()
                                       .region(Region.US_WEST_2)
                                       .overrideConfiguration(conf -> conf.retryStrategy(RetryMode.LEGACY))
                                       .build();
        assertThat(client.serviceClientConfiguration().overrideConfiguration().retryStrategy().get())
            .isInstanceOf(LegacyRetryStrategy.class);

    }

    @Test
    void asyncClient_overrideRetryStrategy_shouldOverride() {
        DefaultRetryModeAsyncClient client =
            DefaultRetryModeAsyncClient.builder()
                                       .region(Region.US_WEST_2)
                                       .overrideConfiguration(conf -> conf.retryStrategy(RetryMode.LEGACY))
                                       .build();
        assertThat(client.serviceClientConfiguration().overrideConfiguration().retryStrategy().get())
            .isInstanceOf(LegacyRetryStrategy.class);
    }
}
