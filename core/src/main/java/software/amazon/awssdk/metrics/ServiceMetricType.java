/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics;

import software.amazon.awssdk.metrics.spi.MetricType;

/**
 * <a href=
 * "http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/software/amazon/awssdk/metrics/package-summary.html"
 * >Service specific Metric type</a> (eg S3, DynamoDB, etc.)
 */
public interface ServiceMetricType extends MetricType {
    String UPLOAD_THROUGHPUT_NAME_SUFFIX = "UploadThroughput";
    String UPLOAD_BYTE_COUNT_NAME_SUFFIX = "UploadByteCount";
    String DOWNLOAD_THROUGHPUT_NAME_SUFFIX = "DownloadThroughput";
    String DOWNLOAD_BYTE_COUNT_NAME_SUFFIX = "DownloadByteCount";

    String getServiceName();
}
