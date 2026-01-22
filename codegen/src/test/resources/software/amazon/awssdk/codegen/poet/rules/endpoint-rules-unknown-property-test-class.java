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

package software.amazon.awssdk.services.query.endpoints;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.rules.testing.BaseEndpointProviderTest;
import software.amazon.awssdk.core.rules.testing.EndpointProviderTestCase;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;

@Generated("software.amazon.awssdk:codegen")
public class QueryEndpointProviderTests extends BaseEndpointProviderTest {
    private static final QueryEndpointProvider PROVIDER = QueryEndpointProvider.defaultProvider();

    @MethodSource("testCases")
    @ParameterizedTest
    public void resolvesCorrectEndpoint(EndpointProviderTestCase tc) {
        verify(tc);
    }

    private static List<EndpointProviderTestCase> testCases() {
        List<EndpointProviderTestCase> testCases = new ArrayList<>();
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://012345678901.myservice.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("Should have been skipped!").build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("Missing info").build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            QueryEndpointParams.Builder builder = QueryEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("Missing info").build()));
        return testCases;
    }
}
