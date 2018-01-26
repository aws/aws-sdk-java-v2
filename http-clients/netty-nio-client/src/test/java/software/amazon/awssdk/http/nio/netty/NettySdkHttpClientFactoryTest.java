/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.Test;

public class NettySdkHttpClientFactoryTest {

    @Test
    public void readTimeoutCanOnlyBeAWholeSecond() {
        assertThat(builder().readTimeout(Duration.ofMillis(5000)).build().readTimeout()).contains(Duration.ofSeconds(5));

        assertThatThrownBy(() -> builder().readTimeout(Duration.ofMillis(500)).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void writeTimeoutCanOnlyBeAWholeSecond() {
        assertThat(builder().writeTimeout(Duration.ofMillis(5000)).build().writeTimeout()).contains(Duration.ofSeconds(5));

        assertThatThrownBy(() -> builder().writeTimeout(Duration.ofMillis(500)).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    private NettySdkHttpClientFactory.Builder builder() {
        return NettySdkHttpClientFactory.builder();
    }

}