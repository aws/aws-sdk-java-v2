/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.internal.handlers;

import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlDecode;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.model.EncodingType;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Encoding type affects the following values in the response:
 * <ul>
 *     <li>V1: Delimiter, Marker, Prefix, NextMarker, Key</li>
 *     <li>V2: Delimiter, Prefix, Key, and StartAfter</li>
 * </ul>
 * <p>
 * See <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html">https://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html</a>
 * and <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/v2-RESTBucketGET.html">https://docs.aws.amazon.com/AmazonS3/latest/API/v2-RESTBucketGET.html</a>
 */
@SdkInternalApi
public final class DecodeUrlEncodedResponseInterceptor implements ExecutionInterceptor {

    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context,
                                      ExecutionAttributes executionAttributes) {
        SdkResponse response = context.response();
        if (shouldHandle(response)) {
            if (response instanceof ListObjectsResponse) {
                response = modifyListObjectsResponse((ListObjectsResponse) response);
            } else if (response instanceof ListObjectsV2Response) {
                response = modifyListObjectsV2Response((ListObjectsV2Response) response);
            }
        }
        return response;
    }

    private static boolean shouldHandle(SdkResponse sdkResponse) {
        return sdkResponse.getValueForField("EncodingType", String.class)
                          .map(et -> EncodingType.URL.toString().equals(et))
                          .orElse(false);
    }

    // Elements to decode: Delimiter, Marker, Prefix, NextMarker, Key
    private static SdkResponse modifyListObjectsResponse(ListObjectsResponse response) {
        return response.toBuilder()
                .delimiter(urlDecode(response.delimiter()))
                .marker(urlDecode(response.delimiter()))
                .prefix(urlDecode(response.prefix()))
                .nextMarker(urlDecode(response.nextMarker()))
                .contents(decodeContents(response.contents()))
                .build();
    }

    // Elements to decode: Delimiter, Prefix, Key, and StartAfter
    private static SdkResponse modifyListObjectsV2Response(ListObjectsV2Response response) {
        return response.toBuilder()
                .delimiter(urlDecode(response.delimiter()))
                .prefix(urlDecode(response.prefix()))
                .startAfter(urlDecode(response.startAfter()))
                .contents(decodeContents(response.contents()))
                .build();
    }

    private static List<S3Object> decodeContents(List<S3Object> contents) {
        if (contents == null) {
            return null;
        }
        return contents.stream()
                       .map(o -> o.toBuilder().key(urlDecode(o.key())).build())
                       .collect(Collectors.toList());
    }
}
