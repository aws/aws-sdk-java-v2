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

import io.micrometer.common.KeyValues;
import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
public class S3OperationConvention implements GlobalObservationConvention<S3OperationContext> {

    @Override
    public String getName() {
        return S3ClientObservation.S3_OPERATION.getName();
    }

    @Override
    public String getContextualName(S3OperationContext context) {
        return "s3 " + (context.getOperationName() != null ? context.getOperationName() : "operation");
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(S3OperationContext context) {
        return KeyValues.of(
            S3ClientObservation.BucketKeyNames.BUCKET_NAME.asString(),
            context.getBucketName() != null ? context.getBucketName() : "unknown",
            S3ClientObservation.BucketKeyNames.OPERATION_NAME.asString(),
            context.getOperationName() != null ? context.getOperationName() : "unknown",
            S3ClientObservation.BucketKeyNames.REGION.asString(),
            context.getRegion() != null ? context.getRegion() : "unknown"
        );
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof S3OperationContext;
    }
}
