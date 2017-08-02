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

package software.amazon.awssdk.internal.http.response;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.handlers.AwsExecutionAttributes;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;

/**
 * Wrapper around protocol specific error handler to deal with some default scenarios and fill in common information.
 */
@SdkInternalApi
public class AwsErrorResponseHandler implements HttpResponseHandler<SdkBaseException> {

    private final HttpResponseHandler<? extends SdkBaseException> delegate;
    private final AwsRequestMetrics awsRequestMetrics;

    public AwsErrorResponseHandler(HttpResponseHandler<? extends SdkBaseException> errorResponseHandler,
                                   AwsRequestMetrics awsRequestMetrics) {
        this.delegate = errorResponseHandler;
        this.awsRequestMetrics = awsRequestMetrics;
    }

    @Override
    public AmazonServiceException handle(HttpResponse response,
                                         ExecutionAttributes executionAttributes) throws Exception {
        final AmazonServiceException ase = (AmazonServiceException) handleAse(response, executionAttributes);
        ase.setStatusCode(response.getStatusCode());
        ase.setServiceName(executionAttributes.getAttribute(AwsExecutionAttributes.SERVICE_NAME));
        awsRequestMetrics.addPropertyWith(AwsRequestMetrics.Field.AWSRequestID, ase.getRequestId())
                         .addPropertyWith(AwsRequestMetrics.Field.AWSErrorCode, ase.getErrorCode())
                         .addPropertyWith(AwsRequestMetrics.Field.StatusCode, ase.getStatusCode());
        return ase;
    }

    private SdkBaseException handleAse(HttpResponse response, ExecutionAttributes executionAttributes) throws Exception {
        final int statusCode = response.getStatusCode();
        try {
            return delegate.handle(response, executionAttributes);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            // If the errorResponseHandler doesn't work, then check for error responses that don't have any content
            if (statusCode == 413) {
                AmazonServiceException exception = new AmazonServiceException("Request entity too large");
                exception.setStatusCode(statusCode);
                exception.setErrorType(AmazonServiceException.ErrorType.Client);
                exception.setErrorCode("Request entity too large");
                return exception;
            } else if (statusCode >= 500 && statusCode < 600) {
                AmazonServiceException exception = new AmazonServiceException(response.getStatusText());
                exception.setStatusCode(statusCode);
                exception.setErrorType(AmazonServiceException.ErrorType.Service);
                exception.setErrorCode(response.getStatusText());
                return exception;
            } else {
                throw e;
            }
        }
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return delegate.needsConnectionLeftOpen();
    }
}
