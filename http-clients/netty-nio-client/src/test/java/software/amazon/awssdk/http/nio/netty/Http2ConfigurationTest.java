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

package software.amazon.awssdk.http.nio.netty;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Http2ConfigurationTest {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void builder_returnsInstance() {
        assertThat(Http2Configuration.builder()).isNotNull();
    }

    @Test
    public void build_buildsCorrectConfig() {
        long maxStreams = 1;
        int initialWindowSize = 2;

        Http2Configuration config = Http2Configuration.builder()
                .maxStreams(maxStreams)
                .initialWindowSize(initialWindowSize)
                .build();

        assertThat(config.maxStreams()).isEqualTo(maxStreams);
        assertThat(config.initialWindowSize()).isEqualTo(initialWindowSize);
    }

    @Test
    public void builder_toBuilder_roundTrip() {
        Http2Configuration config1 = Http2Configuration.builder()
                .maxStreams(7L)
                .initialWindowSize(42)
                .build();

        Http2Configuration config2 = config1.toBuilder().build();

        assertThat(config1).isEqualTo(config2);
    }

    @Test
    public void builder_maxStream_nullValue_doesNotThrow() {
        Http2Configuration.builder().maxStreams(null);
    }

    @Test
    public void builder_maxStream_negative_throws() {
        expected.expect(IllegalArgumentException.class);
        Http2Configuration.builder().maxStreams(-1L);
    }

    @Test
    public void builder_maxStream_0_throws() {
        expected.expect(IllegalArgumentException.class);
        Http2Configuration.builder().maxStreams(0L);
    }

    @Test
    public void builder_initialWindowSize_nullValue_doesNotThrow() {
        Http2Configuration.builder().initialWindowSize(null);
    }

    @Test
    public void builder_initialWindowSize_negative_throws() {
        expected.expect(IllegalArgumentException.class);
        Http2Configuration.builder().initialWindowSize(-1);
    }

    @Test
    public void builder_initialWindowSize_0_throws() {
        expected.expect(IllegalArgumentException.class);
        Http2Configuration.builder().initialWindowSize(0);
    }
}
