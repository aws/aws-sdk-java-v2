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

package software.amazon.awssdk.services.s3.utils;

import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

public class CaptureChecksumValidationInterceptor implements ExecutionInterceptor {
    private   Algorithm validationAlgorithm;
    private  ChecksumValidation responseValidation;
    private  String requestChecksumInTrailer;
    private  String requestChecksumInHeader;
    private  String contentEncoding;

    public String contentEncoding() {
        return contentEncoding;
    }

    public Algorithm validationAlgorithm() {
        return validationAlgorithm;
    }

    public ChecksumValidation responseValidation() {
        return responseValidation;
    }

    public String requestChecksumInTrailer() {
        return requestChecksumInTrailer;
    }

    public String requestChecksumInHeader() {
        return requestChecksumInHeader;
    }
    public void reset() {
        validationAlgorithm = null;
        responseValidation = null;
        requestChecksumInTrailer = null;
        requestChecksumInHeader = null;

    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        requestChecksumInTrailer =
            context.httpRequest().firstMatchingHeader("x-amz-trailer").orElse(null);
        requestChecksumInHeader =
            context.httpRequest().firstMatchingHeader("x-amz-checksum-crc32").orElse(null);}

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        validationAlgorithm =
            executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_CHECKSUM_VALIDATION_ALGORITHM).orElse(null);
        responseValidation =
            executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION).orElse(null);
        contentEncoding = String.join(",", context.httpRequest().matchingHeaders("content-encoding"));
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        validationAlgorithm =
            executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_CHECKSUM_VALIDATION_ALGORITHM).orElse(null);
        responseValidation =
            executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION).orElse(null);
    }
}