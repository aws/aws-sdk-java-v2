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

package software.amazon.awssdk.services.s3.internal.presignedurl;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.services.s3.internal.presignedurl.model.PresignedUrlGetObjectRequestWrapper;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link PresignedUrlGetObjectRequestWrapper} Marshaller
 *
 * <p>
 * Marshalls presigned URL requests by using the complete URL directly and adding optional Range headers.
 * Unlike regular S3 marshalers, this preserves all embedded authentication parameters in the presigned URL.
 * </p>
 */
@SdkInternalApi
public class PresignedUrlGetObjectRequestMarshaller implements Marshaller<PresignedUrlGetObjectRequestWrapper> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder()
            .requestUri("").httpMethod(SdkHttpMethod.GET).hasExplicitPayloadMember(false).hasPayloadMembers(false)
            .putAdditionalMetadata(AwsXmlProtocolFactory.ROOT_MARSHALL_LOCATION_ATTRIBUTE, null)
            .putAdditionalMetadata(AwsXmlProtocolFactory.XML_NAMESPACE_ATTRIBUTE, null).build();

    private final AwsXmlProtocolFactory protocolFactory;

    public PresignedUrlGetObjectRequestMarshaller(AwsXmlProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    /**
     * Marshalls the presigned URL request into an HTTP GET request.
     *
     * @param presignedUrlGetObjectRequestWrapper the request to marshall
     * @return HTTP request ready for execution
     * @throws SdkClientException if URL conversion fails
     */
    @Override
    public SdkHttpFullRequest marshall(PresignedUrlGetObjectRequestWrapper presignedUrlGetObjectRequestWrapper) {
        Validate.paramNotNull(presignedUrlGetObjectRequestWrapper, "presignedUrlGetObjectRequestWrapper");
        try {
            ProtocolMarshaller<SdkHttpFullRequest> protocolMarshaller = protocolFactory
                .createProtocolMarshaller(SDK_OPERATION_BINDING);
            URI presignedUri = presignedUrlGetObjectRequestWrapper.url().toURI();

            return protocolMarshaller.marshall(presignedUrlGetObjectRequestWrapper)
                                     .toBuilder()
                                     .uri(presignedUri)
                                     .build();
        } catch (Exception e) {
            throw SdkClientException.builder()
                    .message("Unable to marshall pre-signed URL Request: " + e.getMessage())
                    .cause(e).build();
        }
    }
}