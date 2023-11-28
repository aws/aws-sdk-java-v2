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

package software.amazon.awssdk.s3benchmarks.s3express;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * System settings set by the benchmark runner.
 */
@SdkPublicApi
public enum BenchmarkSystemSetting implements SystemSetting {
    BENCHMARK_TEST_ROLE,

    RUN_ID,

    ;


    @Override
    public String property() {
        return null;
    }

    @Override
    public String environmentVariable() {
        return name();
    }

    @Override
    public String defaultValue() {
        return null;
    }
}
