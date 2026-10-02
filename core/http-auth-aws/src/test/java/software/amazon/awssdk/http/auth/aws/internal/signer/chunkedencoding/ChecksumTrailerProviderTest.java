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

package software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Pair;

public class ChecksumTrailerProviderTest {

    @Test
    void reset_resetsChecksum() {
        SdkChecksum mockCrc32 = mock(SdkChecksum.class);

        ChecksumTrailerProvider provider = new ChecksumTrailerProvider(mockCrc32,
                                                                       "my-checksum",
                                                                       DefaultChecksumAlgorithm.CRC32,
                                                                       PayloadChecksumStore.create());

        provider.reset();

        verify(mockCrc32).reset();
    }

    @Test
    void get_cacheContainsChecksumValue_usesCachedValued() {
        SdkChecksum mockCrc32 = mock(SdkChecksum.class);
        byte[] checksumValue = "Hello".getBytes(StandardCharsets.UTF_8);
        when(mockCrc32.getChecksumBytes()).thenReturn(checksumValue);

        PayloadChecksumStore cache = PayloadChecksumStore.create();
        cache.putChecksumValue(DefaultChecksumAlgorithm.CRC32, checksumValue);

        ChecksumTrailerProvider provider = new ChecksumTrailerProvider(mockCrc32,
                                                                       "my-checksum-crc32",
                                                                       DefaultChecksumAlgorithm.CRC32,
                                                                       cache);

        Pair<String, List<String>> result = provider.get();

        assertThat(result.right().get(0)).isEqualTo(BinaryUtils.toBase64(checksumValue));
        verifyNoInteractions(mockCrc32);
    }

    @Test
    public void get_cacheEmpty_storesSdkChecksumValue() {
        SdkChecksum mockCrc32 = mock(SdkChecksum.class);
        byte[] checksumValue = "Hello".getBytes(StandardCharsets.UTF_8);
        when(mockCrc32.getChecksumBytes()).thenReturn(checksumValue);

        PayloadChecksumStore cache = PayloadChecksumStore.create();

        ChecksumTrailerProvider provider = new ChecksumTrailerProvider(mockCrc32,
                                                                       "my-checksum-crc32",
                                                                       DefaultChecksumAlgorithm.CRC32,
                                                                       cache);

        Pair<String, List<String>> result = provider.get();

        assertThat(result.right().get(0)).isEqualTo(BinaryUtils.toBase64(checksumValue));
        assertThat(cache.getChecksumValue(DefaultChecksumAlgorithm.CRC32)).isEqualTo(checksumValue);
    }
}
