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

package software.amazon.awssdk.protocols.query.internal.unmarshall;

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
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Response handler for AWS/Query services and Amazon EC2 which is a dialect of the Query protocol.
 *
 * @param <T> Indicates the type being unmarshalled by this response handler.
 */
@SdkInternalApi
public final class AwsQueryResponseHandler<T extends AwsResponse> implements HttpResponseHandler<T> {

    private static final Logger log = Logger.loggerFor(AwsQueryResponseHandler.class);

    private final QueryProtocolUnmarshaller unmarshaller;
    private final Function<SdkHttpFullResponse, SdkPojo> pojoSupplier;


    public AwsQueryResponseHandler(QueryProtocolUnmarshaller unmarshaller,
                                   Function<SdkHttpFullResponse, SdkPojo> pojoSupplier) {
        this.unmarshaller = unmarshaller;
        this.pojoSupplier = pojoSupplier;
    }

    @Override
    public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        try {
            return unmarshallResponse(response);
        } finally {
            response.content().ifPresent(i -> {
                try {
                    i.close();
                } catch (IOException e) {
                    log.warn(() -> "Error closing HTTP content.", e);
                }
            });
        }
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
     * Create the default {@link AwsResponseMetadata}. This might be wrapped by a service
     * specific metadata object to provide modeled access to additional metadata. (See S3 and Kinesis).
     */
    private AwsResponseMetadata generateResponseMetadata(SdkHttpResponse response, Map<String, String> metadata) {
        if (!metadata.containsKey(AWS_REQUEST_ID)) {
            metadata.put(AWS_REQUEST_ID,
                         SdkHttpUtils.firstMatchingHeaderFromCollection(response.headers(), X_AMZN_REQUEST_ID_HEADERS)
                                     .orElse(null));
        }

        response.headers().forEach((key, value) -> metadata.put(key, value.get(0)));
        return DefaultAwsResponseMetadata.create(metadata);
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        // Query doesn't support streaming so this is always false
        return false;
    }

}
