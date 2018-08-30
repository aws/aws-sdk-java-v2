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

import static software.amazon.awssdk.awscore.internal.DefaultAwsResponseMetadata.AWS_REQUEST_ID;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.internal.DefaultAwsResponseMetadata;
import software.amazon.awssdk.awscore.internal.protocol.xml.StaxOperationMetadata;
import software.amazon.awssdk.awscore.internal.protocol.xml.VoidStaxUnmarshaller;
import software.amazon.awssdk.awscore.protocol.xml.StaxUnmarshallerContext;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.XmlUtils;

/**
 * Default implementation of HttpResponseHandler that handles a successful
 * response from an AWS service and unmarshalls the result using a StAX
 * unmarshaller.
 *
 * @param <T> Indicates the type being unmarshalled by this response handler.
 */
@SdkProtectedApi
public final class StaxResponseHandler<T extends AwsResponse> implements HttpResponseHandler<T> {
    private static final Logger log = Logger.loggerFor(StaxResponseHandler.class);

    /**
     * The StAX unmarshaller to use when handling the response.
     */
    private Unmarshaller<T, StaxUnmarshallerContext> responseUnmarshaller;

    private StaxOperationMetadata operationMetadata;

    /**
     * Constructs a new response handler that will use the specified StAX
     * unmarshaller to unmarshall the service response and uses the specified
     * response element path to find the root of the business data in the
     * service's response.
     *
     * @param responseUnmarshaller The StAX unmarshaller to use on the response.
     */
    public StaxResponseHandler(Unmarshaller<T, StaxUnmarshallerContext> responseUnmarshaller,
                               StaxOperationMetadata operationMetadata) {
        this.responseUnmarshaller = responseUnmarshaller;
        this.operationMetadata = operationMetadata;

        /*
         * Even if the invoked operation just returns null, we still need an
         * unmarshaller to run so we can pull out response metadata.
         *
         * We might want to pass this in through the client class so that we
         * don't have to do this check here.
         */
        if (this.responseUnmarshaller == null) {
            this.responseUnmarshaller = new VoidStaxUnmarshaller<>();
        }
    }


    /**
     * @see HttpResponseHandler#handle(SdkHttpFullResponse, ExecutionAttributes)
     */
    @Override
    public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Parsing service response XML.");

        T result = unmarshalResponse(response, responseUnmarshaller);
        SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Done parsing service response.");
        return result;
    }

    /**
     * Create the default {@link AwsResponseMetadata}. Subclasses may override this to create a
     * subclass of {@link AwsResponseMetadata}.
     */
    private AwsResponseMetadata generateResponseMetadata(SdkHttpResponse response, StaxUnmarshallerContext
        unmarshallerContext) {
        Map<String, String> metadata = unmarshallerContext.getMetadata();

        if (!metadata.containsKey(AWS_REQUEST_ID)) {
            metadata.put(AWS_REQUEST_ID,
                         response.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER).orElse(null));
        }

        response.headers().forEach((key, value) -> metadata.put(key, value.get(0)));
        return DefaultAwsResponseMetadata.create(metadata);
    }

    /**
     * Since this response handler completely consumes all the data from the
     * underlying HTTP connection during the handle method, we don't need to
     * keep the HTTP connection open.
     *
     * @see HttpResponseHandler#needsConnectionLeftOpen()
     */
    @Override
    public boolean needsConnectionLeftOpen() {
        return operationMetadata.isHasStreamingSuccessResponse();
    }

    @SuppressWarnings("unchecked")
    private T unmarshalResponse(SdkHttpFullResponse response,
                                Unmarshaller<T, StaxUnmarshallerContext> unmarshaller) throws Exception {
        XMLEventReader eventReader = null;
        try {
            InputStream content;

            if (!response.content().isPresent() || operationMetadata.isHasStreamingSuccessResponse()) {
                content = AbortableInputStream.create(new ByteArrayInputStream("<eof/>".getBytes(StandardCharsets.UTF_8)));
            } else {
                content = response.content().get();
            }

            eventReader = XmlUtils.xmlInputFactory().createXMLEventReader(content);

            StaxUnmarshallerContext unmarshallerContext = new StaxUnmarshallerContext(eventReader, response.headers());
            unmarshallerContext.registerMetadataExpression("ResponseMetadata/RequestId", 2, AWS_REQUEST_ID);
            unmarshallerContext.registerMetadataExpression("requestId", 2, AWS_REQUEST_ID);

            T result = unmarshaller.unmarshall(unmarshallerContext);

            AwsResponseMetadata responseMetadata = generateResponseMetadata(response, unmarshallerContext);
            return (T) result.toBuilder().responseMetadata(responseMetadata).build();

        } finally {
            try {
                if (eventReader != null) {
                    eventReader.close();
                }
            } catch (XMLStreamException e) {
                log.warn(() -> "Error closing XML parser.", e);
            }
        }
    }

}
