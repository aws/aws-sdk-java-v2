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
import java.time.Duration;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.ResponseChecksumValidationFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.AllSync;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.wiretap.testarchive.ChecksumModeOption;
import software.amazon.awssdk.s3.wiretap.testarchive.Feature;
import software.amazon.awssdk.s3.wiretap.testarchive.StoredObjectTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class GetBodyTest extends AbstractGetBodyTest<S3Client> {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest
    @Feature(feature = HttpClientFeature.class, group = AllSync.class)
    @Feature(feature = ResponseChecksumValidationFeature.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface GetSweep {
    }

    @GetSweep
    void responseInputStream(S3Client s3, StoredObject object, ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, (client, request) -> {
            try (ResponseInputStream<GetObjectResponse> in = client.getObject(request)) {
                return in.readAllBytes();
            }
        });
    }

    @GetSweep
    void getObjectAsBytes(S3Client s3, StoredObject object, ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode,
                (client, request) -> client.getObjectAsBytes(request).asByteArray());
    }

    @GetSweep
    void transformerToBytes(S3Client s3, StoredObject object, ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, (client, request) ->
                client.getObject(request, ResponseTransformer.toBytes()).asByteArray());
    }

    @GetSweep
    void transformerToOutputStream(S3Client s3, StoredObject object,
                                  ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, (client, request) -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            client.getObject(request, ResponseTransformer.toOutputStream(out));
            return out.toByteArray();
        });
    }

    @GetSweep
    void transformerToInputStream(S3Client s3, StoredObject object,
                                  ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, (client, request) -> {
            try (ResponseInputStream<GetObjectResponse> in =
                         client.getObject(request, ResponseTransformer.toInputStream())) {
                return in.readAllBytes();
            }
        });
    }

    @GetSweep
    void transformerToInputStreamWithTimeout(S3Client s3, StoredObject object,
                                             ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, (client, request) -> {
            try (ResponseInputStream<GetObjectResponse> in = client.getObject(request,
                    ResponseTransformer.toInputStream(Duration.ofMinutes(1)))) {
                return in.readAllBytes();
            }
        });
    }

    @GetSweep
    void transformerUnmanaged(S3Client s3, StoredObject object, ChecksumModeOption checksumMode) {
        wholeObject(s3, object, checksumMode, (client, request) -> {
            return client.getObject(request,
                    ResponseTransformer.unmanaged((response, in) -> {
                        try (java.io.InputStream owned = in) {
                            return owned.readAllBytes();
                        } catch (IOException e) {
                            throw new IllegalStateException("draining unmanaged stream", e);
                        }
                    }));
        });
    }

    @GetSweep
    void rangeAlignedToPart(S3Client s3, StoredObject object, ChecksumModeOption checksumMode) {
        range(s3, object, checksumMode, object.partOffset(1), object.partLength(1), AS_BYTES);
    }

    @GetSweep
    void rangeNotAlignedToPart(S3Client s3, StoredObject object, ChecksumModeOption checksumMode) {
        int offset = 1000;
        int length = Math.min(object.partLength(1), object.size() - offset);
        range(s3, object, checksumMode, offset, length, AS_BYTES);
    }

    @GetSweep
    void rangeCoveringWholeObject(S3Client s3, StoredObject object,
                                  ChecksumModeOption checksumMode) {
        range(s3, object, checksumMode, 0, object.size(), AS_BYTES);
    }

    @GetSweep
    void partNumberFirstPart(S3Client s3, StoredObject object, ChecksumModeOption checksumMode) {
        partNumber(s3, object, checksumMode, 1, AS_BYTES);
    }

    @GetSweep
    void partNumberLastPart(S3Client s3, StoredObject object, ChecksumModeOption checksumMode) {
        partNumber(s3, object, checksumMode, object.partCount(), AS_BYTES);
    }

    private static final Reader<S3Client> AS_BYTES =
            (client, request) -> client.getObjectAsBytes(request).asByteArray();

    @GetSweep
    void responseInputStreamAbandonedUnread(S3Client s3, StoredObject object,
                                            ChecksumModeOption checksumMode) {
        GetObjectRequest request = requestFor(checksumMode, builder -> builder);
        GetMockResponders.registerCorrupting(wiretap(), KEY, object);

        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(request)) {
            assertNotNull(in.response(), "the response head should still be available");
        } catch (IOException e) {
            throw new IllegalStateException("closing an abandoned stream should not fail", e);
        } catch (RuntimeException e) {
            fail("an abandoned stream must not raise an integrity error, but got: " + e);
        }

        assertAbandonedResponseRecorded(object);
    }
}
