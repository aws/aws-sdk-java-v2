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

package software.amazon.awssdk.core.internal.http.response;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.core.SdkBaseException;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.AwsExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.HttpStatusCodes;
import software.amazon.awssdk.http.HttpStatusFamily;

/**
 * Wrapper around protocol specific error handler to deal with some default scenarios and fill in common information.
 */
@SdkInternalApi
public class AwsErrorResponseHandler implements HttpResponseHandler<SdkBaseException> {

    private final HttpResponseHandler<? extends SdkBaseException> delegate;

    public AwsErrorResponseHandler(HttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {
        this.delegate = errorResponseHandler;
    }

    @Override
    public AmazonServiceException handle(HttpResponse response,
                                         ExecutionAttributes executionAttributes) throws Exception {
        final AmazonServiceException ase = (AmazonServiceException) handleAse(response, executionAttributes);
        ase.setStatusCode(response.getStatusCode());
        ase.setServiceName(executionAttributes.getAttribute(AwsExecutionAttributes.SERVICE_NAME));
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
            if (statusCode == HttpStatusCodes.REQUEST_TOO_LONG) {
                AmazonServiceException exception = new AmazonServiceException("Request entity too large");
                exception.setStatusCode(statusCode);
                exception.setErrorType(AmazonServiceException.ErrorType.Client);
                exception.setErrorCode("Request entity too large");
                return exception;
            } else if (HttpStatusFamily.of(statusCode) == HttpStatusFamily.SERVER_ERROR) {
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
