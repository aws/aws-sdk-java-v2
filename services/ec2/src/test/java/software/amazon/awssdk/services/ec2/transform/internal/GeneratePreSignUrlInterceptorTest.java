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

package software.amazon.awssdk.services.ec2.transform.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.AWS_CREDENTIALS;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.ec2.model.CopySnapshotRequest;

@RunWith(MockitoJUnitRunner.class)
public class GeneratePreSignUrlInterceptorTest {
    private static final GeneratePreSignUrlInterceptor INTERCEPTOR = new GeneratePreSignUrlInterceptor();

    @Mock
    private Context.ModifyHttpRequest mockContext;

    @Test
    public void copySnapshotRequest_httpsProtocolAddedToEndpoint() {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                .uri(URI.create("https://ec2.us-west-2.amazonaws.com"))
                .method(SdkHttpMethod.POST)
                .build();

        CopySnapshotRequest ec2Request = CopySnapshotRequest.builder()
                .sourceRegion("us-west-2")
                .destinationRegion("us-east-2")
                .build();

        when(mockContext.httpRequest()).thenReturn(request);
        when(mockContext.request()).thenReturn(ec2Request);

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AWS_CREDENTIALS, AwsBasicCredentials.create("foo", "bar"));

        SdkHttpRequest modifiedRequest = INTERCEPTOR.modifyHttpRequest(mockContext, attrs);

        String presignedUrl = modifiedRequest.rawQueryParameters().get("PresignedUrl").get(0);

        assertThat(presignedUrl).startsWith("https://");
    }

    @Test
    public void copySnapshotRequest_generatesCorrectPresignedUrl() {
        // Expected URL was derived by first making a request to EC2 using
        // valid credentials and a KMS encrypted snapshot and verifying that
        // the snapshot was copied to the destination region, also encrypted.
        // Then the same code was used to make a second request, changing only
        // the credentials and snapshot ID.
        String expectedPresignedUrl = "https://ec2.us-west-2.amazonaws.com?Action=CopySnapshot" +
                "&Version=2016-11-15" +
                "&DestinationRegion=us-east-1" +
                "&SourceRegion=us-west-2" +
                "&SourceSnapshotId=SNAPSHOT_ID" +
                "&X-Amz-Algorithm=AWS4-HMAC-SHA256" +
                "&X-Amz-Date=20200107T205609Z" +
                "&X-Amz-SignedHeaders=host" +
                "&X-Amz-Expires=604800" +
                "&X-Amz-Credential=akid%2F20200107%2Fus-west-2%2Fec2%2Faws4_request" +
                "&X-Amz-Signature=c1f5e34834292a86ff2b46b5e97cebaf2967b09641b4e2e60a382a37d137a03b";

        ZoneId utcZone = ZoneId.of("UTC").normalized();

        // Same signing date as the one used for the request above
        Instant signingInstant = ZonedDateTime.of(2020, 1, 7, 20, 56, 9, 0, utcZone).toInstant();
        Clock signingClock = Clock.fixed(signingInstant, utcZone);

        GeneratePreSignUrlInterceptor interceptor = new GeneratePreSignUrlInterceptor(signingClock);

        // These details don't really affect the test as they're not used in generating the signed URL
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                .uri(URI.create("https://ec2.us-west-2.amazonaws.com"))
                .method(SdkHttpMethod.POST)
                .build();

        CopySnapshotRequest ec2Request = CopySnapshotRequest.builder()
                .sourceRegion("us-west-2")
                .destinationRegion("us-east-1")
                .sourceSnapshotId("SNAPSHOT_ID")
                .build();

        when(mockContext.httpRequest()).thenReturn(request);
        when(mockContext.request()).thenReturn(ec2Request);

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AWS_CREDENTIALS, AwsBasicCredentials.create("akid", "skid"));

        SdkHttpRequest modifiedRequest = interceptor.modifyHttpRequest(mockContext, attrs);

        String generatedPresignedUrl = modifiedRequest.rawQueryParameters().get("PresignedUrl").get(0);

        assertThat(generatedPresignedUrl).isEqualTo(expectedPresignedUrl);
    }
}
