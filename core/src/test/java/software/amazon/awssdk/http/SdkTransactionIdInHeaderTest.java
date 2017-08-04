/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.http.AmazonHttpClient.HEADER_SDK_TRANSACTION_ID;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import utils.HttpTestUtils;
import utils.http.WireMockTestBase;

public class SdkTransactionIdInHeaderTest extends WireMockTestBase {

    private static final String RESOURCE_PATH = "/transaction-id/";

    @Test
    public void retriedRequest_HasSameTransactionIdForAllRetries() throws Exception {
        stubFor(get(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse().withStatus(500)));
        executeRequest();
        assertTransactionIdIsUnchangedAcrossRetries();
    }

    private void assertTransactionIdIsUnchangedAcrossRetries() {
        String previousTransactionId = null;
        for (LoggedRequest request : findAll(getRequestedFor(urlEqualTo(RESOURCE_PATH)))) {
            final String currentTransactionId = request.getHeader(HEADER_SDK_TRANSACTION_ID);
            // Transaction ID should always be set
            assertNotNull(currentTransactionId);
            // Transaction ID should be the same across retries
            if (previousTransactionId != null) {
                assertEquals(previousTransactionId, currentTransactionId);
            }
            previousTransactionId = currentTransactionId;
        }
    }

    private void executeRequest() throws Exception {
        AmazonHttpClient httpClient = HttpTestUtils.testAmazonHttpClient();
        try {
            SdkHttpFullRequest request = SdkHttpFullRequestAdapter.toHttpFullRequest(newGetRequest(RESOURCE_PATH));
            httpClient.requestExecutionBuilder()
                      .request(request)
                      .errorResponseHandler(stubErrorHandler())
                      .executionContext(ClientExecutionAndRequestTimerTestUtils.executionContext(request))
                      .execute();
            fail("Expected exception");
        } catch (AmazonServiceException expected) {
            // Ignored or expected.
        }
    }

}
