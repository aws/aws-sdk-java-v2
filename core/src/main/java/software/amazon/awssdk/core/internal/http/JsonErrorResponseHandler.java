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

package software.amazon.awssdk.core.internal.http;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.ErrorType;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.protocol.json.JsonContent;
import software.amazon.awssdk.core.internal.protocol.json.JsonErrorUnmarshaller;
import software.amazon.awssdk.http.HttpStatusFamily;

/**
 * Base error response handler for JSON protocol.
 */
@SdkInternalApi
public abstract class JsonErrorResponseHandler<ExceptionT extends SdkServiceException> implements
                                                                                       HttpResponseHandler<ExceptionT> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonErrorResponseHandler.class);

    protected ExceptionT safeUnmarshall(JsonContent jsonContent,
                                        JsonErrorUnmarshaller<ExceptionT> unmarshaller) {
        try {
            return unmarshaller.unmarshall(jsonContent.getJsonNode());
        } catch (Exception e) {
            LOG.info("Unable to unmarshall exception content", e);
            return null;
        }
    }

    /**
     * The default unmarshaller should always work but if it doesn't we fall back to creating an
     * exception explicitly.
     */
    protected abstract ExceptionT createUnknownException();

    protected ErrorType getErrorType(int statusCode) {
        return HttpStatusFamily.of(statusCode) == HttpStatusFamily.SERVER_ERROR ? ErrorType.SERVICE : ErrorType.CLIENT;
    }

    protected String getRequestIdFromHeaders(Map<String, String> headers) {
        return headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(X_AMZN_REQUEST_ID_HEADER))
                      .findFirst().map(Map.Entry::getValue).orElse(null);
    }
}
