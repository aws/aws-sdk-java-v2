/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.ec2.transform;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.EC2Client;
import software.amazon.awssdk.services.ec2.model.CopySnapshotRequest;
import software.amazon.awssdk.util.AwsHostNameUtils;
import software.amazon.awssdk.util.SdkHttpUtils;

/**
 * RequestHandler that generates a pre-signed URL for copying encrypted
 * snapshots
 */
public class GeneratePreSignUrlRequestHandler extends RequestHandler {

    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {

        Object originalRequest = request.handlerContext(AwsHandlerKeys.REQUEST_CONFIG).getOriginalRequest();

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

            URI endPointDestination = request.getEndpoint();
            String destinationRegion = originalCopySnapshotRequest
                                               .destinationRegion() != null ? originalCopySnapshotRequest
                    .destinationRegion() : AwsHostNameUtils
                    .parseRegionName(endPointDestination.getHost(), serviceName);

            URI endPointSource = createEndpoint(sourceRegion, serviceName);

            SdkHttpFullRequest requestForPresigning = generateRequestForPresigning(
                    sourceSnapshotId, sourceRegion, destinationRegion)
                    .toBuilder()
                    .endpoint(endPointSource)
                    .httpMethod(SdkHttpMethod.GET)
                    .build();

            Aws4Signer signer = new Aws4Signer();
            signer.setServiceName(serviceName);

            final SdkHttpFullRequest presignedRequest = signer
                    .presignRequest(requestForPresigning, request.handlerContext(AwsHandlerKeys.AWS_CREDENTIALS), null);

            return request.toBuilder()
                          .queryParameter("DestinationRegion", destinationRegion)
                          .queryParameter("PresignedUrl", generateUrl(presignedRequest))
                          .build();
        }

        return request;

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

        return SdkHttpFullRequestAdapter.toHttpFullRequest(new CopySnapshotRequestMarshaller().marshall(copySnapshotRequest));

    }

    private String generateUrl(SdkHttpFullRequest request) {

        URI endpoint = request.getEndpoint();
        String uri = SdkHttpUtils.appendUri(endpoint.toString(),
                                            request.getResourcePath(), true);
        String encodedParams = SdkHttpUtils.encodeParameters(request);

        if (encodedParams != null) {
            uri += "?" + encodedParams;
        }

        return uri;

    }

    private URI createEndpoint(String regionName, String serviceName) {

        final Region region = Region.of(regionName);

        if (region == null) {
            throw new AmazonClientException("{" + serviceName + ", " + regionName + "} was not "
                                            + "found in region metadata. Update to latest version of SDK and try again.");
        }

        return EC2Client.serviceMetadata().endpointFor(region);
    }

    /**
     * Returns the endpoint as a URI.
     */
    private URI toUri(String endpoint) throws IllegalArgumentException {

        if (endpoint.contains("://") == false) {
            endpoint = Protocol.HTTPS + "://" + endpoint;
        }

        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
