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

package software.amazon.awssdk.core.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.executionContext;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.util.LogCaptor;
import utils.HttpTestUtils;
import utils.http.WireMockTestBase;

public class DefaultErrorResponseHandlerTest extends WireMockTestBase {

    private static final String RESOURCE = "/some-path";
    private final AmazonHttpClient client = HttpTestUtils.testAmazonHttpClient();
    private final DefaultErrorResponseHandler sut = new DefaultErrorResponseHandler(new ArrayList<>());
    private LogCaptor logCaptor = new LogCaptor.DefaultLogCaptor(Level.INFO);

    @Before
    public void setUp() {
        logCaptor.clear();
    }

    @Test
    public void invocationIdIsCapturedInTheLog() throws Exception {
        stubFor(get(urlPathEqualTo(RESOURCE)).willReturn(aResponse().withStatus(418)));

        executeRequest();

        assertThat(infoEvents(), hasEventWithContent("Invocation Id"));
    }

    @Test
    public void invalidXmlLogsXmlContentToInfo() throws Exception {
        String content = RandomStringUtils.randomAlphanumeric(10);
        stubFor(get(urlPathEqualTo(RESOURCE)).willReturn(aResponse().withStatus(418).withBody(content)));

        executeRequest();

        assertThat(infoEvents(), hasEventWithContent(content));
    }

    @Test
    public void requestIdIsLoggedWithInfoIfInTheHeader() throws Exception {
        String requestId = RandomStringUtils.randomAlphanumeric(10);

        stubFor(get(urlPathEqualTo(RESOURCE)).willReturn(aResponse().withStatus(418)
                                                                    .withHeader(X_AMZN_REQUEST_ID_HEADER, requestId)));

        executeRequest();

        assertThat(infoEvents(), hasEventWithContent(requestId));
    }

    private void executeRequest() {
        expectException(new Runnable() {
            @Override
            public void run() {
                Request<?> request = newGetRequest(RESOURCE);
                client.requestExecutionBuilder()
                      .errorResponseHandler(sut)
                      .executionContext(executionContext(SdkHttpFullRequestAdapter.toHttpFullRequest(request)))
                      .request(request)
                      .execute();
            }
        });
    }

    @SuppressWarnings("EmptyCatchBlock")
    private void expectException(Runnable r) {
        try {
            r.run();
            throw new RuntimeException("Expected exception, got none");
        } catch (Exception e) {
            // Ignored or expected.
        }
    }

    private List<LoggingEvent> infoEvents() {
        List<LoggingEvent> info = new ArrayList<LoggingEvent>();
        List<LoggingEvent> loggingEvents = logCaptor.loggedEvents();
        for (LoggingEvent le : loggingEvents) {
            if (le.getLevel().equals(Level.INFO)) {
                info.add(le);
            }
        }
        return info;
    }

    private org.hamcrest.Matcher<Iterable<? extends LoggingEvent>> hasEventWithContent(String content) {
        return contains(hasProperty("message", containsString(content)));
    }
}
