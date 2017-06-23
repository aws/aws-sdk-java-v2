/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.awssdk.services.rds;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.junit.Test;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.internal.AmazonWebServiceRequestAdapter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.runtime.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.services.rds.model.CopyDBSnapshotRequest;
import software.amazon.awssdk.services.rds.transform.CopyDBSnapshotRequestMarshaller;

/**
 * Unit Tests for {@link PresignRequestHandler}
 */
public class PresignRequestHandlerTest {
    private static final AwsCredentials CREDENTIALS = new AwsCredentials("foo", "bar");
    private static final Region DESTINATION_REGION = Region.of("us-west-2");

    private static PresignRequestHandler<CopyDBSnapshotRequest> presignHandler = new CopyDbSnapshotPresignHandler();
    private final CopyDBSnapshotRequestMarshaller marshaller = new CopyDBSnapshotRequestMarshaller();

    @Test
    public void testSetsPresignedUrl() throws URISyntaxException {
        CopyDBSnapshotRequest request = makeTestRequest();
        presignHandler.beforeRequest(marshallRequest(request));

        assertNotNull(request.preSignedUrl());
    }

    @Test
    public void testComputesPresignedUrlCorrectly() throws URISyntaxException {
        // Note: test data was baselined by performing actual calls, with real
        // credentials to RDS and checking that they succeeded. Then the
        // request was recreated with all the same parameters but with test
        // credentials.
        final CopyDBSnapshotRequest request = CopyDBSnapshotRequest.builder()
                .sourceDBSnapshotIdentifier("arn:aws:rds:us-east-1:123456789012:snapshot:rds:test-instance-ss-2016-12-20-23-19")
                .targetDBSnapshotIdentifier("test-instance-ss-copy-2")
                .sourceRegion("us-east-1")
                .kmsKeyId("arn:aws:kms:us-west-2:123456789012:key/11111111-2222-3333-4444-555555555555")
                .build();

        Calendar c = new GregorianCalendar();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        // 20161221T180735Z
        // Note: month is 0-based
        c.set(2016, 11, 21, 18, 7, 35);

        PresignRequestHandler<CopyDBSnapshotRequest> handler = new CopyDbSnapshotPresignHandler(c.getTime());

        handler.beforeRequest(marshallRequest(request));

        final String expectedPreSignedUrl = "https://rds.us-east-1.amazonaws.com/?" +
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

        assertEquals(expectedPreSignedUrl, request.preSignedUrl());
    }

    @Test
    public void testSkipsPresigningIfUrlSet() throws URISyntaxException {
        CopyDBSnapshotRequest request = CopyDBSnapshotRequest.builder()
                .sourceRegion("us-west-2")
                .preSignedUrl("PRESIGNED")
                .build();


        presignHandler.beforeRequest(marshallRequest(request));

        assertEquals("PRESIGNED", request.preSignedUrl());
    }

    @Test
    public void testSkipsPresigningIfSourceRegionNotSet() throws URISyntaxException {
        CopyDBSnapshotRequest request = CopyDBSnapshotRequest.builder().build();

        presignHandler.beforeRequest(marshallRequest(request));

        assertNull(request.preSignedUrl());
    }

    @Test
    public void testParsesDestinationRegionfromRequestEndpoint() throws URISyntaxException {
        CopyDBSnapshotRequest request = CopyDBSnapshotRequest.builder()
                .sourceRegion("us-east-1")
                .build();
        Region destination = Region.of("us-west-2");
        SdkHttpFullRequest marshalled = marshallRequest(request);

        final SdkHttpFullRequest presignedRequest = presignHandler.beforeRequest(marshalled);

        final URI presignedUrl = new URI(request.preSignedUrl());
        assertTrue(presignedUrl.toString().contains("DestinationRegion=" + destination.value()));
    }

    @Test
    public void testSourceRegionRemovedFromOriginalRequest() throws URISyntaxException {
        SdkHttpFullRequest marshalled = marshallRequest(makeTestRequest());

        SdkHttpFullRequest actual = presignHandler.beforeRequest(marshalled);

        assertFalse(actual.getParameters().containsKey("SourceRegion"));
    }

    private SdkHttpFullRequest marshallRequest(CopyDBSnapshotRequest request) throws URISyntaxException {
        SdkHttpFullRequest marshalled = SdkHttpFullRequestAdapter.toHttpFullRequest(marshaller.marshall(request));
        return marshalled.toBuilder()
                         .endpoint(new DefaultServiceEndpointBuilder(RDSClient.SERVICE_NAME, Protocol.HTTPS.toString())
                                           .withRegion(DESTINATION_REGION)
                                           .getServiceEndpoint())
                         .handlerContext(AwsHandlerKeys.AWS_CREDENTIALS, CREDENTIALS)
                         .handlerContext(AwsHandlerKeys.REQUEST_CONFIG, new AmazonWebServiceRequestAdapter(request))
                         .build();
    }

    private CopyDBSnapshotRequest makeTestRequest() {
        return CopyDBSnapshotRequest.builder()
                .sourceDBSnapshotIdentifier("arn:aws:rds:us-east-1:123456789012:snapshot:rds:test-instance-ss-2016-12-20-23-19")
                .targetDBSnapshotIdentifier("test-instance-ss-copy-2")
                .sourceRegion("us-east-1")
                .kmsKeyId("arn:aws:kms:us-west-2:123456789012:key/11111111-2222-3333-4444-555555555555")
                .build();
    }
}
