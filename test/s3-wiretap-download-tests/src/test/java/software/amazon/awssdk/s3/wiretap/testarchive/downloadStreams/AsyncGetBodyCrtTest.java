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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.s3.wiretap.client.ConfiguredClient;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.ResponseChecksumValidationFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.wiretap.server.CapturedRequest;
import software.amazon.awssdk.s3.wiretap.server.S3RequestFilters;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetVerifiers;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.wiretap.testarchive.ChecksumModeOption;
import software.amazon.awssdk.s3.wiretap.testarchive.Feature;
import software.amazon.awssdk.s3.wiretap.testarchive.StoredObjectTest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class AsyncGetBodyCrtTest extends AbstractGetBodyTest<S3AsyncClient> {

    private static final int SIZE = 1024 * 1024;

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(size = SIZE, partSize = SIZE)
    @Feature(feature = HttpClientFeature.class, values = HttpClientFeature.CrtCreate.class)
    @Feature(feature = ResponseChecksumValidationFeature.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.None.class)
    private @interface CrtGetSweep {
    }

    @CrtGetSweep
    void wholeObjectToBytes(ConfiguredClient configured, StoredObject object,
                            ChecksumModeOption checksumMode) {
        wholeObjectAsCrtReadsIt(configured, object, checksumMode, (client, request) ->
                client.getObject(request, AsyncResponseTransformer.toBytes()).join().asByteArray());
    }

    @CrtGetSweep
    void wholeObjectToBlockingInputStream(ConfiguredClient configured, StoredObject object,
                                          ChecksumModeOption checksumMode) {
        wholeObjectAsCrtReadsIt(configured, object, checksumMode, (client, request) -> {
            try (ResponseInputStream<GetObjectResponse> in = client.getObject(request,
                    AsyncResponseTransformer.toBlockingInputStream()).join()) {
                return in.readAllBytes();
            }
        });
    }

    private void wholeObjectAsCrtReadsIt(ConfiguredClient configured, StoredObject object,
                                         ChecksumModeOption checksumMode,
                                         Reader<S3AsyncClient> reader) {
        runGet(configured.asyncClient(), object, checksumMode, 0, object.size(),
                builder -> builder, reader, GetVerifiers.Scope.RANGE);
    }

    @CrtGetSweep
    void crtRewritesTheRequest(ConfiguredClient configured, StoredObject object,
                               ChecksumModeOption checksumMode) {
        boolean willValidate = crtWillValidate(configured, checksumMode);

        GetMockResponders.register(wiretap(), KEY, object);
        byte[] read = readOrFail(configured.asyncClient(),
                requestFor(checksumMode, builder -> builder),
                (client, request) -> client.getObject(request,
                        AsyncResponseTransformer.toBytes()).join().asByteArray(),
                "clean read");
        assertEquals(object.size(), read.length, () -> "bytes read from " + object.describe());

        GetVerifiers.awaitQuiescence(wiretap());

        List<CapturedRequest> all = wiretap().requests();
        List<GetVerifiers.Read> reads = GetVerifiers.reads(wiretap(), object);
        assertEquals(1, reads.size(), () -> "expected exactly one GET, saw " + reads);
        GetVerifiers.Read only = reads.get(0);

        assertEquals(GetVerifiers.Scope.RANGE, only.scope(),
                "CRT reads a whole object by sending a Range, not an unscoped GET");
        assertEquals(0, only.offset(), () -> "the range should start at 0, saw " + only);
        assertEquals(object.size(), only.length(),
                () -> "the window should resolve to the whole object once S3 clamps CRT's "
                        + "over-long range, saw " + only);
        assertFalse(only.request().headers().containsKey("x-amz-te"),
                () -> "CRT never uses the append-md5 transfer checksum, saw "
                        + only.request().headers());

        assertEquals(willValidate, only.checksumMode(),
                () -> "CRT should carry x-amz-checksum-mode exactly when it means to validate; "
                        + "caller asked for checksumMode=" + checksumMode + " with "
                        + configured.config().describe());
        long heads = all.stream().filter(S3RequestFilters.head()).count();
        assertEquals(willValidate ? 1 : 0, heads,
                () -> "CRT probes with a HeadObject exactly when it means to validate, saw " + all);
        if (willValidate) {
            assertTrue(S3RequestFilters.head().test(all.get(0)),
                    () -> "the HEAD must come first, saw " + all);
        }

        assertReadAccountedFor();
    }

    private static boolean crtWillValidate(ConfiguredClient configured,
                                           ChecksumModeOption checksumMode) {
        ResponseChecksumValidation configuredValidation =
                configured.value(ResponseChecksumValidationFeature.class);
        return checksumMode.enabled()
                || configuredValidation != ResponseChecksumValidation.WHEN_REQUIRED;
    }

    @Override
    protected void assertReadAccountedFor() {
        GetVerifiers.awaitQuiescence(wiretap());
        List<CapturedRequest> gets = wiretap().requestsMatching(S3RequestFilters.get());
        assertEquals(1, gets.size(),
                () -> "expected exactly one GET — more would mean CRT split the read: "
                        + wiretap().requests());
        boolean validating = gets.get(0).headers().containsKey("x-amz-checksum-mode");
        long heads = wiretap().requestsMatching(S3RequestFilters.head()).size();
        assertEquals(validating ? 1 : 0, heads,
                () -> "the HeadObject probe should be present exactly when the GET asks for a "
                        + "checksum: " + wiretap().requests());
        assertTotalRequests(1 + (int) heads);
    }
}
