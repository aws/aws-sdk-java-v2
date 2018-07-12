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

import javax.xml.xpath.XPath;
import org.w3c.dom.Node;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.runtime.transform.AbstractErrorUnmarshaller;
import software.amazon.awssdk.core.util.xml.XpathUtils;

/**
 * Unmarshalls an AWS error response into an SdkServiceException, or
 * optionally, a subclass of SdkServiceException if this class is extended.
 */
@SdkProtectedApi
public final class LegacyErrorUnmarshaller extends AbstractErrorUnmarshaller<AwsServiceException, Node> {

    /**
     * Constructs a new unmarshaller that will unmarshall AWS error responses as
     * a generic SdkServiceException object.
     */
    public LegacyErrorUnmarshaller() {
        this(AwsServiceException.class);
    }

    /**
     * Constructor allowing subclasses to specify a specific type of
     * SdkServiceException to instantiating when populating the exception
     * object with data from the AWS error response.
     *
     * @param exceptionClass The class of SdkServiceException to create and populate
     * when unmarshalling the AWS error response.
     */
    public LegacyErrorUnmarshaller(Class<? extends AwsServiceException> exceptionClass) {
        super(exceptionClass);
    }

    @Override
    public AwsServiceException unmarshall(Node in) throws Exception {
        XPath xpath = XpathUtils.xpath();
        String errorCode = parseErrorCode(in, xpath);
        String message = XpathUtils.asString("Response/Errors/Error/Message", in, xpath);
        String requestId = XpathUtils.asString("Response/RequestID", in, xpath);

        AwsServiceException.Builder exception = newException(message).toBuilder();
        exception.requestId(requestId);

        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode(errorCode).build();
        exception.requestId(requestId).awsErrorDetails(awsErrorDetails);

        return exception.build();
    }

    /**
     * Returns the AWS error code for the specified error response.
     *
     * @param in The DOM tree node containing the error response.
     * @return The AWS error code contained in the specified error response.
     * @throws Exception If any problems were encountered pulling out the AWS error
     * code.
     */
    public String parseErrorCode(Node in) throws Exception {
        return XpathUtils.asString("Response/Errors/Error/Code", in);
    }

    public String parseErrorCode(Node in, XPath xpath) throws Exception {
        return XpathUtils.asString("Response/Errors/Error/Code", in, xpath);
    }

    /**
     * Returns the path to the specified property within an error response.
     *
     * @param property The name of the desired property.
     * @return The path to the specified property within an error message.
     */
    public String getErrorPropertyPath(String property) {
        return "Response/Errors/Error/" + property;
    }

}
