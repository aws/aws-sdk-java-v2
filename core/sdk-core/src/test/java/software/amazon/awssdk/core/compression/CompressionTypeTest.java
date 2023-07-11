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

package software.amazon.awssdk.core.compression;

import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class CompressionTypeTest {

    @Test
    public void equalsHashcode() {
        EqualsVerifier.forClass(CompressionType.class)
                      .withNonnullFields("id")
                      .verify();
    }

    @Test
    public void compressionType_gzip() {
        CompressionType gzip = CompressionType.GZIP;
        CompressionType gzipFromString = CompressionType.of("gzip");
        assertThat(gzip).isSameAs(gzipFromString);
        assertThat(gzip).isEqualTo(gzipFromString);
    }

    @Test
    public void compressionType_usesSameInstance_when_sameCompressionTypeOfSameValue() {
        CompressionType brotliFromString = CompressionType.of("brotli");
        CompressionType brotliFromStringDuplicate = CompressionType.of("brotli");
        assertThat(brotliFromString).isSameAs(brotliFromStringDuplicate);
        assertThat(brotliFromString).isEqualTo(brotliFromStringDuplicate);
    }
}
