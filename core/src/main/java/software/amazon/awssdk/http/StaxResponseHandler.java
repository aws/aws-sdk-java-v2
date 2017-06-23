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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.async.AsyncResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.async.UnmarshallingAsyncResponseHandler;
import software.amazon.awssdk.runtime.transform.StaxUnmarshallerContext;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.runtime.transform.UnmarshallingStreamingResponseHandler;
import software.amazon.awssdk.runtime.transform.VoidStaxUnmarshaller;
import software.amazon.awssdk.sync.StreamingResponseHandler;
import software.amazon.awssdk.util.StringUtils;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeFunction;

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

    /**
     * Shared logger for profiling information.
     */
    private static final Log log = LogFactory.getLog("software.amazon.awssdk.request");
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
     * @see HttpResponseHandler#handle(HttpResponse)
     */
    public T handle(HttpResponse response) throws Exception {
        log.trace("Parsing service response XML");
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

            return responseUnmarshaller.unmarshall(unmarshallerContext);
        } finally {
            try {
                eventReader.close();
            } catch (XMLStreamException e) {
                log.warn("Error closing xml parser", e);
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
     * Creates an synchronous {@link HttpResponseHandler} that unmarshalls the resposne POJO thne passes the HTTP content to the
     * given {@link StreamingResponseHandler}.
     *
     * @param unmarshaller     Unmarshaller for response POJO.
     * @param streamingHandler Customer provided response handler.
     * @param <ResponseT>      Response POJO type.
     * @param <ReturnT>        Return type of customer provided response handler.
     */
    public static <ResponseT, ReturnT> HttpResponseHandler<ReturnT> createStreamingResponseHandler(
            Unmarshaller<ResponseT, StaxUnmarshallerContext> unmarshaller,
            StreamingResponseHandler<ResponseT, ReturnT> streamingHandler) {
        UnsafeFunction<HttpResponse, ResponseT> unmarshallFunction = response -> unmarshallStreaming(unmarshaller, response);
        return new UnmarshallingStreamingResponseHandler<>(streamingHandler, unmarshallFunction);
    }

    /**
     * Creates an async {@link SdkHttpResponseHandler} from the given unmarshaller and customer provided {@link
     * AsyncResponseHandler}.
     *
     * @param unmarshaller         Unmarshaller for POJO response type.
     * @param asyncResponseHandler Customer provided response handler to consume HTTP content.
     * @param <ResponseT>          Response POJO type.
     * @param <ReturnT>            Return type of customer provided response handler.
     * @return SdkHttpResponseHandler that first unmarshalls the POJO and then provides the content.
     */
    public static <ResponseT, ReturnT> SdkHttpResponseHandler<ReturnT> createStreamingAsyncResponseHandler(
            Unmarshaller<ResponseT, StaxUnmarshallerContext> unmarshaller,
            AsyncResponseHandler<ResponseT, ReturnT> asyncResponseHandler) {
        return new UnmarshallingAsyncResponseHandler<>(asyncResponseHandler, sdkHttpResponse -> {
            HttpResponse httpResponse = SdkHttpResponseAdapter.adapt(false, null, (SdkHttpFullResponse) sdkHttpResponse);
            return unmarshallStreaming(unmarshaller, httpResponse);
        });
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
