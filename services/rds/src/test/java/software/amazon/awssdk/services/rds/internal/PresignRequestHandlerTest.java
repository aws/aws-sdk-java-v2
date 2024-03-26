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

package software.amazon.awssdk.services.rds.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.awssdk.services.rds.RdsServiceClientConfiguration;
import software.amazon.awssdk.services.rds.auth.scheme.RdsAuthSchemeProvider;
import software.amazon.awssdk.services.rds.model.CopyDbClusterSnapshotRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Unit Tests for {@link RdsPresignInterceptor}
 */
public class PresignRequestHandlerTest {
    private static final AwsBasicCredentials CREDENTIALS = AwsBasicCredentials.create("foo", "bar");
    private static final Region DESTINATION_REGION = Region.of("us-west-2");

    private static RdsPresignInterceptor<CopyDbSnapshotRequest> presignInterceptor = new CopyDbSnapshotPresignInterceptor();
    private final CopyDbSnapshotRequestMarshaller marshaller =
        new CopyDbSnapshotRequestMarshaller(RdsPresignInterceptor.PROTOCOL_FACTORY);

    @Test
    public void testSetsPresignedUrl() {
        CopyDbSnapshotRequest request = makeTestRequest();
        SdkHttpRequest presignedRequest = modifyHttpRequest(presignInterceptor, request, marshallRequest(request));

        assertNotNull(presignedRequest.rawQueryParameters().get("PreSignedUrl").get(0));
    }

    @Test
    public void testComputesPresignedUrlCorrectly() {
        // Note: test data was baselined by performing actual calls, with real
        // credentials to RDS and checking that they succeeded. Then the
        // request was recreated with all the same parameters but with test
        // credentials.
        final CopyDbSnapshotRequest request = CopyDbSnapshotRequest.builder()
                .sourceDBSnapshotIdentifier("arn:aws:rds:us-east-1:123456789012:snapshot:rds:test-instance-ss-2016-12-20-23-19")
                .targetDBSnapshotIdentifier("test-instance-ss-copy-2")
                .sourceRegion("us-east-1")
                .kmsKeyId("arn:aws:kms:us-west-2:123456789012:key/11111111-2222-3333-4444-555555555555")
                .build();

        Calendar c = new GregorianCalendar();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        // 20161221T180735Z
        // Note: month is 0-based
        c.set(2016, Calendar.DECEMBER, 21, 18, 7, 35);

        Clock signingDateOverride = Mockito.mock(Clock.class);
        when(signingDateOverride.millis()).thenReturn(c.getTimeInMillis());

        RdsPresignInterceptor<CopyDbSnapshotRequest> interceptor = new CopyDbSnapshotPresignInterceptor(signingDateOverride);

        SdkHttpRequest presignedRequest = modifyHttpRequest(interceptor, request, marshallRequest(request));

        final String expectedPreSignedUrl = "https://rds.us-east-1.amazonaws.com?" +
                "Action=CopyDBSnapshot" +
                "&Version=2014-10-31" +
                "&SourceDBSnapshotIdentifier=arn%3Aaws%3Ards%3Aus-east-1%3A123456789012%3Asnapshot%3Ards%3Atest-instance-ss-2016-12-20-23-19" +
                "&TargetDBSnapshotIdentifier=test-instance-ss-copy-2" +
                "&KmsKeyId=arn%3Aaws%3Akms%3Aus-west-2%3A123456789012%3Akey%2F11111111-2222-3333-4444-555555555555" +
                "&DestinationRegion=us-west-2" +
                "&X-Amz-Algorithm=AWS4-HMAC-SHA256" +
                "&X-Amz-Date=20161221T180735Z" +
                "&X-Amz-SignedHeaders=host" +
                "&X-Amz-Expires=604800" +
                "&X-Amz-Credential=foo%2F20161221%2Fus-east-1%2Frds%2Faws4_request" +
                "&X-Amz-Signature=f839ca3c728dc96e7c978befeac648296b9f778f6724073de4217173859d13d9";

        assertEquals(expectedPreSignedUrl, presignedRequest.rawQueryParameters().get("PreSignedUrl").get(0));
    }

    @Test
    public void testSkipsPresigningIfUrlSet() {
        CopyDbSnapshotRequest request = CopyDbSnapshotRequest.builder()
                .sourceRegion("us-west-2")
                .preSignedUrl("PRESIGNED")
                .build();


        SdkHttpRequest presignedRequest = modifyHttpRequest(presignInterceptor, request, marshallRequest(request));

        assertEquals("PRESIGNED", presignedRequest.rawQueryParameters().get("PreSignedUrl").get(0));
    }

    @Test
    public void testSkipsPresigningIfSourceRegionNotSet() {
        CopyDbSnapshotRequest request = CopyDbSnapshotRequest.builder().build();

        SdkHttpRequest presignedRequest = modifyHttpRequest(presignInterceptor, request, marshallRequest(request));

        assertNull(presignedRequest.rawQueryParameters().get("PreSignedUrl"));
    }

    @Test
    public void testParsesDestinationRegionfromRequestEndpoint() throws URISyntaxException {
        CopyDbSnapshotRequest request = CopyDbSnapshotRequest.builder()
                .sourceRegion("us-east-1")
                .build();
        Region destination = Region.of("us-west-2");
        SdkHttpFullRequest marshalled = marshallRequest(request);

        final SdkHttpRequest presignedRequest = modifyHttpRequest(presignInterceptor, request, marshalled);

        private TestCaseBuilder shouldContainPreSignedUrl(Boolean value) {
            this.shouldContainPreSignedUrl = value;
            return this;
        }

        private TestCaseBuilder expectedDestinationRegion(String value) {
            this.expectedDestinationRegion = value;
            return this;
        }

        public TestCaseBuilder signingClockOverride(Clock signingClockOverride) {
            this.signingClockOverride = signingClockOverride;
            return this;
        }

        public TestCaseBuilder expectedUri(String expectedUri) {
            this.expectedUri = expectedUri;
            return this;
        }

        public TestCase build() {
            return new TestCase(this);
        }
    }

    static class CapturingInterceptor implements ExecutionInterceptor {
        private static final RuntimeException BOOM = new RuntimeException("boom!");
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw BOOM;
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }

        public SdkHttpRequest httpRequest() {
            return context.httpRequest();
        }
    }
}
