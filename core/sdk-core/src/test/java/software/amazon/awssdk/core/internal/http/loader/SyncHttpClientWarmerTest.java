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

package software.amazon.awssdk.core.internal.http.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.internal.crac.WarmUpRequest;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkHttpService;

/**
 * Unit tests for {@link SyncHttpClientWarmer}. Every test drives the real {@link SyncHttpClientWarmer#warmAll()} with an
 * injected fake {@link SdkServiceLoader} (supplying stub {@link SdkHttpService}s) and a fixed endpoint supplier (standing in
 * for the resolved STS host).
 */
class SyncHttpClientWarmerTest {

    private static final URI ENDPOINT = URI.create("https://sts.us-east-1.amazonaws.com/");

    // ---- per-client recipe (driven through warmAll) ----

    @Test
    void warmAll_drainsAndClosesResponseBody() throws IOException {
        InputStream body = spy(new ByteArrayInputStream("<Error>denied</Error>".getBytes()));
        SdkHttpClient client = stubClient(respondingWith(403, body));

        warmer(serviceFor(client)).warmAll();

        verify(body, atLeastOnce()).read();  // drained to EOF
        verify(body).close();
        verify(client).close();
    }

    @Test
    void warmAll_issuesGetToResolvedEndpoint() {
        SdkHttpClient client = stubClient(respondingWith(403, emptyBody()));
        ArgumentCaptor<HttpExecuteRequest> request = ArgumentCaptor.forClass(HttpExecuteRequest.class);

        warmer(serviceFor(client)).warmAll();

        verify(client).prepareRequest(request.capture());
        assertThat(request.getValue().httpRequest().method()).isEqualTo(SdkHttpMethod.GET);
        assertThat(request.getValue().httpRequest().getUri()).isEqualTo(ENDPOINT);
    }

    @Test
    void warmAll_whenRequestFails_swallowsAndStillClosesClient() throws IOException {
        SdkHttpClient client = mock(SdkHttpClient.class);
        ExecutableHttpRequest request = mock(ExecutableHttpRequest.class);
        when(client.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(request);
        when(request.call()).thenThrow(new IOException("offline"));

        assertThatCode(() -> warmer(serviceFor(client)).warmAll()).doesNotThrowAnyException();
        verify(client).close();
    }

    @Test
    void warmAll_whenNoResponseBody_stillClosesClient() {
        SdkHttpClient client = stubClient(respondingWith(403, null));

        assertThatCode(() -> warmer(serviceFor(client)).warmAll()).doesNotThrowAnyException();
        verify(client).close();
    }

    // ---- discovery loop ----

    @Test
    void warmAll_warmsEveryDiscoveredService() {
        SdkHttpClient first = stubClient(respondingWith(403, emptyBody()));
        SdkHttpClient second = stubClient(respondingWith(403, emptyBody()));

        warmer(serviceFor(first), serviceFor(second)).warmAll();

        verify(first).prepareRequest(any(HttpExecuteRequest.class));
        verify(second).prepareRequest(any(HttpExecuteRequest.class));
    }

    @Test
    void warmAll_whenOneServiceFailsToBuild_stillWarmsOthers() {
        SdkHttpService failing = mock(SdkHttpService.class);
        when(failing.createHttpClientBuilder()).thenThrow(new RuntimeException("bad service"));
        SdkHttpClient healthy = stubClient(respondingWith(403, emptyBody()));

        warmer(failing, serviceFor(healthy)).warmAll();

        verify(healthy).prepareRequest(any(HttpExecuteRequest.class));
    }

    @Test
    void warmAll_whenNoServices_isNoOp() {
        assertThatCode(() -> warmer(Collections.emptyIterator()).warmAll()).doesNotThrowAnyException();
    }

    // ---- helpers ----

    private static SyncHttpClientWarmer warmer(SdkHttpService... services) {
        return warmer(Arrays.asList(services).iterator());
    }

    private static SyncHttpClientWarmer warmer(Iterator<SdkHttpService> services) {
        SdkServiceLoader loader = new SdkServiceLoader() {
            @Override
            @SuppressWarnings("unchecked")
            <T> Iterator<T> loadServices(Class<T> clazz) {
                return (Iterator<T>) services;
            }
        };
        return new SyncHttpClientWarmer(loader, () -> ENDPOINT, WarmUpRequest.get());
    }

    /** A service whose builder yields the given client. */
    private static SdkHttpService serviceFor(SdkHttpClient client) {
        SdkHttpClient.Builder<?> builder = mock(SdkHttpClient.Builder.class);
        when(builder.buildWithDefaults(any())).thenReturn(client);

        SdkHttpService service = mock(SdkHttpService.class);
        when(service.createHttpClientBuilder()).thenReturn(builder);
        return service;
    }

    /** A client whose single request returns the given response. */
    private static SdkHttpClient stubClient(HttpExecuteResponse response) {
        try {
            ExecutableHttpRequest request = mock(ExecutableHttpRequest.class);
            when(request.call()).thenReturn(response);

            SdkHttpClient client = mock(SdkHttpClient.class);
            when(client.prepareRequest(any(HttpExecuteRequest.class))).thenReturn(request);
            return client;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpExecuteResponse respondingWith(int statusCode, InputStream body) {
        return HttpExecuteResponse.builder()
            .response(SdkHttpResponse.builder().statusCode(statusCode).build())
            .responseBody(body == null ? null : AbortableInputStream.create(body))
            .build();
    }

    private static InputStream emptyBody() {
        return new ByteArrayInputStream(new byte[0]);
    }
}
