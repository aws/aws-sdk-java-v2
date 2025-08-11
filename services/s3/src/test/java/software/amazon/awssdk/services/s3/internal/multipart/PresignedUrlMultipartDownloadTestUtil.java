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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.util.Random;

/**
 * Test utility class for PresignedUrlMultipartDownloaderSubscriber WireMock tests.
 * Provides methods to stub HTTP range requests and verify interactions.
 */
public class PresignedUrlMultipartDownloadTestUtil {

    private static final String PRESIGNED_URL_PATH = "/presigned-url";
    private static final String DIFFERENT_ETAG = "different-etag-12345";

    private final String presignedUrl;
    private final String eTag;
    private final Random random = new Random();

    public PresignedUrlMultipartDownloadTestUtil(String presignedUrl, String eTag) {
        this.presignedUrl = presignedUrl;
        this.eTag = eTag;
    }

    public String getPresignedUrl() {
        return presignedUrl;
    }

    public byte[] stubAllRangeParts(int amountOfPartsToTest, int partSize) {
        int actualPartSize = partSize;
        byte[] expectedBody = new byte[amountOfPartsToTest * actualPartSize];
        random.nextBytes(expectedBody);
        
        long totalSize = expectedBody.length;
        for (int i = 0; i < amountOfPartsToTest; i++) {
            long startByte = i * actualPartSize;
            long endByte = Math.min(startByte + actualPartSize - 1, totalSize - 1);
            
            byte[] partBody = new byte[(int)(endByte - startByte + 1)];
            System.arraycopy(expectedBody, (int)startByte, partBody, 0, partBody.length);
            
            String rangeHeader = "bytes=" + startByte + "-" + endByte;
            String contentRange = "bytes " + startByte + "-" + endByte + "/" + totalSize;
            
            stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
                .withHeader("Range", new EqualToPattern(rangeHeader))
                .willReturn(aResponse()
                    .withStatus(206)
                    .withHeader("Content-Range", contentRange)
                    .withHeader("Content-Length", String.valueOf(partBody.length))
                    .withHeader("ETag", eTag)
                    .withBody(partBody)));
        }
        
        return expectedBody;
    }

    public byte[] stubSingleRangePart(int partSize) {
        // For single part, use the actual configured size
        byte[] body = new byte[partSize];
        random.nextBytes(body);
        
        String rangeHeader = "bytes=0-" + (partSize - 1);
        String contentRange = "bytes 0-" + (partSize - 1) + "/" + partSize;
        
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", new EqualToPattern(rangeHeader))
            .willReturn(aResponse()
                .withStatus(206)
                .withHeader("Content-Range", contentRange)
                .withHeader("Content-Length", String.valueOf(partSize))
                .withHeader("ETag", eTag)
                .withBody(body)));
        
        return body;
    }

    public void stubFirstRangePartForSizeDiscovery(int totalParts, int partSize) {
        int actualPartSize = partSize;
        byte[] body = new byte[actualPartSize];
        random.nextBytes(body);
        
        long totalSize = totalParts * actualPartSize;
        String rangeHeader = "bytes=0-" + (actualPartSize - 1);
        String contentRange = "bytes 0-" + (actualPartSize - 1) + "/" + totalSize;
        
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", new EqualToPattern(rangeHeader))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(206)
                .withHeader("Content-Range", contentRange)
                .withHeader("Content-Length", String.valueOf(actualPartSize))
                .withHeader("ETag", eTag)
                .withBody(body)));
    }

    public void stubFirstRangeRequestWithError() {
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(400)
                .withBody("<Error><Code>400</Code><Message>test error message</Message></Error>")));
    }

    public void stubSecondRangeRequestWithError(int partSize) {
        long startByte = partSize;
        long endByte = startByte + partSize - 1;
        String rangeHeader = "bytes=" + startByte + "-" + endByte;
        
        stubFor(get(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", new EqualToPattern(rangeHeader))
            .atPriority(2)
            .willReturn(aResponse()
                .withStatus(400)
                .withBody("<e><Code>400</Code><Message>test error message</Message></e>")));
    }

    public void verifyCorrectAmountOfRangeRequestsMade(int expectedRequestCount) {
        verify(expectedRequestCount, getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH)));
    }

    public void verifyNoRequestMadeForRange(long startByte, long endByte) {
        String rangeHeader = "bytes=" + startByte + "-" + endByte;
        verify(0, getRequestedFor(urlEqualTo(PRESIGNED_URL_PATH))
            .withHeader("Range", new EqualToPattern(rangeHeader)));
    }
}
