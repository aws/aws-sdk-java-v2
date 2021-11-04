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

package software.amazon.awssdk.services.neptune.internal;

import org.junit.Test;
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
import software.amazon.awssdk.services.neptune.model.CopyDbClusterSnapshotRequest;
import software.amazon.awssdk.services.neptune.model.NeptuneRequest;
import software.amazon.awssdk.services.neptune.transform.CopyDbClusterSnapshotRequestMarshaller;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit Tests for {@link RdsPresignInterceptor}
 */
public class PresignRequestHandlerTest {
    private static final AwsBasicCredentials CREDENTIALS = AwsBasicCredentials.create("foo", "bar");
    private static final Region DESTINATION_REGION = Region.of("us-west-2");

    private static final RdsPresignInterceptor<CopyDbClusterSnapshotRequest> presignInterceptor = new CopyDbClusterSnapshotPresignInterceptor();
    private final CopyDbClusterSnapshotRequestMarshaller marshaller =
            new CopyDbClusterSnapshotRequestMarshaller(RdsPresignInterceptor.PROTOCOL_FACTORY);

    @Test
    public void testSetsPresignedUrl() {
        CopyDbClusterSnapshotRequest request = makeTestRequest();
        SdkHttpRequest presignedRequest = modifyHttpRequest(presignInterceptor, request, marshallRequest(request));

        assertNotNull(presignedRequest.rawQueryParameters().get("PreSignedUrl").get(0));
    }

    @Test
    public void testComputesPresignedUrlCorrectlyForCopyDbClusterSnapshotRequest() {
        // Note: test data was baselined by performing actual calls, with real
        // credentials to RDS and checking that they succeeded. Then the
        // request was recreated with all the same parameters but with test
        // credentials.
        final CopyDbClusterSnapshotRequest request = CopyDbClusterSnapshotRequest.builder()
                .sourceDBClusterSnapshotIdentifier("arn:aws:rds:us-east-1:123456789012:snapshot:rds:test-instance-ss-2016-12-20-23-19")
                .targetDBClusterSnapshotIdentifier("test-instance-ss-copy-2")
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

        RdsPresignInterceptor<CopyDbClusterSnapshotRequest> interceptor = new CopyDbClusterSnapshotPresignInterceptor(signingDateOverride);

        SdkHttpRequest presignedRequest = modifyHttpRequest(interceptor, request, marshallRequest(request));

        final String expectedPreSignedUrl = "https://rds.us-east-1.amazonaws.com?" +
                "Action=CopyDBClusterSnapshot" +
                "&Version=2014-10-31" +
                "&SourceDBClusterSnapshotIdentifier=arn%3Aaws%3Ards%3Aus-east-1%3A123456789012%3Asnapshot%3Ards%3Atest-instance-ss-2016-12-20-23-19" +
                "&TargetDBClusterSnapshotIdentifier=test-instance-ss-copy-2" +
                "&KmsKeyId=arn%3Aaws%3Akms%3Aus-west-2%3A123456789012%3Akey%2F11111111-2222-3333-4444-555555555555" +
                "&DestinationRegion=us-west-2" +
                "&X-Amz-Algorithm=AWS4-HMAC-SHA256" +
                "&X-Amz-Date=20161221T180735Z" +
                "&X-Amz-SignedHeaders=host" +
                "&X-Amz-Expires=604800" +
                "&X-Amz-Credential=foo%2F20161221%2Fus-east-1%2Frds%2Faws4_request" +
                "&X-Amz-Signature=00822ebbba95e2e6ac09112aa85621fbef060a596e3e1480f9f4ac61493e9821";
        assertEquals(expectedPreSignedUrl, presignedRequest.rawQueryParameters().get("PreSignedUrl").get(0));
    }

    @Test
    public void testSkipsPresigningIfUrlSet() {
        CopyDbClusterSnapshotRequest request = CopyDbClusterSnapshotRequest.builder()
                .sourceRegion("us-west-2")
                .preSignedUrl("PRESIGNED")
                .build();


        SdkHttpRequest presignedRequest = modifyHttpRequest(presignInterceptor, request, marshallRequest(request));

        assertEquals("PRESIGNED", presignedRequest.rawQueryParameters().get("PreSignedUrl").get(0));
    }

    @Test
    public void testSkipsPresigningIfSourceRegionNotSet() {
        CopyDbClusterSnapshotRequest request = CopyDbClusterSnapshotRequest.builder().build();

        SdkHttpRequest presignedRequest = modifyHttpRequest(presignInterceptor, request, marshallRequest(request));

        assertNull(presignedRequest.rawQueryParameters().get("PreSignedUrl"));
    }

    @Test
    public void testParsesDestinationRegionfromRequestEndpoint() throws URISyntaxException {
        CopyDbClusterSnapshotRequest request = CopyDbClusterSnapshotRequest.builder()
                .sourceRegion("us-east-1")
                .build();
        Region destination = Region.of("us-west-2");
        SdkHttpFullRequest marshalled = marshallRequest(request);

        final SdkHttpRequest presignedRequest = modifyHttpRequest(presignInterceptor, request, marshalled);

        final URI presignedUrl = new URI(presignedRequest.rawQueryParameters().get("PreSignedUrl").get(0));
        assertTrue(presignedUrl.toString().contains("DestinationRegion=" + destination.id()));
    }

    @Test
    public void testSourceRegionRemovedFromOriginalRequest() {
        CopyDbClusterSnapshotRequest request = makeTestRequest();
        SdkHttpFullRequest marshalled = marshallRequest(request);
        SdkHttpRequest actual = modifyHttpRequest(presignInterceptor, request, marshalled);

        assertFalse(actual.rawQueryParameters().containsKey("SourceRegion"));
    }

    private SdkHttpFullRequest marshallRequest(CopyDbClusterSnapshotRequest request) {
        SdkHttpFullRequest.Builder marshalled = marshaller.marshall(request).toBuilder();

        URI endpoint = new DefaultServiceEndpointBuilder("rds", Protocol.HTTPS.toString())
                .withRegion(DESTINATION_REGION)
                .getServiceEndpoint();
        return marshalled.uri(endpoint).build();
    }

    private ExecutionAttributes executionAttributes() {
        return new ExecutionAttributes().putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, CREDENTIALS)
                                        .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, DESTINATION_REGION)
                                        .putAttribute(SdkExecutionAttribute.PROFILE_FILE, ProfileFile.defaultProfileFile())
                                        .putAttribute(SdkExecutionAttribute.PROFILE_NAME, "default");
    }

    private CopyDbClusterSnapshotRequest makeTestRequest() {
        return CopyDbClusterSnapshotRequest.builder()
                .sourceDBClusterSnapshotIdentifier("arn:aws:rds:us-east-1:123456789012:snapshot:rds:test-instance-ss-2016-12-20-23-19")
                .targetDBClusterSnapshotIdentifier("test-instance-ss-copy-2")
                .sourceRegion("us-east-1")
                .kmsKeyId("arn:aws:kms:us-west-2:123456789012:key/11111111-2222-3333-4444-555555555555")
                .build();
    }

    private SdkHttpRequest modifyHttpRequest(ExecutionInterceptor interceptor,
                                             NeptuneRequest request,
                                             SdkHttpFullRequest httpRequest) {
        InterceptorContext context = InterceptorContext.builder().request(request).httpRequest(httpRequest).build();
        return interceptor.modifyHttpRequest(context, executionAttributes());
    }
}
