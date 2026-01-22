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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC64NVME;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;
import static software.amazon.awssdk.http.auth.aws.internal.signer.FlexibleChecksummer.option;

import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;
import software.amazon.awssdk.utils.BinaryUtils;

public class FlexibleChecksummerTest {

    ContentStreamProvider payload = () -> new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8));
    Publisher<ByteBuffer> payloadAsync = Flowable.just(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));


    SdkHttpRequest.Builder request = SdkHttpRequest.builder()
                                                   .uri(URI.create("https://localhost"))
                                                   .method(SdkHttpMethod.GET);

    @Test
    public void checksummer_withNoChecksums_shouldNotAddAnyChecksum() {
        FlexibleChecksummer checksummer = new FlexibleChecksummer(PayloadChecksumStore.create());
        SdkHttpRequest expectedRequest = request.build();

        checksummer.checksum(payload, request);

        assertEquals(expectedRequest.headers(), request.build().headers());
    }

    @Test
    public void checksummerAsync_withNoChecksums_shouldNotAddAnyChecksum() {
        FlexibleChecksummer checksummer = new FlexibleChecksummer(PayloadChecksumStore.create());
        SdkHttpRequest expectedRequest = request.build();

        checksummer.checksum(payloadAsync, request);

        assertEquals(expectedRequest.headers(), request.build().headers());
    }

    @Test
    public void checksummer_withOneChecksum_shouldAddOneChecksum() {
        FlexibleChecksummer checksummer = new FlexibleChecksummer(
            PayloadChecksumStore.create(),
            option().headerName("sha256").algorithm(SHA256).formatter(BinaryUtils::toHex).build()
        );
        SdkHttpRequest expectedRequest = request
            .putHeader("sha256", "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae")
            .build();

        checksummer.checksum(payload, request);

        assertEquals(expectedRequest.headers(), request.build().headers());
    }

    @Test
    public void checksummerAsync_withOneChecksum_shouldAddOneChecksum() {
        FlexibleChecksummer checksummer = new FlexibleChecksummer(
            PayloadChecksumStore.create(),
            option().headerName("sha256").algorithm(SHA256).formatter(BinaryUtils::toBase64).build()
        );
        SdkHttpRequest expectedRequest = request
            .putHeader("sha256", "LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564=")
            .build();

        checksummer.checksum(payloadAsync, request);

        assertEquals(expectedRequest.headers(), request.build().headers());
    }

    @Test
    public void checksummer_withMultipleChecksums_shouldAddAllChecksums() {
        FlexibleChecksummer checksummer = new FlexibleChecksummer(
            PayloadChecksumStore.create(),
            option().headerName("sha256").algorithm(SHA256).formatter(BinaryUtils::toHex).build(),
            option().headerName("crc32").algorithm(CRC32).formatter(BinaryUtils::toBase64).build(),
            option().headerName("crc64nvme").algorithm(CRC64NVME).formatter(BinaryUtils::toBase64).build()
        );
        SdkHttpRequest expectedRequest = request
            .putHeader("sha256", "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae")
            .putHeader("crc32", "jHNlIQ==")
            .putHeader("crc64nvme", "5O33DmauDQI=")
            .build();

        checksummer.checksum(payload, request);

        assertEquals(expectedRequest.headers(), request.build().headers());
    }

    @Test
    public void checksummerAsync_withMultipleChecksums_shouldAddAllChecksums() {
        FlexibleChecksummer checksummer = new FlexibleChecksummer(
            PayloadChecksumStore.create(),
            option().headerName("sha256").algorithm(SHA256).formatter(BinaryUtils::toBase64).build(),
            option().headerName("crc32").algorithm(CRC32).formatter(BinaryUtils::toHex).build(),
            option().headerName("crc64nvme").algorithm(CRC64NVME).formatter(BinaryUtils::toBase64).build()
        );
        SdkHttpRequest expectedRequest = request
            .putHeader("sha256", "LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564=")
            .putHeader("crc32", "8c736521")
            .putHeader("crc64nvme", "5O33DmauDQI=")
            .build();

        checksummer.checksum(payloadAsync, request);

        assertEquals(expectedRequest.headers(), request.build().headers());
    }

    @Test
    public void checksummer_withCachedValue_shouldPreferCachedValue() {
        byte[] checksumValue = "HelloWorld".getBytes(StandardCharsets.UTF_8);
        PayloadChecksumStore cache = PayloadChecksumStore.create();
        cache.putChecksumValue(SHA256, checksumValue);

        FlexibleChecksummer checksummer = new FlexibleChecksummer(
            cache,
            option().headerName("sha256").algorithm(SHA256).formatter(BinaryUtils::toBase64).build()
        );

        checksummer.checksum(payload, request);

        assertThat(request.firstMatchingHeader("sha256")).hasValue(BinaryUtils.toBase64(checksumValue));
    }

    @Test
    public void checksummer_noCachedValue_shouldCacheComputedValue() {
        PayloadChecksumStore cache = PayloadChecksumStore.create();

        FlexibleChecksummer checksummer = new FlexibleChecksummer(
            cache,
            option().headerName("sha256").algorithm(SHA256).formatter(BinaryUtils::toBase64).build()
        );

        checksummer.checksum(payload, request);

        assertThat(cache.getChecksumValue(SHA256))
            .isEqualTo(BinaryUtils.fromBase64("LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564="));
    }

    @Test
    void checksummerAsync_withCachedValue_shouldPreferCachedValue() {
        byte[] checksumValue = "HelloWorld".getBytes(StandardCharsets.UTF_8);
        PayloadChecksumStore cache = PayloadChecksumStore.create();
        cache.putChecksumValue(SHA256, checksumValue);

        FlexibleChecksummer checksummer = new FlexibleChecksummer(
            cache,
            option().headerName("sha256").algorithm(SHA256).formatter(BinaryUtils::toBase64).build()
        );

        checksummer.checksum(payloadAsync, request);

        assertThat(request.firstMatchingHeader("sha256")).hasValue(BinaryUtils.toBase64(checksumValue));
    }

    @Test
    void checksummerAsync_noCachedValue_shouldCacheComputedValue() {
        PayloadChecksumStore cache = PayloadChecksumStore.create();

        FlexibleChecksummer checksummer = new FlexibleChecksummer(
            cache,
            option().headerName("sha256").algorithm(SHA256).formatter(BinaryUtils::toBase64).build()
        );

        checksummer.checksum(payloadAsync, request);

        assertThat(BinaryUtils.toBase64(cache.getChecksumValue(SHA256)))
            .isEqualTo("LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564=");
    }
}
