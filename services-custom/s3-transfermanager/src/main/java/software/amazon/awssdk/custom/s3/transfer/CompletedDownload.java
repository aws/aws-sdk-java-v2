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

package software.amazon.awssdk.custom.s3.transfer;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.custom.s3.transfer.internal.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * A completed download transfer.
 */
@SdkPublicApi
public interface CompletedDownload extends CompletedTransfer {

    /**
     * Returns the API response from the {@link S3CrtAsyncClient#getObject}
     * @return the response
     */
    GetObjectResponse response();
}
