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

package software.amazon.awssdk.services.s3.regression.upload;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.services.s3.regression.BucketType;
import software.amazon.awssdk.utils.ToString;

public class FlattenUploadConfig {
    private BucketType bucketType;
    private boolean forcePathStyle;
    private RequestChecksumCalculation requestChecksumValidation;
    private UploadStreamingRegressionTesting.BodyType bodyType;
    private UploadStreamingRegressionTesting.ContentSize contentSize;
    private boolean payloadSigning;

    public static List<FlattenUploadConfig> testConfigs() {
        List<FlattenUploadConfig> configs = new ArrayList<>();

        boolean[] payloadSign = {true, false};
        RequestChecksumCalculation[] checksumValidations = {RequestChecksumCalculation.WHEN_REQUIRED,
                                                            RequestChecksumCalculation.WHEN_SUPPORTED};
        for (RequestChecksumCalculation checksumValidation : checksumValidations) {
            for (UploadStreamingRegressionTesting.BodyType bodType : UploadStreamingRegressionTesting.BodyType.values()) {
                for (UploadStreamingRegressionTesting.ContentSize cs :
                    UploadStreamingRegressionTesting.ContentSize.values()) {
                    for (boolean ps : payloadSign) {
                        FlattenUploadConfig testConfig = new FlattenUploadConfig();
                        testConfig.setBucketType(BucketType.STANDARD_BUCKET);
                        testConfig.setRequestChecksumValidation(checksumValidation);
                        testConfig.setBodyType(bodType);
                        testConfig.setContentSize(cs);
                        testConfig.setPayloadSigning(ps);
                        configs.add(testConfig);
                    }
                }
            }
        }
        return configs;
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

    public UploadStreamingRegressionTesting.BodyType getBodyType() {
        return bodyType;
    }

    public void setBodyType(UploadStreamingRegressionTesting.BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public UploadStreamingRegressionTesting.ContentSize getContentSize() {
        return contentSize;
    }

    public void setContentSize(UploadStreamingRegressionTesting.ContentSize contentSize) {
        this.contentSize = contentSize;
    }

    public boolean isPayloadSigning() {
        return payloadSigning;
    }

    public void setPayloadSigning(boolean payloadSigning) {
        this.payloadSigning = payloadSigning;
    }

    @Override
    public String toString() {
        return ToString.builder("FlattenUploadConfig")
                       .add("bucketType", bucketType)
                       .add("forcePathStyle", forcePathStyle)
                       .add("requestChecksumValidation", requestChecksumValidation)
                       .add("bodyType", bodyType)
                       .add("contentSize", contentSize)
                       .add("payloadSigning", payloadSigning)
                       .build();
    }
}
