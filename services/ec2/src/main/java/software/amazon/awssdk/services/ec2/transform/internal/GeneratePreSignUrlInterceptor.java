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

import static software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.AWS_CREDENTIALS;

import java.net.URI;
import java.time.Clock;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.awscore.util.AwsHostNameUtils;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.protocols.query.AwsEc2ProtocolFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CopySnapshotRequest;
import software.amazon.awssdk.services.ec2.transform.CopySnapshotRequestMarshaller;

/**
 * ExecutionInterceptor that generates a pre-signed URL for copying encrypted snapshots
 */
@SdkInternalApi
public final class GeneratePreSignUrlInterceptor implements ExecutionInterceptor {

    private static final URI CUSTOM_ENDPOINT_LOCALHOST = URI.create("http://localhost");

    private static final AwsEc2ProtocolFactory PROTOCOL_FACTORY = AwsEc2ProtocolFactory
        .builder()
        // Need an endpoint to marshall but this will be overwritten in modifyHttpRequest
        .clientConfiguration(SdkClientConfiguration.builder()
                                                   .option(SdkClientOption.ENDPOINT, CUSTOM_ENDPOINT_LOCALHOST)
                                                   .build())
        .build();

    private static final CopySnapshotRequestMarshaller MARSHALLER = new CopySnapshotRequestMarshaller(PROTOCOL_FACTORY);

    private final Clock testClock; // for testing only

    public GeneratePreSignUrlInterceptor() {
        testClock = null;
    }

    @SdkTestInternalApi
    GeneratePreSignUrlInterceptor(Clock testClock) {
        this.testClock = testClock;
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpRequest request = context.httpRequest();
        SdkRequest originalRequest = context.request();

        if (originalRequest instanceof CopySnapshotRequest) {

            CopySnapshotRequest originalCopySnapshotRequest = (CopySnapshotRequest) originalRequest;

            // Return if presigned url is already specified by the user.
            if (originalCopySnapshotRequest.presignedUrl() != null) {
                return request;
            }

            String serviceName = "ec2";

            // The source regions where the snapshot currently resides.
            String sourceRegion = originalCopySnapshotRequest.sourceRegion();
            String sourceSnapshotId = originalCopySnapshotRequest
                    .sourceSnapshotId();

            /*
             * The region where the snapshot has to be copied from the source.
             * The original copy snap shot request will have the end point set
             * as the destination region in the client before calling this
             * request.
             */
            String destinationRegion = originalCopySnapshotRequest.destinationRegion();

            if (destinationRegion == null) {
                destinationRegion =
                        AwsHostNameUtils.parseSigningRegion(request.host(), serviceName)
                                        .orElseThrow(() -> new IllegalArgumentException("Could not determine region for " +
                                                                                        request.host()))
                                        .id();
            }

            URI endPointSource = createEndpoint(sourceRegion, serviceName);

            SdkHttpFullRequest requestForPresigning = generateRequestForPresigning(
                    sourceSnapshotId, sourceRegion, destinationRegion)
                    .toBuilder()
                    .uri(endPointSource)
                    .method(SdkHttpMethod.GET)
                    .build();

            Aws4Signer signer = Aws4Signer.create();
            Aws4PresignerParams signingParams = getPresignerParams(executionAttributes, sourceRegion, serviceName);

            SdkHttpFullRequest presignedRequest = signer.presign(requestForPresigning, signingParams);

            return request.toBuilder()
                          .putRawQueryParameter("DestinationRegion", destinationRegion)
                          .putRawQueryParameter("PresignedUrl", presignedRequest.getUri().toString())
                          .build();
        }

        return request;
    }

    private Aws4PresignerParams getPresignerParams(ExecutionAttributes attributes, String signingRegion, String signingName) {
        return Aws4PresignerParams.builder()
                                  .signingRegion(Region.of(signingRegion))
                                  .signingName(signingName)
                                  .awsCredentials(attributes.getAttribute(AWS_CREDENTIALS))
                                  .signingClockOverride(testClock)
                                  .build();
    }

    /**
     * Generates a Request object for the pre-signed URL.
     */
    private SdkHttpFullRequest generateRequestForPresigning(String sourceSnapshotId,
                                                            String sourceRegion,
                                                            String destinationRegion) {

        CopySnapshotRequest copySnapshotRequest = CopySnapshotRequest.builder()
                                                                     .sourceSnapshotId(sourceSnapshotId)
                                                                     .sourceRegion(sourceRegion)
                                                                     .destinationRegion(destinationRegion)
                                                                     .build();

        return MARSHALLER.marshall(copySnapshotRequest);
    }

    private URI createEndpoint(String regionName, String serviceName) {

        Region region = Region.of(regionName);

        if (region == null) {
            throw SdkClientException.builder()
                                    .message("{" + serviceName + ", " + regionName + "} was not "
                                             + "found in region metadata. Update to latest version of SDK and try again.")
                                    .build();
        }

        URI endpoint = Ec2Client.serviceMetadata().endpointFor(region);
        if (endpoint.getScheme() == null) {
            return URI.create("https://" + endpoint);
        }
        return endpoint;
    }
}
