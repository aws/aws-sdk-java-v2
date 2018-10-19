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

package software.amazon.awssdk.awscore.protocol.query;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.http.response.AwsQueryResponseHandler;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.protocol.query.marshall.QueryProtocolMarshaller;
import software.amazon.awssdk.core.internal.protocol.query.unmarshall.QueryProtocolUnmarshaller;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.SdkPojo;

/**
 * Protocol factory for the AWS/Query protocol.
 */
@SdkProtectedApi
public final class AwsEc2ProtocolFactory extends AwsQueryProtocolFactory {

    @Override
    public <T extends AwsRequest> ProtocolMarshaller<Request<T>> createProtocolMarshaller(OperationInfo operationInfo,
                                                                                          T origRequest) {
        return QueryProtocolMarshaller.builder(origRequest)
                                      .operationInfo(operationInfo)
                                      .isEc2(true)
                                      .build();
    }

    public <T extends AwsResponse> HttpResponseHandler<T> createResponseHandler(Supplier<SdkPojo> pojoSupplier) {
        return new AwsQueryResponseHandler<>(new QueryProtocolUnmarshaller<>(false), r -> pojoSupplier.get());
    }
}
