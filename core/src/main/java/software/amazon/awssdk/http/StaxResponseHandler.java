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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.SdkStandardLoggers;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.async.AsyncResponseHandler;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.runtime.transform.StaxUnmarshallerContext;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.runtime.transform.VoidStaxUnmarshaller;
import software.amazon.awssdk.sync.StreamingResponseHandler;
import software.amazon.awssdk.util.StringUtils;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeFunction;
import software.amazon.awssdk.utils.Logger;

/**
 * Default implementation of HttpResponseHandler that handles a successful
 * response from an AWS service and unmarshalls the result using a StAX
 * unmarshaller.
 *
 * @param <T> Indicates the type being unmarshalled by this response handler.
 */
@SdkProtectedApi
@ReviewBeforeRelease("Metadata is currently broken. Revisit when base result types are refactored")
public class StaxResponseHandler<T> implements HttpResponseHandler<T> {
    private static final Logger log = Logger.loggerFor(StaxResponseHandler.class);

    /**
     * Shared factory for creating XML event readers.
     */
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    /**
     * The StAX unmarshaller to use when handling the response.
     */
    private Unmarshaller<T, StaxUnmarshallerContext> responseUnmarshaller;

    /**
     * Constructs a new response handler that will use the specified StAX
     * unmarshaller to unmarshall the service response and uses the specified
     * response element path to find the root of the business data in the
     * service's response.
     *
     * @param responseUnmarshaller The StAX unmarshaller to use on the response.
     */
    public StaxResponseHandler(Unmarshaller<T, StaxUnmarshallerContext> responseUnmarshaller) {
        this.responseUnmarshaller = responseUnmarshaller;

        /*
         * Even if the invoked operation just returns null, we still need an
         * unmarshaller to run so we can pull out response metadata.
         *
         * We might want to pass this in through the client class so that we
         * don't have to do this check here.
         */
        if (this.responseUnmarshaller == null) {
            this.responseUnmarshaller = new VoidStaxUnmarshaller<T>();
        }
    }


    /**
     * @see HttpResponseHandler#handle(HttpResponse, ExecutionAttributes)
     */
    public T handle(HttpResponse response, ExecutionAttributes executionAttributes) throws Exception {
        SdkStandardLoggers.REQUEST_LOGGER.trace(() -> "Parsing service response XML.");
        InputStream content = response.getContent();
        if (content == null) {
            content = new ByteArrayInputStream("<eof/>".getBytes(StringUtils.UTF8));
        }

        XMLEventReader eventReader;
        synchronized (XML_INPUT_FACTORY) {
            eventReader = XML_INPUT_FACTORY.createXMLEventReader(content);
        }

        try {
            StaxUnmarshallerContext unmarshallerContext = new StaxUnmarshallerContext(eventReader, response.getHeaders());
            unmarshallerContext.registerMetadataExpression("ResponseMetadata/RequestId", 2, ResponseMetadata.AWS_REQUEST_ID);
            unmarshallerContext.registerMetadataExpression("requestId", 2, ResponseMetadata.AWS_REQUEST_ID);
            registerAdditionalMetadataExpressions(unmarshallerContext);

            T result = responseUnmarshaller.unmarshall(unmarshallerContext);

            SdkStandardLoggers.REQUEST_LOGGER.trace(() -> "Done parsing service response.");

            return result;
        } finally {
            try {
                eventReader.close();
            } catch (XMLStreamException e) {
                log.warn(() -> "Error closing XML parser.", e);
            }
        }
    }

    /**
     * Create the default {@link ResponseMetadata}. Subclasses may override this to create a
     * subclass of {@link ResponseMetadata}. Currently only SimpleDB does this.
     */
    protected ResponseMetadata getResponseMetadata(Map<String, String> metadata) {
        return new ResponseMetadata(metadata);
    }

    /**
     * Hook for subclasses to override in order to collect additional metadata
     * from service responses.
     *
     * @param unmarshallerContext The unmarshaller context used to configure a service's response
     *                            data.
     */
    protected void registerAdditionalMetadataExpressions(StaxUnmarshallerContext unmarshallerContext) {
    }

    /**
     * Since this response handler completely consumes all the data from the
     * underlying HTTP connection during the handle method, we don't need to
     * keep the HTTP connection open.
     *
     * @see HttpResponseHandler#needsConnectionLeftOpen()
     */
    public boolean needsConnectionLeftOpen() {
        return false;
    }

    /**
     * Creates an synchronous {@link HttpResponseHandler} that unmarshalls into the response POJO while leaving the
     * connection open for further processing (by a {@link StreamingResponseHandler} or {@link AsyncResponseHandler}
     * for example).
     *
     * @param unmarshaller Unmarshaller for response POJO.
     * @param <ResponseT>  Response POJO type.
     */
    public static <ResponseT> HttpResponseHandler<ResponseT> createStreamingResponseHandler(
            Unmarshaller<ResponseT, StaxUnmarshallerContext> unmarshaller) {
        UnsafeFunction<HttpResponse, ResponseT> unmarshallFunction = response -> unmarshallStreaming(unmarshaller, response);
        return new HttpResponseHandler<ResponseT>() {
            @Override
            public ResponseT handle(HttpResponse response, ExecutionAttributes executionAttributes) throws Exception {
                return unmarshallFunction.apply(response);
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return true;
            }
        };
    }

    /**
     * Unmarshalls a streaming HTTP response into a POJO. Does not touch the content since that's consumed by the response
     * handler (either {@link StreamingResponseHandler} or {@link AsyncResponseHandler}).
     *
     * @param unmarshaller Unmarshaller for resposne type.
     * @param response     HTTP response
     * @param <ResponseT>  Response POJO Type.
     * @return Unmarshalled response type.
     * @throws Exception if error occurs during unmarshalling.
     */
    private static <ResponseT> ResponseT unmarshallStreaming(Unmarshaller<ResponseT, StaxUnmarshallerContext> unmarshaller,
                                                             HttpResponse response) throws Exception {
        // Create a dummy event reader to make unmarshallers happy
        XMLEventReader eventReader;
        synchronized (XML_INPUT_FACTORY) {
            eventReader = invokeSafely(() -> XML_INPUT_FACTORY
                    .createXMLEventReader(new ByteArrayInputStream("<eof/>".getBytes(StringUtils.UTF8))));
        }

        StaxUnmarshallerContext unmarshallerContext = new StaxUnmarshallerContext(eventReader, response.getHeaders());
        unmarshallerContext.registerMetadataExpression("ResponseMetadata/RequestId", 2, ResponseMetadata.AWS_REQUEST_ID);
        unmarshallerContext.registerMetadataExpression("requestId", 2, ResponseMetadata.AWS_REQUEST_ID);
        return unmarshaller.unmarshall(unmarshallerContext);
    }

}
