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
import software.amazon.awssdk.services.s3.internal.presignedurl.model.InternalPresignedUrlGetObjectRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * Custom marshaller for presigned URL GetObject requests.
 * Unlike regular requests, this marshaller uses the presigned URL directly
 * and adds Range headers for partial downloads.
 */
@SdkInternalApi
final class PresignedUrlGetObjectRequestMarshaller implements Marshaller<InternalPresignedUrlGetObjectRequest> {

    @Override
    public SdkHttpFullRequest marshall(InternalPresignedUrlGetObjectRequest request) {
        Validate.paramNotNull(request, "request");
        Validate.paramNotNull(request.url(), "presigned URL");
        
        try {
            // Parse the presigned URL
            URI uri = URI.create(request.url());
            
            // Build the HTTP request using the presigned URL directly
            SdkHttpFullRequest.Builder httpRequestBuilder = SdkHttpFullRequest.builder()
                    .method(SdkHttpMethod.GET)
                    .uri(uri);
            
            // Add Range header if specified
            if (request.range() != null) {
                httpRequestBuilder.putHeader("Range", request.range());
            }
            
            SdkHttpFullRequest httpRequest = httpRequestBuilder.build();
            return httpRequest;
            
        } catch (Exception e) {
            throw SdkClientException.builder()
                    .message("Unable to marshall presigned URL request: " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }
}