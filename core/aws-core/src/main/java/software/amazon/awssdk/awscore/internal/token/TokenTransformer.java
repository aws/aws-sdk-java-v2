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

package software.amazon.awssdk.awscore.internal.token;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.awscore.AwsResponse;

/**
 * Transformer to convert the response received from service to respective Token objects.
 * @param <T> AwsToken that needs to be transformed to.
 * @param <R> AwsResponse that needs to be transformed from.
 */

@SdkInternalApi
public interface TokenTransformer<T extends SdkToken, R extends  AwsResponse> {

    /**
     * API to convert the response received from the service to Token.
     * @param awsResponse Response received from service which will have token details.
     * @return
     */
    T transform(R awsResponse);
}
