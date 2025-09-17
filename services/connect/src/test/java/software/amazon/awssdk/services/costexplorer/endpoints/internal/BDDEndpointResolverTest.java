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

package software.amazon.awssdk.services.costexplorer.endpoints.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.rules.testing.BaseEndpointProviderTest;
import software.amazon.awssdk.core.rules.testing.EndpointProviderTestCase;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointParams;
import software.amazon.awssdk.services.connect.endpoints.ConnectEndpointProvider;
import software.amazon.awssdk.services.connect.endpoints.internal.BDDEndpointResolver;

public class BDDEndpointResolverTest extends BaseEndpointProviderTest {
    private static final ConnectEndpointProvider PROVIDER = new BDDEndpointResolver();

    @MethodSource("testCases")
    @ParameterizedTest
    public void resolvesCorrectEndpoint(EndpointProviderTestCase tc) {
        verify(tc);
    }

    private static List<EndpointProviderTestCase> testCases() {
        List<EndpointProviderTestCase> testCases = new ArrayList<>();
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("af-south-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.af-south-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("ap-northeast-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.ap-northeast-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("ap-northeast-2"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.ap-northeast-2.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("ap-southeast-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.ap-southeast-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("ap-southeast-2"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.ap-southeast-2.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("ca-central-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.ca-central-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("eu-central-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.eu-central-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("eu-west-2"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.eu-west-2.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-east-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-west-2"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-west-2.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            builder.useFips(true);
            builder.useDualStack(true);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect-fips.us-east-1.api.aws")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            builder.useFips(true);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect-fips.us-east-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            builder.useFips(false);
            builder.useDualStack(true);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-east-1.api.aws")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("cn-north-1"));
            builder.useFips(true);
            builder.useDualStack(true);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect
                                                       .builder()
                                                       .endpoint(
                                                           Endpoint.builder().url(URI.create("https://connect-fips.cn-north-1.api.amazonwebservices.com.cn"))
                                                                   .build()).build()));
        testCases
            .add(new EndpointProviderTestCase(() -> {
                ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
                builder.region(Region.of("cn-north-1"));
                builder.useFips(true);
                builder.useDualStack(false);
                return PROVIDER.resolveEndpoint(builder.build()).join();
            }, Expect.builder()
                     .endpoint(Endpoint.builder().url(URI.create("https://connect-fips.cn-north-1.amazonaws.com.cn")).build())
                     .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("cn-north-1"));
            builder.useFips(false);
            builder.useDualStack(true);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder()
                 .endpoint(Endpoint.builder().url(URI.create("https://connect.cn-north-1.api.amazonwebservices.com.cn")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("cn-north-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.cn-north-1.amazonaws.com.cn")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-gov-west-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-gov-west-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-gov-west-1"));
            builder.useFips(true);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-gov-west-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-gov-east-1"));
            builder.useFips(true);
            builder.useDualStack(true);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect-fips.us-gov-east-1.api.aws")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-gov-east-1"));
            builder.useFips(true);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-gov-east-1.amazonaws.com")).build())
                 .build()));
        testCases
            .add(new EndpointProviderTestCase(() -> {
                ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
                builder.region(Region.of("us-gov-east-1"));
                builder.useFips(false);
                builder.useDualStack(true);
                return PROVIDER.resolveEndpoint(builder.build()).join();
            }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-gov-east-1.api.aws")).build())
                     .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-gov-east-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-gov-east-1.amazonaws.com")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-iso-east-1"));
            builder.useFips(true);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect-fips.us-iso-east-1.c2s.ic.gov")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-iso-east-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-iso-east-1.c2s.ic.gov")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-isob-east-1"));
            builder.useFips(true);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder()
                 .endpoint(Endpoint.builder().url(URI.create("https://connect-fips.us-isob-east-1.sc2s.sgov.gov")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-isob-east-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://connect.us-isob-east-1.sc2s.sgov.gov")).build())
                 .build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            builder.useFips(false);
            builder.useDualStack(false);
            builder.endpoint("https://example.com");
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://example.com")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.useFips(false);
            builder.useDualStack(false);
            builder.endpoint("https://example.com");
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().endpoint(Endpoint.builder().url(URI.create("https://example.com")).build()).build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            builder.useFips(true);
            builder.useDualStack(false);
            builder.endpoint("https://example.com");
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("Invalid Configuration: FIPS and custom endpoint are not supported").build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            builder.region(Region.of("us-east-1"));
            builder.useFips(false);
            builder.useDualStack(true);
            builder.endpoint("https://example.com");
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("Invalid Configuration: Dualstack and custom endpoint are not supported").build()));
        testCases.add(new EndpointProviderTestCase(() -> {
            ConnectEndpointParams.Builder builder = ConnectEndpointParams.builder();
            return PROVIDER.resolveEndpoint(builder.build()).join();
        }, Expect.builder().error("Invalid Configuration: Missing Region").build()));
        return testCases;
    }
}