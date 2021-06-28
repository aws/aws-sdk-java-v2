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

package software.amazon.awssdk.transfer.s3.internal;

import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.s3.CrtS3RuntimeException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;

@RunWith(MockitoJUnitRunner.class)
public class CrtErrorHandlerTest {

    @Mock
    private CrtS3RuntimeException mockCrtS3RuntimeException;

    @Test
    public void crtS3ExceptionAreTransformed(){
        CrtErrorHandler crtErrorHandler = new CrtErrorHandler();
        when(mockCrtS3RuntimeException.getAwsErrorCode()).thenReturn("BucketAlreadyExists");
        when(mockCrtS3RuntimeException.getAwsErrorMessage()).thenReturn("Bucket Already Exists");
        when(mockCrtS3RuntimeException.getStatusCode()).thenReturn(404);
        Exception transformException = crtErrorHandler.transformException(mockCrtS3RuntimeException);
        Assertions.assertThat(transformException).isInstanceOf(BucketAlreadyExistsException.class);
        Assertions.assertThat(transformException.getMessage()).contains("Bucket Already Exists");
    }

    @Test
    public void nonCrtS3ExceptionAreNotTransformed(){
        CrtErrorHandler crtErrorHandler = new CrtErrorHandler();
        Exception transformException = crtErrorHandler.transformException(new CrtRuntimeException("AWS_ERROR"));
        Assertions.assertThat(transformException).isInstanceOf(SdkClientException.class);
    }


    @Test
    public void crtS3ExceptionAreTransformedWhenExceptionIsInCause(){
        CrtErrorHandler crtErrorHandler = new CrtErrorHandler();
        when(mockCrtS3RuntimeException.getAwsErrorCode()).thenReturn("InvalidObjectState");
        when(mockCrtS3RuntimeException.getAwsErrorMessage()).thenReturn("Invalid Object State");
        when(mockCrtS3RuntimeException.getStatusCode()).thenReturn(404);
        final Exception transformException  = crtErrorHandler.transformException(new Exception("Some Exception", mockCrtS3RuntimeException));

        System.out.println("transformException " +transformException);

        Assertions.assertThat(transformException).isInstanceOf(InvalidObjectStateException.class);
        Assertions.assertThat(transformException.getMessage()).contains("Invalid Object State");
        Assertions.assertThat(transformException.getCause()).isInstanceOf(CrtS3RuntimeException.class);
    }

    @Test
    public void nonCrtS3ExceptionAreNotTransformedWhenExceptionIsInCause(){
        CrtErrorHandler crtErrorHandler = new CrtErrorHandler();
        final Exception crtRuntimeException = new Exception("Some Exception", new CrtRuntimeException("AWS_ERROR"));
        Exception transformException = crtErrorHandler.transformException(
                crtRuntimeException);
        Assertions.assertThat(transformException).isNotInstanceOf(CrtRuntimeException.class);
        Assertions.assertThat(transformException).isInstanceOf(SdkClientException.class);
        Assertions.assertThat(transformException.getMessage()).isEqualTo("Some Exception");
        Assertions.assertThat(transformException.getCause()).isEqualTo(crtRuntimeException);
    }

    @Test
    public void crtS3ExceptionWithErrorCodeNodeNotInS3Model() {
        CrtErrorHandler crtErrorHandler = new CrtErrorHandler();
        when(mockCrtS3RuntimeException.getAwsErrorCode()).thenReturn("NewS3ExceptionFromCrt");
        when(mockCrtS3RuntimeException.getAwsErrorMessage()).thenReturn("New S3 Exception From Crt");
        when(mockCrtS3RuntimeException.getStatusCode()).thenReturn(404);
        Exception transformException = crtErrorHandler.transformException(mockCrtS3RuntimeException);
        Assertions.assertThat(transformException).isInstanceOf(SdkServiceException.class);
        Assertions.assertThat(transformException.getCause()).isEqualTo(mockCrtS3RuntimeException);
        Assertions.assertThat(transformException.getMessage()).isEqualTo(mockCrtS3RuntimeException.getMessage());
        Assertions.assertThat(((SdkServiceException)transformException).statusCode())
                .isEqualTo(mockCrtS3RuntimeException.getStatusCode());
    }

    @Test
    public void crtS3ExceptionInCauseWithErrorCodeNodeNotInS3Model() {
        CrtErrorHandler crtErrorHandler = new CrtErrorHandler();
        when(mockCrtS3RuntimeException.getAwsErrorCode()).thenReturn("NewS3ExceptionFromCrt");
        when(mockCrtS3RuntimeException.getAwsErrorMessage()).thenReturn("New S3 Exception From Crt");
        when(mockCrtS3RuntimeException.getStatusCode()).thenReturn(404);
        final Exception crtRuntimeException = new Exception(mockCrtS3RuntimeException);
        Exception transformException = crtErrorHandler.transformException(crtRuntimeException);
        Assertions.assertThat(transformException).isInstanceOf(SdkServiceException.class);
        Assertions.assertThat(transformException.getCause()).isEqualTo(mockCrtS3RuntimeException);
        Assertions.assertThat(transformException.getMessage()).isEqualTo(mockCrtS3RuntimeException.getMessage());
        Assertions.assertThat(((SdkServiceException) transformException).statusCode()).isEqualTo(404);
    }
}