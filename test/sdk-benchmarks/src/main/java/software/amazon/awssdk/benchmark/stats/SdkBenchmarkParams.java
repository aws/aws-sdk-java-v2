/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.benchmark.stats;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.BenchmarkParams;
import software.amazon.awssdk.core.util.VersionInfo;

/**
 * Contains metadata for the benchmark
 */
public class SdkBenchmarkParams {

    private String sdkVersion;

    private String jdkVersion;

    private String jvmName;

    private String jvmVersion;

    private Mode mode;

    public SdkBenchmarkParams() {
    }

    public SdkBenchmarkParams(BenchmarkParams benchmarkParams) {
        this.sdkVersion = VersionInfo.SDK_VERSION;
        this.jdkVersion = benchmarkParams.getJdkVersion();
        this.jvmName = benchmarkParams.getVmName();
        this.jvmVersion = benchmarkParams.getVmVersion();
        this.mode = benchmarkParams.getMode();
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public String getJdkVersion() {
        return jdkVersion;
    }

    public void setJdkVersion(String jdkVersion) {
        this.jdkVersion = jdkVersion;
    }

    public String getJvmName() {
        return jvmName;
    }

    public void setJvmName(String jvmName) {
        this.jvmName = jvmName;
    }

    public String getJvmVersion() {
        return jvmVersion;
    }

    public void setJvmVersion(String jvmVersion) {
        this.jvmVersion = jvmVersion;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

}
