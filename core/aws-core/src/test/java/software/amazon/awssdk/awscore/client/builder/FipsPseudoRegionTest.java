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

package software.amazon.awssdk.awscore.client.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;

public class FipsPseudoRegionTest {
    private static final AttributeMap MOCK_DEFAULTS = AttributeMap
        .builder()
        .put(SdkHttpConfigurationOption.READ_TIMEOUT, Duration.ofSeconds(10))
        .build();

    private static final String ENDPOINT_PREFIX = "s3";
    private static final String SIGNING_NAME = "demo";
    private static final String SERVICE_NAME = "Demo";

    private SdkHttpClient.Builder defaultHttpClientBuilder;


    @BeforeEach
    public void setup() {
        defaultHttpClientBuilder = mock(SdkHttpClient.Builder.class);
        when(defaultHttpClientBuilder.buildWithDefaults(any())).thenReturn(mock(SdkHttpClient.class));
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void verifyRegion(TestCase tc) {
        TestClient client = testClientBuilder().region(tc.inputRegion).build();

        assertThat(client.clientConfiguration.option(AwsClientOption.AWS_REGION)).isEqualTo(tc.expectedClientRegion);
        assertThat(client.clientConfiguration.option(AwsClientOption.FIPS_ENDPOINT_ENABLED)).isTrue();
    }

    public static List<TestCase> testCases() {
        List<TestCase> testCases = new ArrayList<>();

        testCases.add(new TestCase(Region.of("fips-us-west-2"), Region.of("us-west-2")));
        testCases.add(new TestCase(Region.of("us-west-2-fips"), Region.of("us-west-2")));
        testCases.add(new TestCase(Region.of("rekognition-fips.us-west-2"), Region.of("rekognition.us-west-2")));
        testCases.add(new TestCase(Region.of("rekognition.fips-us-west-2"), Region.of("rekognition.us-west-2")));
        testCases.add(new TestCase(Region.of("query-fips-us-west-2"), Region.of("query-us-west-2")));
        testCases.add(new TestCase(Region.of("fips-fips-us-west-2"), Region.of("us-west-2")));
        testCases.add(new TestCase(Region.of("fips-us-west-2-fips"), Region.of("us-west-2")));


        return testCases;
    }

    private AwsClientBuilder<TestClientBuilder, TestClient> testClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder()
                                       .putAdvancedOption(SIGNER, mock(Signer.class))
                                       .putAdvancedOption(ENABLE_DEFAULT_REGION_DETECTION, false)
                                       .build();

        return new TestClientBuilder().credentialsProvider(AnonymousCredentialsProvider.create())
                                                                  .overrideConfiguration(overrideConfig);
    }

    private static class TestCase {
        private Region inputRegion;
        private Region expectedClientRegion;

        public TestCase(Region inputRegion, Region expectedClientRegion) {
            this.inputRegion = inputRegion;
            this.expectedClientRegion = expectedClientRegion;
        }
    }

    private static class TestClient {
        private final SdkClientConfiguration clientConfiguration;

        public TestClient(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
        }
    }

    private class TestClientBuilder extends AwsDefaultClientBuilder<TestClientBuilder, TestClient>
        implements AwsClientBuilder<TestClientBuilder, TestClient> {

        public TestClientBuilder() {
            super(defaultHttpClientBuilder, null, null);
        }

        @Override
        protected TestClient buildClient() {
            return new TestClient(super.syncClientConfiguration());
        }

        @Override
        protected String serviceEndpointPrefix() {
            return ENDPOINT_PREFIX;
        }

        @Override
        protected String signingName() {
            return SIGNING_NAME;
        }

        @Override
        protected String serviceName() {
            return SERVICE_NAME;
        }

        @Override
        protected AttributeMap serviceHttpConfig() {
            return MOCK_DEFAULTS;
        }
    }
}
