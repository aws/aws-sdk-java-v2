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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.utils.CompletableFutureUtils.joinLikeSync;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Crc32Checksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.aws.internal.signer.checksums.Sha256Checksum;
import software.amazon.awssdk.utils.BinaryUtils;

public class ChecksumPublisherTest {

    @Test
    public void checksum_computesCorrectSha256() {
        String testString = "AWS SDK for Java";
        String expectedDigest = "004c6bbd87e7fe70109b3bc23c8b1ab8f18a8bede0ed38c9233f6cdfd4f7b5d6";

        SdkChecksum checksum = new Sha256Checksum();
        Flowable<ByteBuffer> publisher = Flowable.just(ByteBuffer.wrap(testString.getBytes(StandardCharsets.UTF_8)));
        ChecksumPublisher checksumPublisher = new ChecksumPublisher(publisher, Collections.singleton(checksum));
        List<String> contents = new ArrayList<>();
        SimpleSubscriber subscriber = new SimpleSubscriber((b) -> contents.add(new String(b.array(), StandardCharsets.UTF_8)));
        checksumPublisher.subscribe(subscriber);

        joinLikeSync(checksumPublisher.checksum());
        String computedDigest = BinaryUtils.toHex(checksum.getChecksumBytes());

        assertThat(computedDigest).isEqualTo(expectedDigest);
        assertThat(contents).containsExactly(testString);
    }

    @Test
    public void checksum_withMultipleChecksums_shouldComputeCorrectChecksumsAndForwardsEvents() {
        String testString = "AWS SDK for Java";
        String expectedSha256Digest = "004c6bbd87e7fe70109b3bc23c8b1ab8f18a8bede0ed38c9233f6cdfd4f7b5d6";
        String expectedCrc32Digest = "4ac37ece";

        SdkChecksum sha256Checksum = new Sha256Checksum();
        SdkChecksum crc32Checksum = new Crc32Checksum();
        Flowable<ByteBuffer> publisher = Flowable.just(ByteBuffer.wrap(testString.getBytes(StandardCharsets.UTF_8)));
        ChecksumPublisher checksumPublisher = new ChecksumPublisher(publisher, Arrays.asList(sha256Checksum, crc32Checksum));
        List<String> contents = new ArrayList<>();
        SimpleSubscriber subscriber = new SimpleSubscriber((b) -> contents.add(new String(b.array(), StandardCharsets.UTF_8)));
        checksumPublisher.subscribe(subscriber);

        joinLikeSync(checksumPublisher.checksum());
        String computedSha256Digest = BinaryUtils.toHex(sha256Checksum.getChecksumBytes());
        String computedCrc32Digest = BinaryUtils.toHex(crc32Checksum.getChecksumBytes());

        assertThat(computedSha256Digest).isEqualTo(expectedSha256Digest);
        assertThat(computedCrc32Digest).isEqualTo(expectedCrc32Digest);

        assertThat(contents).containsExactly(testString);
    }


    @Test
    public void checksum_withMultipleEventsAndMultipleChecksums_shouldComputeCorrectChecksumsAndForwardsEvents() {
        String testString = "AWS SDK for Java";
        String testString2 = " is really fun and cool";
        String testString3 = " and extra awesome!";
        String expectedSha256Digest = "5970c8c61c7eba274d2843f12216453c3424c6bf36a30e031fabd01f848937c7";
        String expectedCrc32Digest = "79d29801";

        SdkChecksum sha256Checksum = new Sha256Checksum();
        SdkChecksum crc32Checksum = new Crc32Checksum();
        Flowable<ByteBuffer> publisher = Flowable.just(
            ByteBuffer.wrap(testString.getBytes(StandardCharsets.UTF_8)),
            ByteBuffer.wrap(testString2.getBytes(StandardCharsets.UTF_8)),
            ByteBuffer.wrap(testString3.getBytes(StandardCharsets.UTF_8))
        );
        ChecksumPublisher checksumPublisher = new ChecksumPublisher(publisher, Arrays.asList(sha256Checksum, crc32Checksum));
        List<String> contents = new ArrayList<>();
        SimpleSubscriber subscriber = new SimpleSubscriber((b) -> contents.add(new String(b.array(), StandardCharsets.UTF_8)));
        checksumPublisher.subscribe(subscriber);

        joinLikeSync(checksumPublisher.checksum());
        String computedSha256Digest = BinaryUtils.toHex(sha256Checksum.getChecksumBytes());
        String computedCrc32Digest = BinaryUtils.toHex(crc32Checksum.getChecksumBytes());

        assertThat(computedSha256Digest).isEqualTo(expectedSha256Digest);
        assertThat(computedCrc32Digest).isEqualTo(expectedCrc32Digest);

        assertThat(contents).containsExactly(testString, testString2, testString3);
    }

    @Test
    public void checksum_futureBeforeSubscribe_throws() {
        String testString = "Never read.";
        Flowable<ByteBuffer> publisher = Flowable.just(ByteBuffer.wrap(testString.getBytes(StandardCharsets.UTF_8)));
        ChecksumPublisher checksumPublisher = new ChecksumPublisher(publisher, Collections.emptyList());

        assertThrows(IllegalStateException.class, checksumPublisher::checksum);
    }
}
