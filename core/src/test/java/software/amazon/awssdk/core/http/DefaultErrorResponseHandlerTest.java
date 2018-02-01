/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;
import static software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils.executionContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.util.LogCaptor;
import utils.HttpTestUtils;
import utils.http.WireMockTestBase;

public class DefaultErrorResponseHandlerTest extends WireMockTestBase {

    private static final String RESOURCE = "/some-path";
    private static LogCaptor logCaptor;
    private final AmazonHttpClient client = HttpTestUtils.testAmazonHttpClient();
    private final DefaultErrorResponseHandler sut = new DefaultErrorResponseHandler(new ArrayList<>());

    @BeforeClass
    public static void setup() {
        logCaptor = new LogCaptor.DefaultLogCaptor(Level.DEBUG);
    }

    @AfterClass
    public static void teardown() throws Exception {
        logCaptor.close();
    }

    @Before
    public void methodSetup() {
        logCaptor.clear();
    }

    @Test
    public void invocationIdIsCapturedInTheLog() throws Exception {
        stubFor(get(urlPathEqualTo(RESOURCE)).willReturn(aResponse().withStatus(418)));

        executeRequest();

        assertThat(debugEvents()).anySatisfy(hasMessageContaining("Invocation Id"));
    }

    @Test
    public void invalidXmlLogsXmlContentToInfo() throws Exception {
        String content = RandomStringUtils.randomAlphanumeric(10);
        stubFor(get(urlPathEqualTo(RESOURCE)).willReturn(aResponse().withStatus(418).withBody(content)));

        executeRequest();

        assertThat(debugEvents()).anySatisfy(hasMessageContaining(content));
    }

    @Test
    public void requestIdIsLoggedWithInfoIfInTheHeader() throws Exception {
        String requestId = RandomStringUtils.randomAlphanumeric(10);

        stubFor(get(urlPathEqualTo(RESOURCE)).willReturn(aResponse().withStatus(418)
                                                                    .withHeader(X_AMZN_REQUEST_ID_HEADER, requestId)));

        executeRequest();

        assertThat(debugEvents()).anySatisfy(hasMessageContaining(requestId));
    }

    private void executeRequest() {
        expectException(new Runnable() {
            @Override
            public void run() {
                Request<?> request = newGetRequest(RESOURCE);
                client.requestExecutionBuilder()
                      .errorResponseHandler(sut)
                      .originalRequest(NoopTestAwsRequest.builder().build())
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
            System.out.println("exept");
            // Ignored or expected.
        }
    }

    private List<LoggingEvent> debugEvents() {
        List<LoggingEvent> info = new ArrayList<LoggingEvent>();
        List<LoggingEvent> loggingEvents = logCaptor.loggedEvents();
        for (LoggingEvent le : loggingEvents) {
            if (le.getLevel().equals(Level.DEBUG)) {
                info.add(le);
            }
        }
        return info;
    }

    private Consumer<LoggingEvent> hasMessageContaining(String str) {
        return evt -> assertThat(evt.getMessage()).asString().contains(str);
    }
}
