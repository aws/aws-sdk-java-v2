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

package software.amazon.awssdk.services.rds;

import java.net.URI;
import java.util.Date;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.runtime.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.util.AwsHostNameUtils;
import software.amazon.awssdk.util.SdkHttpUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Abstract pre-sign handler that follows the pre-signing scheme outlined in the 'RDS Presigned URL for Cross-Region Copying'
 * SEP.
 *
 * @param <T> The request type.
 */
abstract class PresignRequestHandler<T extends AmazonWebServiceRequest> extends RequestHandler {
    private static final String SERVICE_NAME = "rds";
    private static final String PARAM_SOURCE_REGION = "SourceRegion";
    private static final String PARAM_DESTINATION_REGION = "DestinationRegion";
    private static final String PARAM_PRESIGNED_URL = "PreSignedUrl";

    protected interface PresignableRequest {
        void setPreSignedUrl(String preSignedUrl);

        String getSourceRegion();

        SdkHttpFullRequest.Builder marshall();
    }

    private final Class<T> requestClassToPreSign;

    private final Date signingOverrideDate;

    protected PresignRequestHandler(Class<T> requestClassToPreSign) {
        this(requestClassToPreSign, null);
    }

    protected PresignRequestHandler(Class<T> requestClassToPreSign, Date signingOverrideDate) {
        this.requestClassToPreSign = requestClassToPreSign;
        if (signingOverrideDate != null) {
            this.signingOverrideDate = new Date(signingOverrideDate.getTime());
        } else {
            this.signingOverrideDate = null;
        }
    }

    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {
        Object originalRequest = request.handlerContext(AwsHandlerKeys.REQUEST_CONFIG).getOriginalRequest();
        if (!requestClassToPreSign.isInstance(originalRequest)) {
            return request;
        }

        if (request.getParameters().containsKey(PARAM_PRESIGNED_URL)) {
            return request;
        }

        PresignableRequest presignableRequest = adaptRequest(requestClassToPreSign.cast(originalRequest));

        String sourceRegion = presignableRequest.getSourceRegion();
        if (sourceRegion == null) {
            return request;
        }

        String destinationRegion = AwsHostNameUtils.parseRegion(request.getEndpoint().getHost(), SERVICE_NAME);

        SdkHttpFullRequest requestToPresign =
                presignableRequest.marshall()
                                  .removeQueryParameter(PARAM_SOURCE_REGION)
                                  .queryParameter(PARAM_DESTINATION_REGION, destinationRegion)
                                  .httpMethod(SdkHttpMethod.GET)
                                  .endpoint(createEndpoint(sourceRegion, SERVICE_NAME))
                                  .build();

        AwsCredentials credentials = request.handlerContext(AwsHandlerKeys.AWS_CREDENTIALS);

        requestToPresign = presignRequest(requestToPresign, credentials, sourceRegion);

        final String presignedUrl = generateUrl(requestToPresign);

        presignableRequest.setPreSignedUrl(presignedUrl);

        return request.toBuilder()
                      .queryParameter(PARAM_PRESIGNED_URL, presignedUrl)
                      // Remove the unmodeled params to stop them getting onto the wire
                      .removeQueryParameter(PARAM_SOURCE_REGION)
                      .build();
    }

    protected abstract PresignableRequest adaptRequest(T originalRequest);

    private SdkHttpFullRequest presignRequest(SdkHttpFullRequest request, AwsCredentials credentials, String signingRegion) {
        Aws4Signer signer = createNewSignerWithRegion(signingRegion);
        return signer.presignRequest(request, credentials, null);
    }

    private Aws4Signer createNewSignerWithRegion(String signingRegion) {
        Aws4Signer signer = new Aws4Signer(true);
        signer.setRegionName(signingRegion);
        signer.setServiceName(SERVICE_NAME);
        signer.setOverrideDate(signingOverrideDate);
        return signer;
    }

    private URI createEndpoint(String regionName, String serviceName) {
        final Region region = Region.of(regionName);

        if (region == null) {
            throw new AmazonClientException("{" + serviceName + ", " + regionName + "} was not "
                                            + "found in region metadata. Update to latest version of SDK and try again.");
        }

        return new DefaultServiceEndpointBuilder(SERVICE_NAME, Protocol.HTTPS.toString())
                .withRegion(region)
                .getServiceEndpoint();
    }

    private String generateUrl(SdkHttpFullRequest request) {
        URI endpoint = request.getEndpoint();
        String uri = SdkHttpUtils.appendUri(endpoint.toString(),
                                            request.getResourcePath(), true);
        String encodedParams = SdkHttpUtils.encodeParameters(request);

        if (!StringUtils.isEmpty(encodedParams)) {
            uri += "?" + encodedParams;
        }

        return uri;

    }

}


