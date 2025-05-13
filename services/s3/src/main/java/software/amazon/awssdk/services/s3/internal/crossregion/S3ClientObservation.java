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

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.docs.ObservationDocumentation;

public enum S3ClientObservation implements ObservationDocumentation {

    S3_OPERATION {
        @Override
        public String getName() {
            return "s3.operation";
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return new KeyName[] {
                BucketKeyNames.BUCKET_NAME,
                BucketKeyNames.OPERATION_NAME,
                BucketKeyNames.REGION
            };
        }
    };

    public enum BucketKeyNames implements KeyName {
        BUCKET_NAME {
            @Override
            public String asString() {
                return "bucket.name";
            }
        },
        OPERATION_NAME {
            @Override
            public String asString() {
                return "operation.name";
            }
        },
        REGION {
            @Override
            public String asString() {
                return "aws.region";
            }
        }
    }
}
