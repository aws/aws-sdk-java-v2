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

package software.amazon.awssdk.services.s3.internal.crossregion;

import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.DelegatingS3Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Request;

@SdkInternalApi
public final class S3CrossRegionSyncClient extends DelegatingS3Client {

    public S3CrossRegionSyncClient(S3Client s3Client) {
        super(s3Client);
    }

    @Override
    protected <T extends S3Request, ReturnT> ReturnT invokeOperation(T request, Function<T, ReturnT> operation) {
        Optional<String> bucket = request.getValueForField("bucket", String.class);

        if (bucket.isPresent()) {
            try {
                operation.apply(request); //TODO: add modifyRequest logic
            } catch (Exception e) {
                handleOperationFailure(e, bucket.get());
            }
        }

        return operation.apply(request);
    }

    private void handleOperationFailure(Throwable t, String bucket) {
        //TODO: handle failure case
    }
}
