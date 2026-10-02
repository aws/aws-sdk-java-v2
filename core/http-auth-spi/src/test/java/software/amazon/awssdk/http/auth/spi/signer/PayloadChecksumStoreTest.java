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

package software.amazon.awssdk.http.auth.spi.signer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.spi.internal.signer.DefaultPayloadChecksumStore;

public class PayloadChecksumStoreTest {
    private static final ChecksumAlgorithm ALGORITHM  = () -> "crc32";

    @Test
    void putChecksumValue_noPreviousEntry_returnsNull() {
        DefaultPayloadChecksumStore cache = new DefaultPayloadChecksumStore();

        byte[] previous = cache.putChecksumValue(ALGORITHM, new byte[] {1, 2, 3});

        assertThat(previous).isNull();
    }

    @Test
    public void putChecksumValue_previousEntry_returnsValue() {
        DefaultPayloadChecksumStore cache = new DefaultPayloadChecksumStore();

        byte[] previous = {1, 2, 3};
        cache.putChecksumValue(ALGORITHM, previous);

        byte[] cached = cache.putChecksumValue(ALGORITHM, new byte[] {4, 5, 6});

        assertThat(cached).isEqualTo(previous);
    }

    @Test
    public void getChecksumValue_noEntry_returnsNull() {
        DefaultPayloadChecksumStore cache = new DefaultPayloadChecksumStore();

        assertThat(cache.getChecksumValue(ALGORITHM)).isNull();
    }

    @Test
    public void getChecksumValue_hasEntry_returnsValue() {
        DefaultPayloadChecksumStore cache = new DefaultPayloadChecksumStore();

        byte[] value = {1, 2, 3};
        cache.putChecksumValue(ALGORITHM, value);

        assertThat(cache.getChecksumValue(ALGORITHM)).isEqualTo(value);
    }

    @Test
    void containsChecksumValue_noEntry_returnsFalse() {
        DefaultPayloadChecksumStore cache = new DefaultPayloadChecksumStore();
        assertThat(cache.containsChecksumValue(ALGORITHM)).isFalse();
    }

    @Test
    void containsChecksumValue_hasEntry_returnsValue() {
        DefaultPayloadChecksumStore cache = new DefaultPayloadChecksumStore();
        byte[] value = {1, 2, 3};
        cache.putChecksumValue(ALGORITHM, value);
        assertThat(cache.containsChecksumValue(ALGORITHM)).isTrue();
    }
}
