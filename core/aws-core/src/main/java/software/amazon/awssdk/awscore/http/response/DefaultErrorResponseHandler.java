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

package software.amazon.awssdk.awscore.http.response;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.core.util.xml.XpathUtils;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Implementation of HttpResponseHandler that handles only error responses from Amazon Web Services.
 * A list of unmarshallers is passed into the constructor, and while handling a response, each
 * unmarshaller is tried, in order, until one is found that can successfully unmarshall the error
 * response.  If no unmarshaller is found that can unmarshall the error response, a generic
 * SdkServiceException is created and populated with the AWS error response information (error
 * message, AWS error code, AWS request ID, etc).
 */
@SdkProtectedApi
public final class DefaultErrorResponseHandler implements HttpResponseHandler<AwsServiceException> {
    private static final Logger log = LoggerFactory.getLogger(DefaultErrorResponseHandler.class);

    /**
     * The list of error response unmarshallers to try to apply to error responses.
     */
    private List<Unmarshaller<AwsServiceException, Node>> unmarshallerList;

    /**
     * Constructs a new DefaultErrorResponseHandler that will handle error responses from Amazon
     * services using the specified list of unmarshallers. Each unmarshaller will be tried, in
     * order, until one is found that can unmarshall the error response.
     *
     * @param unmarshallerList The list of unmarshallers to try using when handling an error
     *                         response.
     */
    public DefaultErrorResponseHandler(
            List<Unmarshaller<AwsServiceException, Node>> unmarshallerList) {
        this.unmarshallerList = unmarshallerList;
    }

    @Override
    public AwsServiceException handle(SdkHttpFullResponse errorResponse,
                                      ExecutionAttributes executionAttributes) throws Exception {
        AwsServiceException exception = createServiceException(errorResponse);

        if (exception == null) {
            throw SdkClientException.builder().message("Unable to unmarshall error response from service").build();
        }

        AwsServiceException.Builder exceptionBuilder = exception.toBuilder();

        AwsErrorDetails.Builder awsErrorDetails =
                exceptionBuilder.awsErrorDetails()
                                .toBuilder()
                                .sdkHttpResponse(errorResponse)
                                .serviceName(executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME));

        if (awsErrorDetails.errorCode() == null) {
            awsErrorDetails.errorCode(errorResponse.statusCode() + " " + errorResponse.statusText().orElse(null));
        }

        return exceptionBuilder.awsErrorDetails(awsErrorDetails.build()).build();
    }

    private AwsServiceException createServiceException(SdkHttpFullResponse errorResponse) throws Exception {

        // Try to parse the error response as XML
        final Document document = documentFromContent(errorResponse.content().orElse(null), idString(errorResponse));

        /*
         * We need to select which exception unmarshaller is the correct one to
         * use from all the possible exceptions this operation can throw.
         * Currently we rely on the unmarshallers to return null if they can't
         * unmarshall the response, but we might need something a little more
         * sophisticated in the future.
         */
        for (Unmarshaller<AwsServiceException, Node> unmarshaller : unmarshallerList) {
            AwsServiceException exception = unmarshaller.unmarshall(document);

            if (exception != null) {
                return exception.toBuilder().statusCode(errorResponse.statusCode()).build();
            }
        }
        return null;
    }

    private Document documentFromContent(InputStream content, String idString)
            throws ParserConfigurationException, SAXException, IOException {
        try {
            return parseXml(contentToString(content, idString), idString);
        } catch (Exception e) {
            // Generate an empty document to make the unmarshallers happy. Ultimately the default
            // unmarshaller will be called to unmarshall into the service base exception.
            return XpathUtils.documentFrom("<empty/>");
        }
    }

    private String contentToString(InputStream content, String idString) throws Exception {
        try {
            return IoUtils.toUtf8String(content);
        } catch (Exception e) {
            log.debug(String.format("Unable to read input stream to string (%s)", idString), e);
            throw e;
        }
    }

    private Document parseXml(String xml, String idString) throws Exception {
        try {
            return XpathUtils.documentFrom(xml);
        } catch (Exception e) {
            log.debug("Unable to parse HTTP response ({}) content to XML document '{}' ", idString, xml, e);
            throw e;
        }
    }

    private String idString(SdkHttpFullResponse errorResponse) {
        StringBuilder idString = new StringBuilder();
        try {
            errorResponse.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                         .ifPresent(s -> idString.append("Request Id:").append(s));

        } catch (NullPointerException npe) {
            log.debug("Error getting Request or Invocation ID from response", npe);
        }
        return idString.length() > 0 ? idString.toString() : "Unknown";
    }

    /**
     * Since this response handler completely consumes all the data from the underlying HTTP
     * connection during the handle method, we don't need to keep the HTTP connection open.
     *
     * @see HttpResponseHandler#needsConnectionLeftOpen()
     */
    @Override
    public boolean needsConnectionLeftOpen() {
        return false;
    }
}
