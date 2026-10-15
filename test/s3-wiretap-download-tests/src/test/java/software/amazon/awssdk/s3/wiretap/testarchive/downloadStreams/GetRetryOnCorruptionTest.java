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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.s3.wiretap.client.feature.builder.ResponseChecksumValidationFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature.AllSync;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.RetryFeature;
import software.amazon.awssdk.s3.wiretap.server.S3RequestFilters;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetMockResponders;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.GetVerifiers;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;
import software.amazon.awssdk.s3.wiretap.testarchive.ChecksumModeOption;
import software.amazon.awssdk.s3.wiretap.testarchive.Feature;
import software.amazon.awssdk.s3.wiretap.testarchive.StoredObjectTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class GetRetryOnCorruptionTest extends AbstractGetBodyTest<S3Client> {

    private static final Logger LOG = LogManager.getLogger(GetRetryOnCorruptionTest.class);

    private static final int SIZE = 1024 * 1024;

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @StoredObjectTest(size = SIZE, partSize = SIZE,
            shapes = StoredObject.Shape.SINGLE_PUT_CRC32)
    @Feature(feature = HttpClientFeature.class, group = AllSync.class)
    @Feature(feature = RetryFeature.class, values = RetryFeature.Default.class)
    @Feature(feature = ResponseChecksumValidationFeature.class,
            values = ResponseChecksumValidationFeature.Unset.class)
    private @interface RetrySweep {
    }

    @RetrySweep
    void corruptionIsRetriedOnlyWhenTheTransferChecksumFailed(S3Client s3, StoredObject object,
                                                              ChecksumModeOption checksumMode) {
        GetObjectRequest request = requestFor(checksumMode, builder -> builder);

        GetMockResponders.register(wiretap(), KEY, object);
        readOrFail(s3, request, (client, req) ->
                client.getObject(req, ResponseTransformer.toBytes()).asByteArray(), "clean pass");
        GetVerifiers.Read cleanRead = singleRead(object);
        GetVerifiers.Integrity integrity = cleanRead.integrity();
        assertTotalRequests(1);

        assertTrue(integrity == GetVerifiers.Integrity.TRANSFER_MD5
                        || integrity == GetVerifiers.Integrity.STORED_CHECKSUM,
                () -> "this class is about a corruption the SDK detects; " + checksumMode
                        + " left nothing to detect it with (" + integrity + ")");
        boolean retryable = integrity == GetVerifiers.Integrity.TRANSFER_MD5;

        wiretap().clearPendingResponses();
        GetMockResponders.registerCorrupting(wiretap(), KEY, object);

        Throwable raised = null;
        try {
            s3.getObject(request, ResponseTransformer.toBytes());
        } catch (Throwable t) {
            raised = t;
        }
        assertNotNull(raised, () -> "a corrupted " + integrity + " read must fail");
        assertIntegrityFailure(raised, integrity, cleanRead);

        if (retryable) {
            assertInstanceOf(RetryableException.class, raised,
                    "a failed transfer checksum is classified retryable");
        } else if (raised instanceof RetryableException) {
            fail("a failed stored-checksum comparison must NOT be retryable — retrying cannot fix "
                    + "bytes that do not match what S3 stored, and doing so would re-transfer the "
                    + "whole object for nothing: " + raised);
        }

        List<software.amazon.awssdk.s3.wiretap.server.CapturedRequest> attempts =
                wiretap().requestsMatching(S3RequestFilters.get());
        LOG.info("{} on {}: {} attempt(s), {}", integrity, object.describe(), attempts.size(),
                raised.getClass().getSimpleName());
        int maxAttempts = maxAttemptsOf(s3);
        assertEquals(retryable ? maxAttempts : 1, attempts.size(),
                () -> "a corrupted " + integrity + " read should cost "
                        + (retryable ? maxAttempts + " attempts (the client's retry strategy)" : "a single attempt")
                        + ", saw " + attempts.size());

        assertTotalRequests(attempts.size());
    }

    private static int maxAttemptsOf(S3Client s3) {
        ClientOverrideConfiguration config = s3.serviceClientConfiguration().overrideConfiguration();
        int maxAttempts = config.retryPolicy().map(policy -> policy.numRetries() + 1)
                .orElseGet(() -> config.retryStrategy().map(strategy -> strategy.maxAttempts())
                        .orElseThrow(() -> new IllegalStateException("the client reports no retry strategy")));
        assertTrue(maxAttempts > 1, () -> "this class needs retries on; the client allows " + maxAttempts + " attempt(s)");
        return maxAttempts;
    }
}
