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

package software.amazon.awssdk.services.s3.handlers;

//import java.lang.reflect.Method;
//import java.util.Arrays;
//import java.util.List;
//import software.amazon.awssdk.Request;
//import software.amazon.awssdk.Response;

import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.interceptor.ExecutionInterceptor;
//import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
//import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
//import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

@ReviewBeforeRelease("Finish this and hook it up")
public class DecodeUrlEncodedResponseInterceptor implements ExecutionInterceptor {

    //    @Override
    //    public void afterResponse(Request<?> request, Response<?> response) {
    //
    //        if (response.getAwsResponse() instanceof ListObjectsV2Response) {
    //            decodeListObjectsV2ResponseIfRequired(request, response);
    //        }
    //
    //        if (response.getAwsResponse() instanceof ListObjectVersionsResponse) {
    //            decodeListObjectVersionsResponseIfRequired(request, response);
    //        }
    //    }
    //
    //    public void decodeListObjectsV2ResponseIfRequired(Request<?> request, Response<?> response) {
    //        ListObjectsV2Request listObjectsV2Request = (ListObjectsV2Request) request.getOriginalRequest();
    //        ListObjectsV2Response listObjectsV2Response = (ListObjectsV2Response) response.getAwsResponse();
    //
    //        if (listObjectsV2Request.encodingType() != null) {
    //            listObjectsV2Response.toBuilder().
    //        }
    //
    //        response.
    //    }
}
