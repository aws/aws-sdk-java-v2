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

package software.amazon.awssdk.retries.api.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of the {@link RecordSuccessRequest} interfface
 */
@SdkInternalApi
public final class RecordSuccessRequestImpl implements RecordSuccessRequest {
    private final RetryToken token;

    private RecordSuccessRequestImpl(RetryToken token) {
        this.token = Validate.paramNotNull(token, "token");
    }

    @Override
    public RetryToken token() {
        return token;
    }

    public static RecordSuccessRequest create(RetryToken token) {
        return new RecordSuccessRequestImpl(token);
    }
}
