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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import static software.amazon.awssdk.awscore.util.AwsHeader.AWS_REQUEST_ID;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public final class AwsXmlResponseHandler<T extends AwsResponse> implements HttpResponseHandler<T> {

    private static final Logger log = Logger.loggerFor(AwsXmlResponseHandler.class);

    private final XmlProtocolUnmarshaller<T> unmarshaller;
    private final Function<SdkHttpFullResponse, SdkPojo> pojoSupplier;
    private final boolean needsConnectionLeftOpen;

    public AwsXmlResponseHandler(XmlProtocolUnmarshaller<T> unmarshaller,
                                 Function<SdkHttpFullResponse, SdkPojo> pojoSupplier,
                                 boolean needsConnectionLeftOpen) {
        this.unmarshaller = unmarshaller;
        this.pojoSupplier = pojoSupplier;
        this.needsConnectionLeftOpen = needsConnectionLeftOpen;
    }

    @Override
    public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        try {
            return unmarshallResponse(response);
        } finally {
            if (!needsConnectionLeftOpen) {
                closeStream(response);
            }
        }
    }

    private void closeStream(SdkHttpFullResponse response) {
        response.content().ifPresent(i -> {
            try {
                i.close();
            } catch (IOException e) {
                log.warn(() -> "Error closing HTTP content.", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private T unmarshallResponse(SdkHttpFullResponse response) throws Exception {
        SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Parsing service response XML.");
        Pair<T, Map<String, String>> result = unmarshaller.unmarshall(pojoSupplier.apply(response), response);
        SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Done parsing service response.");
        AwsResponseMetadata responseMetadata = generateResponseMetadata(response, result.right());
        return (T) result.left().toBuilder().responseMetadata(responseMetadata).build();
    }

    /**
     * Create the default {@link AwsResponseMetadata}. Subclasses may override this to create a
     * subclass of {@link AwsResponseMetadata}.
     */
    private AwsResponseMetadata generateResponseMetadata(SdkHttpResponse response, Map<String, String> metadata) {
        if (!metadata.containsKey(AWS_REQUEST_ID)) {
            metadata.put(AWS_REQUEST_ID,
                         response.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER).orElse(null));
        }

        response.headers().forEach((key, value) -> metadata.put(key, value.get(0)));
        return DefaultAwsResponseMetadata.create(metadata);
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return needsConnectionLeftOpen;
    }
}
