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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
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
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;

@RunWith(Parameterized.class)
public class EndpointOverrideEndpointResolutionTest {
    private final TestCase testCase;
    private final SignerAndPresigner mockSigner;
    private final MockSyncHttpClient mockHttpClient;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final GetObjectRequest getObjectRequest;
    private final S3Utilities s3Utilities;

    public interface SignerAndPresigner extends Signer, Presigner {}

    public EndpointOverrideEndpointResolutionTest(TestCase testCase) throws UnsupportedEncodingException {
        this.testCase = testCase;
        this.mockSigner = Mockito.mock(SignerAndPresigner.class);
        this.mockHttpClient = new MockSyncHttpClient();
        this.s3Client = S3Client.builder()
                                .region(testCase.clientRegion)
                                .dualstackEnabled(testCase.clientDualstackEnabled)
                                .fipsEnabled(testCase.clientFipsEnabled)
                                .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("dummy-key", "dummy-secret")))
                                .endpointOverride(testCase.endpointUrl)
                                .serviceConfiguration(testCase.s3Configuration)
                                .httpClient(mockHttpClient)
                                .overrideConfiguration(c -> c.putAdvancedOption(SdkAdvancedClientOption.SIGNER, mockSigner))
                                .build();
        this.s3Presigner = S3Presigner.builder()
                                      .region(testCase.clientRegion)
                                      .credentialsProvider(StaticCredentialsProvider.create(
                                          AwsBasicCredentials.create("dummy-key", "dummy-secret")))
                                      .endpointOverride(testCase.endpointUrl)
                                      .serviceConfiguration(testCase.s3Configuration)
                                      .dualstackEnabled(testCase.clientDualstackEnabled)
                                      .fipsEnabled(testCase.clientFipsEnabled)
                                      .build();
        this.s3Utilities = S3Utilities.builder()
                                      .region(testCase.clientRegion)
                                      .s3Configuration(testCase.s3Configuration)
                                      .dualstackEnabled(testCase.clientDualstackEnabled)
                                      .fipsEnabled(testCase.clientFipsEnabled)
                                      .build();

        this.getObjectRequest = testCase.getObjectBucketName == null
                                ? null
                                : GetObjectRequest.builder()
                                                  .bucket(testCase.getObjectBucketName)
                                                  .key("object")
                                                  .overrideConfiguration(c -> c.signer(mockSigner))
                                                  .build();

        mockHttpClient.stubNextResponse(S3MockUtils.mockListObjectsResponse());
        Mockito.when(mockSigner.sign(any(), any())).thenAnswer(r -> r.getArgumentAt(0, SdkHttpFullRequest.class));
        Mockito.when(mockSigner.presign(any(), any())).thenAnswer(r -> r.getArgumentAt(0, SdkHttpFullRequest.class)
                                                                        .copy(h -> h.putRawQueryParameter("X-Amz-SignedHeaders", "host")));
    }

    @Test
    public void s3Client_endpointSigningRegionAndServiceNamesAreCorrect() {
        try {
            if (getObjectRequest != null) {
                s3Client.getObject(getObjectRequest);
            } else {
                s3Client.listBuckets();
            }

            assertThat(mockHttpClient.getLastRequest().getUri()).isEqualTo(testCase.expectedEndpoint);
            assertThat(signingRegion()).isEqualTo(testCase.expectedSigningRegion);
            assertThat(signingServiceName()).isEqualTo(testCase.expectedSigningServiceName);
            assertThat(testCase.expectedException).isNull();
        } catch (RuntimeException e) {
            if (testCase.expectedException == null) {
                throw e;
            }

            assertThat(e).isInstanceOf(testCase.expectedException);
        }
    }

    @Test
    public void s3Presigner_endpointSigningRegionAndServiceNamesAreCorrect() {
        try {
            if (getObjectRequest != null) {
                PresignedGetObjectRequest presignedGetObjectRequest =
                    s3Presigner.presignGetObject(r -> r.getObjectRequest(getObjectRequest)
                                                       .signatureDuration(Duration.ofDays(1)));

                URI uriWithoutQueryParameters = presignedGetObjectRequest.httpRequest()
                                                                         .copy(r -> r.removeQueryParameter("X-Amz-SignedHeaders"))
                                                                         .getUri();
                assertThat(uriWithoutQueryParameters).isEqualTo(testCase.expectedEndpoint);
                assertThat(signingRegion()).isEqualTo(testCase.expectedSigningRegion);
                assertThat(signingServiceName()).isEqualTo(testCase.expectedSigningServiceName);
            } else {
                System.out.println("There are (currently) no operations which do not take a bucket. Test will be skipped.");
            }

            assertThat(testCase.expectedException).isNull();
        } catch (RuntimeException e) {
            if (testCase.expectedException == null) {
                throw e;
            }

            assertThat(e).isInstanceOf(testCase.expectedException);
        }
    }

    @Test
    public void s3Utilities_endpointSigningRegionAndServiceNamesAreCorrect() throws URISyntaxException {
        try {
            if (testCase.getObjectBucketName != null) {
                URL url = s3Utilities.getUrl(r -> r.bucket(testCase.getObjectBucketName)
                                                   .key("object")
                                                   .endpoint(testCase.endpointUrl)
                                                   .region(testCase.clientRegion));
                assertThat(url.toURI()).isEqualTo(testCase.expectedEndpoint);
            } else {
                System.out.println("There are (currently) no operations which do not take a bucket. Test will be skipped.");
            }

            assertThat(testCase.expectedException).isNull();
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

        cases.add(new TestCase().setCaseName("normal bucket")
                                .setGetObjectBucketName("bucketname")
                                .setEndpointUrl("https://beta.example.com")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://beta.example.com/bucketname/object")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("normal bucket with http, path, query and port")
                                .setGetObjectBucketName("bucketname")
                                .setEndpointUrl("http://beta.example.com:1234/path?foo=bar")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://beta.example.com:1234/path/bucketname/object?foo=bar")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("access point")
                                .setGetObjectBucketName("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                .setEndpointUrl("https://beta.example.com")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://myendpoint-123456789012.beta.example.com/object")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("access point with http, path, query, and port")
                                .setGetObjectBucketName("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                .setEndpointUrl("http://beta.example.com:1234/path?foo=bar")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://myendpoint-123456789012.beta.example.com:1234/path/object?foo=bar")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("outposts access point")
                                .setGetObjectBucketName("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                .setEndpointUrl("https://beta.example.com")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://myaccesspoint-123456789012.op-01234567890123456.beta.example.com/object")
                                .setExpectedSigningServiceName("s3-outposts")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("outposts access point with http, path, query, and port")
                                .setGetObjectBucketName("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                .setEndpointUrl("http://beta.example.com:1234/path?foo=bar")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://myaccesspoint-123456789012.op-01234567890123456.beta.example.com:1234/path/object?foo=bar")
                                .setExpectedSigningServiceName("s3-outposts")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("list buckets")
                                .setEndpointUrl("https://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com/")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("list buckets with http, path, query and port")
                                .setEndpointUrl("http://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com:1234/path?foo=bar")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com:1234/path/?foo=bar")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("normal bucket with vpce, path style addressing explicitly enabled")
                                .setGetObjectBucketName("bucketname")
                                .setEndpointUrl("https://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com")
                                .setS3Configuration(c -> c.pathStyleAccessEnabled(true))
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com/bucketname/object")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("normal bucket with http, vpce, path, query, port, and path style addressing explicitly enabled")
                                .setGetObjectBucketName("bucketname")
                                .setEndpointUrl("http://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com:1234/path?foo=bar")
                                .setS3Configuration(c -> c.pathStyleAccessEnabled(true))
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com:1234/path/bucketname/object?foo=bar")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("normal bucket with vpce")
                                .setGetObjectBucketName("bucketname")
                                .setEndpointUrl("https://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("https://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com/bucketname/object")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("normal bucket with http, vpce, path, query and port")
                                .setGetObjectBucketName("bucketname")
                                .setEndpointUrl("http://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com:1234/path?foo=bar")
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedEndpoint("http://bucket.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com:1234/path/bucketname/object?foo=bar")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("access point with different arn region and arn region enabled")
                                .setGetObjectBucketName("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                .setEndpointUrl("https://accesspoint.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com")
                                .setS3Configuration(c -> c.useArnRegionEnabled(true))
                                .setClientRegion(Region.EU_WEST_1)
                                .setExpectedEndpoint("https://myendpoint-123456789012.accesspoint.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com/object")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("access point with http, path, query, port, different arn region, and arn region enabled")
                                .setGetObjectBucketName("arn:aws:s3:us-west-2:123456789012:accesspoint:myendpoint")
                                .setEndpointUrl("http://accesspoint.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com:1234/path?foo=bar")
                                .setS3Configuration(c -> c.useArnRegionEnabled(true))
                                .setClientRegion(Region.EU_WEST_1)
                                .setExpectedEndpoint("http://myendpoint-123456789012.accesspoint.vpce-123-abc.s3.us-west-2.vpce.amazonaws.com:1234/path/object?foo=bar")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.US_WEST_2));

        cases.add(new TestCase().setCaseName("outposts access point with dual stack enabled via s3 config")
                                .setGetObjectBucketName("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                .setEndpointUrl("https://beta.example.com")
                                .setS3Configuration(c -> c.dualstackEnabled(true))
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("outposts access point with dual stack enabled via client builder")
                                .setGetObjectBucketName("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                .setEndpointUrl("https://beta.example.com")
                                .setClientDualstackEnabled(true)
                                .setClientRegion(Region.US_WEST_2)
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("outposts access point with fips enabled via client builder calling cross-region")
                                .setGetObjectBucketName("arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint")
                                .setClientFipsEnabled(true)
                                .setClientRegion(Region.US_EAST_1)
                                .setExpectedException(IllegalArgumentException.class));

        cases.add(new TestCase().setCaseName("mrap access point with arn region enabled")
                                .setGetObjectBucketName("arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap")
                                .setEndpointUrl("https://accesspoint.vpce-123-abc.s3-global.vpce.amazonaws.com")
                                .setS3Configuration(c -> c.useArnRegionEnabled(true))
                                .setClientRegion(Region.EU_WEST_1)
                                .setExpectedEndpoint("https://mfzwi23gnjvgw.mrap.accesspoint.vpce-123-abc.s3-global.vpce.amazonaws.com/object")
                                .setExpectedSigningServiceName("s3")
                                .setExpectedSigningRegion(Region.EU_WEST_1));

        return cases;
    }

    private static class TestCase {
        private String caseName;
        private URI endpointUrl;
        private String getObjectBucketName;
        private S3Configuration s3Configuration = S3Configuration.builder().build();
        private Region clientRegion;
        private URI expectedEndpoint;
        private String expectedSigningServiceName;
        private Region expectedSigningRegion;
        private Class<? extends RuntimeException> expectedException;
        private Boolean clientDualstackEnabled;
        private Boolean clientFipsEnabled;

        public TestCase setCaseName(String caseName) {
            this.caseName = caseName;
            return this;
        }

        public TestCase setGetObjectBucketName(String getObjectBucketName) {
            this.getObjectBucketName = getObjectBucketName;
            return this;
        }

        public TestCase setEndpointUrl(String endpointUrl) {
            this.endpointUrl = URI.create(endpointUrl);
            return this;
        }

        public TestCase setS3Configuration(Consumer<S3Configuration.Builder> s3Configuration) {
            S3Configuration.Builder configBuilder = S3Configuration.builder();
            s3Configuration.accept(configBuilder);
            this.s3Configuration = configBuilder.build();
            return this;
        }

        public TestCase setClientRegion(Region clientRegion) {
            this.clientRegion = clientRegion;
            return this;
        }

        public TestCase setClientDualstackEnabled(Boolean dualstackEnabled) {
            this.clientDualstackEnabled = dualstackEnabled;
            return this;
        }

        public TestCase setClientFipsEnabled(Boolean fipsEnabled) {
            this.clientFipsEnabled = fipsEnabled;
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
            return this.caseName + (getObjectBucketName == null ? "" : ": " + getObjectBucketName);
        }
    }
}
