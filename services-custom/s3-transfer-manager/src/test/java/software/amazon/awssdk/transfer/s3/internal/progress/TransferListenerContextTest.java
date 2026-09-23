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

package software.amazon.awssdk.transfer.s3.internal.progress;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.transfer.s3.model.PresignedDownloadFileRequest;

class TransferListenerContextTest {

    @Test
    void toString_redactsPresignedUrlQueryString() {
        URL url = signedUrl();

        String result = presignedContext(url).toString();

        assertNoSignatureMaterial(result, url);
    }

    @Test
    void failedContext_toString_redactsRequest_withUnrelatedException() {
        URL url = signedUrl();

        TransferListenerFailedContext failedContext =
            TransferListenerFailedContext.builder()
                                         .transferContext(presignedContext(url))
                                         .exception(new RuntimeException("boom"))
                                         .build();

        assertNoSignatureMaterial(failedContext.toString(), url);
    }

    @Test
    void failedContext_toString_redactsRequest_withMarshallingShapedException() throws Exception {
        URL url = signedUrl();
        URL malformedUrl = new URL("https", "bucket.s3.us-east-1.amazonaws.com", -1,
                                   "/my key.txt?X-Amz-Signature=deadbeefdeadbeef"
                                   + "&X-Amz-Security-Token=SESSIONTOKENEXAMPLE");

        // Shaped like the exception the marshaller builds: a redacted message over a URISyntaxException embedding the URL.
        SdkClientException marshallingException =
            SdkClientException.builder()
                              .message("Unable to marshall pre-signed URL Request for "
                                       + "https://bucket.s3.us-east-1.amazonaws.com/my key.txt"
                                       + "?*** Sensitive Data Redacted ***: Illegal character in path at index 44")
                              .cause(uriSyntaxExceptionFor(malformedUrl))
                              .build();

        TransferListenerFailedContext failedContext =
            TransferListenerFailedContext.builder()
                                         .transferContext(presignedContext(url))
                                         .exception(new CompletionException(marshallingException))
                                         .build();

        assertNoSignatureMaterial(failedContext.toString(), url);
        assertThat(failedContext.toString()).doesNotContain(malformedUrl.getQuery());
    }

    private void assertNoSignatureMaterial(String result, URL url) {
        assertThat(result)
            .doesNotContain("X-Amz-Signature")
            .doesNotContain("deadbeef")
            .doesNotContain("X-Amz-Credential")
            .doesNotContain("AKIAEXAMPLE")
            .doesNotContain("X-Amz-Security-Token")
            .doesNotContain("SESSIONTOKENEXAMPLE")
            .doesNotContain(url.getQuery())
            .contains("*** Sensitive Data Redacted ***");
    }

    private TransferListenerContext presignedContext(URL url) {
        PresignedDownloadFileRequest request =
            PresignedDownloadFileRequest.builder()
                                        .destination(Paths.get("destination-file.txt"))
                                        .presignedUrlDownloadRequest(b -> b.presignedUrl(url))
                                        .build();

        return TransferListenerContext.builder()
                                      .request(request)
                                      .progressSnapshot(DefaultTransferProgressSnapshot.builder()
                                                                                       .transferredBytes(0L)
                                                                                       .build())
                                      .build();
    }

    private URISyntaxException uriSyntaxExceptionFor(URL url) {
        try {
            url.toURI();
        } catch (URISyntaxException e) {
            return e;
        }
        throw new AssertionError("Expected " + url + " to be unconvertible to a URI");
    }

    private URL signedUrl() {
        try {
            return new URL("https://bucket.s3.us-east-1.amazonaws.com/dir/key.txt?"
                           + "X-Amz-Algorithm=AWS4-HMAC-SHA256&"
                           + "X-Amz-Credential=AKIAEXAMPLE%2F20240101%2Fus-east-1%2Fs3%2Faws4_request&"
                           + "X-Amz-Security-Token=SESSIONTOKENEXAMPLE&"
                           + "X-Amz-Signature=deadbeefdeadbeef");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
