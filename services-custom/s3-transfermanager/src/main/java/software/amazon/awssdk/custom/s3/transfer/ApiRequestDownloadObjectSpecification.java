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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of {@link DownloadObjectSpecification} for {@link GetObjectRequest}.
 */
@SdkInternalApi
final class ApiRequestDownloadObjectSpecification extends DownloadObjectSpecification {
    private final GetObjectRequest apiRequest;

    ApiRequestDownloadObjectSpecification(GetObjectRequest apiRequest) {
        this.apiRequest = Validate.notNull(apiRequest, "apiRequest must not be null");
    }

    @Override
    public boolean isApiRequest() {
        return true;
    }

    @Override
    public GetObjectRequest asApiRequest() {
        return apiRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApiRequestDownloadObjectSpecification that = (ApiRequestDownloadObjectSpecification) o;
        return apiRequest.equals(that.apiRequest);
    }

    @Override
    public int hashCode() {
        return apiRequest.hashCode();
    }
}
