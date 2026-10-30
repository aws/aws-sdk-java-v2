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

package software.amazon.awssdk.s3.wiretap.testarchive.downloadStreams;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.time.Duration;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.ResponseChecksumValidationFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.AllAsyncNonCrt;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.wiretap.testarchive.ChecksumModeOption;
import software.amazon.awssdk.s3.wiretap.testarchive.Feature;
import software.amazon.awssdk.s3.wiretap.testarchive.StoredObjectTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class AsyncGetBodyTest extends AbstractGetBodyTest<S3AsyncClient> {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest
    @Feature(feature = HttpClientFeature.class, group = AllAsyncNonCrt.class)
    @Feature(feature = ResponseChecksumValidationFeature.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface AsyncGetSweep {
    }

    private static final Reader<S3AsyncClient> AS_BYTES = (client, request) ->
            client.getObject(request, AsyncResponseTransformer.toBytes()).join().asByteArray();

    @AsyncGetSweep
    void transformerToBytes(S3AsyncClient s3, StoredObject object,
                            ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, AS_BYTES);
    }

    @AsyncGetSweep
    void transformerToBlockingInputStream(S3AsyncClient s3, StoredObject object,
                                          ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, (client, request) -> {
            try (ResponseInputStream<GetObjectResponse> in = client.getObject(request,
                    AsyncResponseTransformer.toBlockingInputStream()).join()) {
                return in.readAllBytes();
            }
        });
    }

    @AsyncGetSweep
    void transformerToPublisher(S3AsyncClient s3, StoredObject object,
                                ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, (client, request) ->
                drain(client.getObject(request, AsyncResponseTransformer.toPublisher()).join()));
    }

    @AsyncGetSweep
    void transformerToPublisherWithTimeout(S3AsyncClient s3, StoredObject object,
                                           ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, (client, request) -> drain(client.getObject(request,
                AsyncResponseTransformer.toPublisher(Duration.ofMinutes(1))).join()));
    }

    @AsyncGetSweep
    void rangeAlignedToPart(S3AsyncClient s3, StoredObject object,
                            ChecksumModeOption checksumMode) {
        range(s3, object, checksumMode, object.partOffset(1), object.partLength(1), AS_BYTES);
    }

    @AsyncGetSweep
    void rangeNotAlignedToPart(S3AsyncClient s3, StoredObject object,
                               ChecksumModeOption checksumMode) {
        int offset = 1000;
        int length = Math.min(object.partLength(1), object.size() - offset);
        range(s3, object, checksumMode, offset, length, AS_BYTES);
    }

    @AsyncGetSweep
    void rangeCoveringWholeObject(S3AsyncClient s3, StoredObject object,
                                  ChecksumModeOption checksumMode) {
        range(s3, object, checksumMode, 0, object.size(), AS_BYTES);
    }

    @AsyncGetSweep
    void partNumberFirstPart(S3AsyncClient s3, StoredObject object,
                             ChecksumModeOption checksumMode) {
        partNumber(s3, object, checksumMode, 1, AS_BYTES);
    }

    @AsyncGetSweep
    void partNumberLastPart(S3AsyncClient s3, StoredObject object,
                            ChecksumModeOption checksumMode) {
        partNumber(s3, object, checksumMode, object.partCount(), AS_BYTES);
    }

    @AsyncGetSweep
    void blockingInputStreamAbandonedUnread(S3AsyncClient s3, StoredObject object,
                                            ChecksumModeOption checksumMode) {
        GetObjectRequest request = requestFor(checksumMode, builder -> builder);
        GetMockResponders.registerCorrupting(wiretap(), KEY, object);

        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(request,
                AsyncResponseTransformer.toBlockingInputStream()).join()) {
            assertNotNull(in.response(), "the response head should still be available");
        } catch (IOException e) {
            throw new IllegalStateException("closing an abandoned stream should not fail", e);
        } catch (RuntimeException e) {
            fail("an abandoned stream must not raise an integrity error, but got: " + e);
        }

        assertAbandonedResponseRecorded(object);
    }

    private static byte[] drain(ResponsePublisher<GetObjectResponse> publisher) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        publisher.subscribe((ByteBuffer buffer) -> {
            byte[] chunk = new byte[buffer.remaining()];
            buffer.get(chunk);
            out.write(chunk, 0, chunk.length);
        }).join();
        return out.toByteArray();
    }

}
