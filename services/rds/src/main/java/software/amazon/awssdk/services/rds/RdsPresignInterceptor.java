/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.auth.signer.internal.AwsSignerExecutionAttribute.AWS_CREDENTIALS;

import java.net.URI;
import java.time.Clock;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.awscore.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.SdkHttpFullRequestAdapter;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.util.AwsHostNameUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.model.RdsRequest;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Abstract pre-sign handler that follows the pre-signing scheme outlined in the 'RDS Presigned URL for Cross-Region Copying'
 * SEP.
 *
 * @param <T> The request type.
 */
abstract class RdsPresignInterceptor<T extends RdsRequest> implements ExecutionInterceptor {
    private static final String SERVICE_NAME = "rds";
    private static final String PARAM_SOURCE_REGION = "SourceRegion";
    private static final String PARAM_DESTINATION_REGION = "DestinationRegion";
    private static final String PARAM_PRESIGNED_URL = "PreSignedUrl";

    protected interface PresignableRequest {
        String getSourceRegion();

        Request<?> marshall();
    }

    private final Class<T> requestClassToPreSign;

    private final Clock signingOverrideClock;

    RdsPresignInterceptor(Class<T> requestClassToPreSign) {
        this(requestClassToPreSign, null);
    }

    RdsPresignInterceptor(Class<T> requestClassToPreSign, Clock signingOverrideClock) {
        this.requestClassToPreSign = requestClassToPreSign;
        this.signingOverrideClock = signingOverrideClock;
    }

    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();
        SdkRequest originalRequest = context.request();
        if (!requestClassToPreSign.isInstance(originalRequest)) {
            return request;
        }

        if (request.rawQueryParameters().containsKey(PARAM_PRESIGNED_URL)) {
            return request;
        }

        PresignableRequest presignableRequest = adaptRequest(requestClassToPreSign.cast(originalRequest));

        String sourceRegion = presignableRequest.getSourceRegion();
        if (sourceRegion == null) {
            return request;
        }

        String destinationRegion = AwsHostNameUtils.parseRegion(request.host(), SERVICE_NAME);

        URI endpoint = createEndpoint(sourceRegion, SERVICE_NAME);
        Request<?> legacyRequest = presignableRequest.marshall();
        legacyRequest.setEndpoint(endpoint);
        SdkHttpFullRequest.Builder marshalledRequest = SdkHttpFullRequestAdapter.toMutableHttpFullRequest(legacyRequest);
        SdkHttpFullRequest requestToPresign =
                marshalledRequest.encodedPath(SdkHttpUtils.appendUri(endpoint.getPath(), marshalledRequest.encodedPath()))
                                 .method(SdkHttpMethod.GET)
                                 .putRawQueryParameter(PARAM_DESTINATION_REGION, destinationRegion)
                                 .removeQueryParameter(PARAM_SOURCE_REGION)
                                 .build();

        requestToPresign = presignRequest(requestToPresign, executionAttributes, sourceRegion);

        final String presignedUrl = requestToPresign.getUri().toString();

        return request.toBuilder()
                      .putRawQueryParameter(PARAM_PRESIGNED_URL, presignedUrl)
                      // Remove the unmodeled params to stop them getting onto the wire
                      .removeQueryParameter(PARAM_SOURCE_REGION)
                      .build();
    }

    protected abstract PresignableRequest adaptRequest(T originalRequest);

    private SdkHttpFullRequest presignRequest(SdkHttpFullRequest request,
                                              ExecutionAttributes attributes,
                                              String signingRegion) {

        Aws4Signer signer = Aws4Signer.create();
        Aws4PresignerParams presignerParams = Aws4PresignerParams.builder()
                                                                 .signingRegion(Region.of(signingRegion))
                                                                 .signingName(SERVICE_NAME)
                                                                 .signingClockOverride(signingOverrideClock)
                                                                 .awsCredentials(attributes.getAttribute(AWS_CREDENTIALS))
                                                                 .build();

        return signer.presign(request, presignerParams);
    }

    private URI createEndpoint(String regionName, String serviceName) {
        final Region region = Region.of(regionName);

        if (region == null) {
            throw SdkClientException.builder()
                                    .message("{" + serviceName + ", " + regionName + "} was not "
                                            + "found in region metadata. Update to latest version of SDK and try again.")
                                    .build();
        }

        return new DefaultServiceEndpointBuilder(SERVICE_NAME, Protocol.HTTPS.toString())
                .withRegion(region)
                .getServiceEndpoint();
    }
}


