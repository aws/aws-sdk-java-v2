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

package software.amazon.awssdk.utils.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshResultTest {

    @Test
    void toBuilder_copiesExpiration() {
        Instant staleTime = Instant.parse("2024-01-01T00:00:00Z");
        Instant prefetchTime = staleTime.minusSeconds(240);
        Instant expiration = staleTime.plusSeconds(60);

        RefreshResult<String> original = RefreshResult.builder("value")
                                                      .staleTime(staleTime)
                                                      .prefetchTime(prefetchTime)
                                                      .expiration(expiration)
                                                      .build();

        RefreshResult<String> copy = original.toBuilder().build();

        assertThat(copy.value()).isEqualTo("value");
        assertThat(copy.staleTime()).isEqualTo(staleTime);
        assertThat(copy.prefetchTime()).isEqualTo(prefetchTime);
        assertThat(copy.expiration()).isEqualTo(expiration);
    }

    @Test
    void builder_expirationDefaultsToNull() {
        RefreshResult<String> result = RefreshResult.builder("value").build();

        assertThat(result.expiration()).isNull();
        assertThat(result.staleTime()).isEqualTo(Instant.MAX);
        assertThat(result.prefetchTime()).isEqualTo(Instant.MAX);
    }
}
