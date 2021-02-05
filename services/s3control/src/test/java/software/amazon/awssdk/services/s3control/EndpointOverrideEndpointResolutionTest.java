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

package software.amazon.awssdk.services.s3control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3control.model.GetAccessPointRequest;
import software.amazon.awssdk.services.s3control.model.GetBucketRequest;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

@RunWith(Parameterized.class)
public class EndpointOverrideEndpointResolutionTest {
    private final TestCase testCase;
    private final SignerAndPresigner mockSigner;
    private final MockSyncHttpClient mockHttpClient;
    private final S3ControlClient s3ControlClient;

    public interface SignerAndPresigner extends Signer, Presigner {}

    public EndpointOverrideEndpointResolutionTest(TestCase testCase) {
        this.testCase = testCase;
        this.mockSigner = Mockito.mock(SignerAndPresigner.class);
        this.mockHttpClient = new MockSyncHttpClient();
        this.s3ControlClient =
            S3ControlClient.builder()
                           .region(testCase.clientRegion)
                           .endpointOverride(testCase.endpointUrl)
                           .serviceConfiguration(testCase.s3ControlConfiguration)
                           .httpClient(mockHttpClient)
                           .overrideConfiguration(c -> c.putAdvancedOption(SdkAdvancedClientOption.SIGNER, mockSigner))
                           .build();

        HttpExecuteResponse response =
            HttpExecuteResponse.builder()
                               .response(SdkHttpResponse.builder().statusCode(200).build())
                               .responseBody(AbortableInputStream.create(new StringInputStream("<body/>")))
                               .build();
        mockHttpClient.stubNextResponse(response);

        Mockito.when(mockSigner.sign(any(), any())).thenAnswer(r -> r.getArgumentAt(0, SdkHttpFullRequest.class));
        Mockito.when(mockSigner.presign(any(), any())).thenAnswer(r -> r.getArgumentAt(0, SdkHttpFullRequest.class)
                                                                        .copy(h -> h.putRawQueryParameter("X-Amz-SignedHeaders", "host")));
    }

    @Test
    public void s3ControlClient_endpointSigningRegionAndServiceNamesAreCorrect() {
        try {
            if (testCase.getAccessPointRequest != null) {
                s3ControlClient.getAccessPoint(testCase.getAccessPointRequest);
            } else if (testCase.createBucketRequest != null) {
                s3ControlClient.createBucket(testCase.createBucketRequest);
            } else {
                s3ControlClient.getBucket(testCase.getBucketRequest);
            }

            assertThat(testCase.expectedException).isNull();
            assertThat(mockHttpClient.getLastRequest().getUri()).isEqualTo(testCase.expectedEndpoint);
            assertThat(signingRegion()).isEqualTo(testCase.expectedSigningRegion);
            assertThat(signingServiceName()).isEqualTo(testCase.expectedSigningServiceName);
        } catch (RuntimeException e) {
            if (testCase.expectedException == null) {
                throw e;
            }

            assertThat(e).isInstanceOf(testCase.expectedException);
        }
    }

    private String signingServiceName() {
        return attributesPassedToSigner().getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME);
    }

    private Region signingRegion() {
        return attributesPassedToSigner().getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION);
    }

    private ExecutionAttributes attributesPassedToSigner() {
        ArgumentCaptor<ExecutionAttributes> executionAttributes = ArgumentCaptor.forClass(ExecutionAttributes.class);
        Mockito.verify(mockSigner, Mockito.atLeast(0)).sign(any(), executionAttributes.capture());

        if (executionAttributes.getAllValues().isEmpty()) {
            return attributesPassedToPresigner();
        }

        return executionAttributes.getValue();
    }

    private ExecutionAttributes attributesPassedToPresigner() {
        ArgumentCaptor<ExecutionAttributes> executionAttributes = ArgumentCaptor.forClass(ExecutionAttributes.class);
        Mockito.verify(mockSigner).presign(any(), executionAttributes.capture());
        return executionAttributes.getValue();
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<TestCase> testCases() {
        List<TestCase> cases = new ArrayList<>();

        cases.add(new TestCase().setCaseName("get-access-point by access point name")
                                .setGetAccessPointRequest(r -> r.name("apname").accountId("123456789012"))
                                .setEndpointUrl("https://beta.example.com")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://123456789012.beta.example.com/v20180820/accesspoint/apname")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("get-access-point by access point name with http, path, query and port")
                                .setGetAccessPointRequest(r -> r.name("apname").accountId("123456789012"))
                                .setEndpointUrl("http://beta.example.com:1234/path?foo=bar")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://123456789012.beta.example.com:1234/path/v20180820/accesspoint/apname?foo=bar")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("get-access-point by outpost access point arn")
                                .setGetAccessPointRequest(r -> r.name("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint"))
                                .setEndpointUrl("https://beta.example.com")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://beta.example.com/v20180820/accesspoint/myaccesspoint")
                                .setExpectedSigningServiceName("s3-outposts")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("get-access-point by outpost access point arn with http, path, query and port")
                                .setGetAccessPointRequest(r -> r.name("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint"))
                                .setEndpointUrl("http://beta.example.com:1234/path?foo=bar")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://beta.example.com:1234/path/v20180820/accesspoint/myaccesspoint?foo=bar")
                                .setExpectedSigningServiceName("s3-outposts")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("create-bucket with outpost ID")
                                .setCreateBucketRequest(r -> r.bucket("bucketname").outpostId("op-123"))
                                .setEndpointUrl("https://beta.example.com")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://beta.example.com/v20180820/bucket/bucketname")
                                .setExpectedSigningServiceName("s3-outposts")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("create-bucket with outpost ID, http, path, query and port")
                                .setCreateBucketRequest(r -> r.bucket("bucketname").outpostId("op-123"))
                                .setEndpointUrl("http://beta.example.com:1234/path?foo=bar")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://beta.example.com:1234/path/v20180820/bucket/bucketname?foo=bar")
                                .setExpectedSigningServiceName("s3-outposts")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("get-bucket with outpost bucket arn")
                                .setGetBucketRequest(r -> r.bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket"))
                                .setEndpointUrl("https://beta.example.com")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://beta.example.com/v20180820/bucket/mybucket")
                                .setExpectedSigningServiceName("s3-outposts")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("get-bucket with outpost bucket arn, http, path, query and port")
                                .setGetBucketRequest(r -> r.bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket"))
                                .setEndpointUrl("http://beta.example.com:1234/path?foo=bar")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://beta.example.com:1234/path/v20180820/bucket/mybucket?foo=bar")
                                .setExpectedSigningServiceName("s3-outposts")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("get-access-point by access point name with dualstack")
                                .setGetAccessPointRequest(r -> r.name("apname").accountId("123456789012"))
                                .setEndpointUrl("https://beta.example.com")
                                .setS3ControlConfiguration(c -> c.dualstackEnabled(true))
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("get-access-point by outpost access point arn with dualstack")
                                .setGetAccessPointRequest(r -> r.name("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint"))
                                .setEndpointUrl("https://beta.example.com")
                                .setS3ControlConfiguration(c -> c.dualstackEnabled(true))
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("create-bucket with outpost ID with dualstack")
                                .setCreateBucketRequest(r -> r.bucket("bucketname").outpostId("op-123"))
                                .setEndpointUrl("https://beta.example.com")
                                .setS3ControlConfiguration(c -> c.dualstackEnabled(true))
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("get-bucket with outpost bucket arn with dualstack")
                                .setGetBucketRequest(r -> r.bucket("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:bucket:mybucket"))
                                .setEndpointUrl("https://beta.example.com")
                                .setS3ControlConfiguration(c -> c.dualstackEnabled(true))
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedException(IllegalArgumentException.class));

        return cases;
    }

    private static class TestCase {
        private String caseName;
        private URI endpointUrl;
        private GetAccessPointRequest getAccessPointRequest;
        private CreateBucketRequest createBucketRequest;
        private GetBucketRequest getBucketRequest;
        private S3ControlConfiguration s3ControlConfiguration = S3ControlConfiguration.builder().build();
        private Region clientRegion;
        private URI expectedEndpoint;
        private String expectedSigningServiceName;
        private Region expectedSigningRegion;
        private Class<? extends RuntimeException> expectedException;

        public TestCase setCaseName(String caseName) {
            this.caseName = caseName;
            return this;
        }

        public TestCase setGetAccessPointRequest(Consumer<GetAccessPointRequest.Builder> consumer) {
            GetAccessPointRequest.Builder builder = GetAccessPointRequest.builder();
            consumer.accept(builder);
            this.getAccessPointRequest = builder.build();
            return this;
        }

        public TestCase setCreateBucketRequest(Consumer<CreateBucketRequest.Builder> consumer) {
            CreateBucketRequest.Builder builder = CreateBucketRequest.builder();
            consumer.accept(builder);
            this.createBucketRequest = builder.build();
            return this;
        }

        public TestCase setGetBucketRequest(Consumer<GetBucketRequest.Builder> consumer) {
            GetBucketRequest.Builder builder = GetBucketRequest.builder();
            consumer.accept(builder);
            this.getBucketRequest = builder.build();
            return this;
        }

        public TestCase setEndpointUrl(String endpointUrl) {
            this.endpointUrl = URI.create(endpointUrl);
            return this;
        }

        public TestCase setS3ControlConfiguration(Consumer<S3ControlConfiguration.Builder> s3ControlConfiguration) {
            S3ControlConfiguration.Builder configBuilder = S3ControlConfiguration.builder();
            s3ControlConfiguration.accept(configBuilder);
            this.s3ControlConfiguration = configBuilder.build();
            return this;
        }

        public TestCase setClientRegion(Region clientRegion) {
            this.clientRegion = clientRegion;
            return this;
        }

        public TestCase setExpectedEndpoint(String expectedEndpoint) {
            this.expectedEndpoint = URI.create(expectedEndpoint);
            return this;
        }

        public TestCase setExpectedSigningServiceName(String expectedSigningServiceName) {
            this.expectedSigningServiceName = expectedSigningServiceName;
            return this;
        }

        public TestCase setExpectedSigningRegion(Region expectedSigningRegion) {
            this.expectedSigningRegion = expectedSigningRegion;
            return this;
        }

        public TestCase setExpectedException(Class<? extends RuntimeException> expectedException) {
            this.expectedException = expectedException;
            return this;
        }

        @Override
        public String toString() {
            return this.caseName;
        }
    }
}
