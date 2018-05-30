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

package software.amazon.awssdk.awscore.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.awscore.client.utils.HttpTestUtils;
import software.amazon.awssdk.awscore.client.utils.WireMockTestBase;
import software.amazon.awssdk.awscore.http.response.DefaultErrorResponseHandler;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.testutils.LogCaptor;

public class DefaultErrorResponseHandlerTest extends WireMockTestBase {

    private static final String RESOURCE = "/some-path";
    private static LogCaptor logCaptor;
    private final AmazonSyncHttpClient client = HttpTestUtils.testAmazonHttpClient();
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
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(RESOURCE)).willReturn(WireMock.aResponse().withStatus(418)));

        executeRequest();

        assertThat(debugEvents()).anySatisfy(hasMessageContaining("Invocation Id"));
    }

    @Test
    public void invalidXmlLogsXmlContentToInfo() throws Exception {
        String content = RandomStringUtils.randomAlphanumeric(10);
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(RESOURCE)).willReturn(WireMock.aResponse().withStatus(418).withBody(content)));

        executeRequest();

        assertThat(debugEvents()).anySatisfy(hasMessageContaining(content));
    }

    @Test
    public void requestIdIsLoggedWithInfoIfInTheHeader() throws Exception {
        String requestId = RandomStringUtils.randomAlphanumeric(10);

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(RESOURCE)).willReturn(WireMock.aResponse().withStatus(418)
                                                                                            .withHeader(X_AMZN_REQUEST_ID_HEADER, requestId)));

        executeRequest();

        assertThat(debugEvents()).anySatisfy(hasMessageContaining(requestId));
    }

    private void executeRequest() {
        expectException(() -> {
            Request<?> request = newGetRequest(RESOURCE);
            client.requestExecutionBuilder()
                  .errorResponseHandler(sut)
                  .originalRequest(NoopTestAwsRequest.builder().build())
                  .executionContext(executionContext(SdkHttpFullRequestAdapter.toHttpFullRequest(request)))
                  .request(request)
                  .execute();
        });
    }

    @SuppressWarnings("EmptyCatchBlock")
    private void expectException(Runnable r) {
        try {
            r.run();
            throw new RuntimeException("Expected exception, got none");
        } catch (Exception expected) {
        }
    }

    private List<LoggingEvent> debugEvents() {
        List<LoggingEvent> info = new ArrayList<>();
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

    private static ExecutionContext executionContext(SdkHttpFullRequest request) {
        InterceptorContext incerceptorContext =
            InterceptorContext.builder()
                              .request(NoopTestAwsRequest.builder().build())
                              .httpRequest(request)
                              .build();
        return ExecutionContext.builder()
                               .signer(new NoOpSigner())
                               .interceptorChain(new ExecutionInterceptorChain(Collections.emptyList()))
                               .executionAttributes(new ExecutionAttributes())
                               .interceptorContext(incerceptorContext)
                               .build();
    }
}
