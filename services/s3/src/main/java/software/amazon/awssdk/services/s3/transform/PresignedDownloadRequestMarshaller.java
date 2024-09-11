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

package software.amazon.awssdk.services.s3.transform;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.model.PresignedDownloadRequest;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public class PresignedDownloadRequestMarshaller implements Marshaller<PresignedDownloadRequest> {

    @Override
    public SdkHttpFullRequest marshall(PresignedDownloadRequest request) {
        Validate.paramNotNull(request, "presignedDownloadRequest");

        URI presignedUri = null;
        try {
            presignedUri = request.presignedUrl().toURI();
        } catch (URISyntaxException e) {
            throw SdkClientException.create(e.getMessage(), e);
        }

        return SdkHttpFullRequest.builder()
                                 .headers(extractHeaders(request))
                                 .uri(presignedUri)
                                 .method(SdkHttpMethod.GET)
                                 .build();
    }

    private Map<String, List<String>> extractHeaders(PresignedDownloadRequest request) {
        Map<String, List<String>> headers = new HashMap<>(request.customHeaders());

        if (request.startByte() != null || request.endByte() != null) {
            long start = request.startByte() != null ? request.startByte() : 0;
            String end = request.endByte() != null ? String.valueOf(request.endByte()) : "";
            headers.put("Range", Collections.singletonList("bytes=" + start + "-" + end));
        }

        return headers;
    }
}
