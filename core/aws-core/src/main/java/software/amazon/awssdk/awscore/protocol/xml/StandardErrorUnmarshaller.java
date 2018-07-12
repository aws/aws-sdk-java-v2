/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.protocol.xml;

import static software.amazon.awssdk.core.util.XpathUtils.asString;
import static software.amazon.awssdk.core.util.XpathUtils.xpath;

import javax.xml.xpath.XPath;
import org.w3c.dom.Node;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.runtime.transform.AbstractErrorUnmarshaller;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

/**
 * Error unmarshaller that knows how to interpret a standard AWS error message
 * (i.e. where to find the AWS error code, the error message, etc.) and turn it
 * into an SdkServiceException.
 *
 * @see LegacyErrorUnmarshaller
 */
@SdkProtectedApi
public class StandardErrorUnmarshaller extends AbstractErrorUnmarshaller<AwsServiceException, Node> {

    /**
     * Constructs a new unmarshaller that will unmarshall a standard AWS error
     * message as a generic SdkServiceException object.
     */
    public StandardErrorUnmarshaller() {
        super(AwsServiceException.class);
    }

    /**
     * Constructor allowing subclasses to specify a specific type of
     * SdkServiceException to instantiating when populating the exception
     * object with data from the error message.
     *
     * @param exceptionClass
     *            The class of SdkServiceException to create and populate
     *            when unmarshalling the error message.
     */
    public StandardErrorUnmarshaller(Class<? extends AwsServiceException> exceptionClass) {
        super(exceptionClass);
    }

    /**
     * @see Unmarshaller#unmarshall(java.lang.Object)
     */
    @Override
    public AwsServiceException unmarshall(Node in) throws Exception {
        XPath xpath = xpath();
        String errorCode = parseErrorCode(in, xpath);

        return standardErrorPathException(errorCode, in, xpath);
    }

    /**
     * Returns the AWS error code for the specified error response.
     *
     * @param in
     *            The DOM tree node containing the error response.
     *
     * @return The AWS error code contained in the specified error response.
     *
     * @throws Exception
     *             If any problems were encountered pulling out the AWS error
     *             code.
     */
    public String parseErrorCode(Node in) throws Exception {
        return asString("ErrorResponse/Error/Code", in);
    }

    public String parseErrorCode(Node in, XPath xpath) throws Exception {
        return asString("ErrorResponse/Error/Code", in, xpath);
    }

    /**
     * Returns the path to the specified property within an error response.
     *
     * @param property
     *            The name of the desired property.
     *
     * @return The path to the specified property within an error message.
     */
    public String getErrorPropertyPath(String property) {
        return "ErrorResponse/Error/" + property;
    }

    public AwsServiceException standardErrorPathException(String errorCode, Node in, XPath xpath) throws Exception {

        String requestId = asString("ErrorResponse/RequestId", in, xpath);
        String message = asString("ErrorResponse/Error/Message", in, xpath);

        AwsServiceException.Builder exception = newException(message).toBuilder();

        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode(errorCode).errorMessage(message).build();
        exception.requestId(requestId);
        exception.awsErrorDetails(awsErrorDetails);

        return exception.build();
    }
}
