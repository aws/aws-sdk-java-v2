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

package software.amazon.awssdk.eventnotifications.s3.model;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;

/**
 * The responseElements key value is useful if you want to trace a request by following up with AWS Support.
 * Both x-amz-request-id and x-amz-id-2 help Amazon S3 trace an individual request. These values are the same as those that
 * Amazon S3 returns in the response to the request that initiates the events. This is so they can be used to match the event
 * to the request.
 */
@SdkPublicApi
public class ResponseElements {

    private final String xAmzId2;
    private final String xAmzRequestId;

    public ResponseElements(String xAmzId2, String xAmzRequestId) {
        this.xAmzId2 = xAmzId2;
        this.xAmzRequestId = xAmzRequestId;
    }

    public String getXAmzId2() {
        return xAmzId2;
    }

    public String getXAmzRequestId() {
        return xAmzRequestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResponseElements that = (ResponseElements) o;

        if (!Objects.equals(xAmzId2, that.xAmzId2)) {
            return false;
        }
        return Objects.equals(xAmzRequestId, that.xAmzRequestId);
    }

    @Override
    public int hashCode() {
        int result = xAmzId2 != null ? xAmzId2.hashCode() : 0;
        result = 31 * result + (xAmzRequestId != null ? xAmzRequestId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("ResponseElements")
                       .add("xAmzId2", xAmzId2)
                       .add("xAmzRequestId", xAmzRequestId)
                       .build();
    }
}
