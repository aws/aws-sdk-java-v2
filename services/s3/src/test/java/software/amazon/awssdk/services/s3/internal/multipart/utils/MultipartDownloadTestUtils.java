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

package software.amazon.awssdk.services.s3.internal.multipart.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import software.amazon.awssdk.services.s3.utils.AsyncResponseTransformerTestSupplier;

public class MultipartDownloadTestUtils {

    private final String testBucket;
    private final String testKey;
    private final String eTag;
    private final Random random = new Random();

    public MultipartDownloadTestUtils(String testBucket, String testKey, String eTag) {
        this.testBucket = testBucket;
        this.testKey = testKey;
        this.eTag = eTag;
    }

    public static List<AsyncResponseTransformerTestSupplier<?>> transformersSuppliers() {
        return Arrays.asList(
            new AsyncResponseTransformerTestSupplier.ByteTestArtSupplier(),
            new AsyncResponseTransformerTestSupplier.InputStreamArtSupplier(),
            new AsyncResponseTransformerTestSupplier.PublisherArtSupplier(),
            new AsyncResponseTransformerTestSupplier.FileArtSupplier()
        );
    }

    public byte[] stubAllParts(String testBucket, String testKey, int amountOfPartToTest, int partSize) {
        byte[] expectedBody = new byte[amountOfPartToTest * partSize];
        for (int i = 0; i < amountOfPartToTest; i++) {
            byte[] individualBody = stubForPart(testBucket, testKey, i + 1, amountOfPartToTest, partSize);
            System.arraycopy(individualBody, 0, expectedBody, i * partSize, individualBody.length);
        }
        return expectedBody;
    }

    public byte[] stubForPart(String testBucket, String testKey,int part, int totalPart, int partSize) {
        byte[] body = new byte[partSize];
        random.nextBytes(body);
        stubFor(get(urlEqualTo(String.format("/%s/%s?partNumber=%d", testBucket, testKey, part))).willReturn(
            aResponse()
                .withHeader("x-amz-mp-parts-count", totalPart + "")
                .withHeader("ETag", eTag)
                .withBody(body)));
        return body;
    }

    public void verifyCorrectAmountOfRequestsMade(int amountOfPartToTest) {
        String urlTemplate = ".*partNumber=%d.*";
        for (int i = 1; i <= amountOfPartToTest; i++) {
            verify(getRequestedFor(urlMatching(String.format(urlTemplate, i))));
        }
        verify(0, getRequestedFor(urlMatching(String.format(urlTemplate, amountOfPartToTest + 1))));
    }

    public static String errorBody(String errorCode, String errorMessage) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
               + "<Error>\n"
               + "  <Code>" + errorCode + "</Code>\n"
               + "  <Message>" + errorMessage + "</Message>\n"
               + "</Error>";
    }

    public static String internalErrorBody() {
        return errorBody("InternalError", "We encountered an internal error. Please try again.");
    }

    public static String slowdownErrorBody() {
        return errorBody("SlowDown", "Please reduce your request rate.");
    }
}
