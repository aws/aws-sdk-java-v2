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

package software.amazon.awssdk.services.s3control.internal.interceptors;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.protocols.query.unmarshall.XmlDomParser;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;
import software.amazon.awssdk.services.s3control.model.InvalidRequestException;
import software.amazon.awssdk.services.s3control.model.S3ControlException;

/**
 * Translate S3 style exceptions, which have the Error tag at root instead of wrapped in ErrorResponse.
 * If the exception follows this structure but isn't known, create an S3ControlException with the
 * error code and message.
 */
@SdkInternalApi
public final class TopLevelXMLErrorInterceptor implements ExecutionInterceptor {

    private static final String XML_ERROR_ROOT = "Error";
    private static final String XML_ELEMENT_CODE = "Code";
    private static final String XML_ELEMENT_MESSAGE = "Message";

    private static final String INVALID_REQUEST_CODE = "InvalidRequest";

    @Override
    public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        S3ControlException exception = (S3ControlException) (context.exception());
        AwsErrorDetails awsErrorDetails = exception.awsErrorDetails();

        if (!(exception.getMessage().contains("null"))) {
            return context.exception();
        }

        XmlElement errorXml = XmlDomParser.parse(awsErrorDetails.rawResponse().asInputStream());
        if (!XML_ERROR_ROOT.equals(errorXml.elementName())) {
            return context.exception();
        }

        String errorCode = getChildElement(errorXml, XML_ELEMENT_CODE);
        String errorMessage = getChildElement(errorXml, XML_ELEMENT_MESSAGE);

        S3ControlException.Builder builder = findMatchingBuilder(errorCode);
        copyErrorDetails(exception, builder);
        return builder
            .message(errorMessage)
            .awsErrorDetails(copyAwsErrorDetails(awsErrorDetails, errorCode, errorMessage))
            .build();
    }

    private String getChildElement(XmlElement root, String elementName) {
        return root.getOptionalElementByName(elementName)
                   .map(XmlElement::textContent)
                   .orElse(null);
    }

    private S3ControlException.Builder findMatchingBuilder(String errorCode) {
        return INVALID_REQUEST_CODE.equals(errorCode) ?
               InvalidRequestException.builder() :
               S3ControlException.builder();
    }

    private void copyErrorDetails(S3ControlException exception, S3ControlException.Builder builder) {
        builder.cause(exception.getCause());
        builder.requestId(exception.requestId());
        builder.extendedRequestId(exception.extendedRequestId());
    }

    private AwsErrorDetails copyAwsErrorDetails(AwsErrorDetails original, String errorCode, String errorMessage) {
        return original.toBuilder()
                       .errorMessage(errorMessage)
                       .errorCode(errorCode)
                       .build();
    }
}
