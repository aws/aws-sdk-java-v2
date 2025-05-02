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

package software.amazon.awssdk.services.s3.regression;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;

public class TestConfig {
    private S3ClientFlavor flavor;
    private BucketType bucketType;
    private boolean forcePathStyle;
    private RequestChecksumCalculation requestChecksumValidation;
    private boolean accelerateEnabled;

    public S3ClientFlavor getFlavor() {
        return flavor;
    }

    public void setFlavor(S3ClientFlavor flavor) {
        this.flavor = flavor;
    }

    public BucketType getBucketType() {
        return bucketType;
    }

    public void setBucketType(BucketType bucketType) {
        this.bucketType = bucketType;
    }

    public boolean isForcePathStyle() {
        return forcePathStyle;
    }

    public void setForcePathStyle(boolean forcePathStyle) {
        this.forcePathStyle = forcePathStyle;
    }

    public RequestChecksumCalculation getRequestChecksumValidation() {
        return requestChecksumValidation;
    }

    public void setRequestChecksumValidation(RequestChecksumCalculation requestChecksumValidation) {
        this.requestChecksumValidation = requestChecksumValidation;
    }

    public boolean isAccelerateEnabled() {
        return accelerateEnabled;
    }

    public void setAccelerateEnabled(boolean accelerateEnabled) {
        this.accelerateEnabled = accelerateEnabled;
    }

    @Override
    public String toString() {
        return "[" +
               "flavor=" + flavor +
               ", bucketType=" + bucketType +
               ", forcePathStyle=" + forcePathStyle +
               ", requestChecksumValidation=" + requestChecksumValidation +
               ", accelerateEnabled=" + accelerateEnabled +
               ']';
    }

    public static List<TestConfig> testConfigs() {
        List<TestConfig> configs = new ArrayList<>();

        boolean[] forcePathStyle = {true, false};
        RequestChecksumCalculation[] checksumValidations = {RequestChecksumCalculation.WHEN_REQUIRED,
                                                            RequestChecksumCalculation.WHEN_SUPPORTED};
        boolean[] accelerateEnabled = {true, false};
        for (boolean pathStyle : forcePathStyle) {
            for (RequestChecksumCalculation checksumValidation : checksumValidations) {
                for (S3ClientFlavor flavor : S3ClientFlavor.values()) {
                    for (BucketType bucketType : BucketType.values()) {
                        for (boolean accelerate : accelerateEnabled) {
                            TestConfig testConfig = new TestConfig();
                            testConfig.setFlavor(flavor);
                            testConfig.setBucketType(bucketType);
                            testConfig.setForcePathStyle(pathStyle);
                            testConfig.setRequestChecksumValidation(checksumValidation);
                            testConfig.setAccelerateEnabled(accelerate);
                            configs.add(testConfig);
                        }
                    }
                }
            }
        }

        return configs;
    }

}
