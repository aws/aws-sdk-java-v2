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

package software.amazon.awssdk.codegen.model.rules.endpoints;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

public enum BuiltInParameter {
    AWS_REGION,
    AWS_USE_DUAL_STACK,
    AWS_USE_FIPS,
    SDK_ENDPOINT,
    AWS_STS_USE_GLOBAL_ENDPOINT,
    AWS_S3_FORCE_PATH_STYLE,
    AWS_S3_ACCELERATE,
    AWS_S3_USE_GLOBAL_ENDPOINT,
    AWS_S3_DISABLE_MULTI_REGION_ACCESS_POINTS,
    AWS_S3_USE_ARN_REGION,
    AWS_S3_CONTROL_USE_ARN_REGION
    ;

    @JsonCreator
    public static BuiltInParameter fromValue(String s) {
        switch (s.toLowerCase(Locale.ENGLISH)) {
            case "aws::region":
                return AWS_REGION;
            case "aws::usedualstack":
                return AWS_USE_DUAL_STACK;
            case "aws::usefips":
                return AWS_USE_FIPS;
            case "sdk::endpoint":
                return SDK_ENDPOINT;
            case "aws::sts::useglobalendpoint":
                return AWS_STS_USE_GLOBAL_ENDPOINT;
            case "aws::s3::forcepathstyle":
                return AWS_S3_FORCE_PATH_STYLE;
            case "aws::s3::accelerate":
                return AWS_S3_ACCELERATE;
            case "aws::s3::useglobalendpoint":
                return AWS_S3_USE_GLOBAL_ENDPOINT;
            case "aws::s3::disablemultiregionaccesspoints":
                return AWS_S3_DISABLE_MULTI_REGION_ACCESS_POINTS;
            case "aws::s3::usearnregion":
                return AWS_S3_USE_ARN_REGION;
            case "aws::s3control::usearnregion":
                return AWS_S3_CONTROL_USE_ARN_REGION;
            default:
                throw new RuntimeException("Unrecognized builtin: " + s);
        }
    }
}
