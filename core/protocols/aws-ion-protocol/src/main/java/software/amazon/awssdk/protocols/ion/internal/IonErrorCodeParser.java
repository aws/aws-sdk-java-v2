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

package software.amazon.awssdk.protocols.ion.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.ErrorCodeParser;
import software.amazon.awssdk.protocols.json.JsonContent;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonSystem;
import software.amazon.ion.IonType;

//TODO Clean up ION parser
@SdkInternalApi
class IonErrorCodeParser implements ErrorCodeParser {
    private static final Logger log = LoggerFactory.getLogger(IonErrorCodeParser.class);

    private static final String TYPE_PREFIX = "aws-type:";
    private static final String X_AMZN_REQUEST_ID_HEADER = "x-amzn-RequestId";

    private final IonSystem ionSystem;

    IonErrorCodeParser(IonSystem ionSystem) {
        this.ionSystem = ionSystem;
    }

    private static String getRequestId(SdkHttpFullResponse response) {
        return response.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER).orElse(null);
    }

    @Override
    public String parseErrorCode(SdkHttpFullResponse response, JsonContent jsonContents) {
        IonReader reader = ionSystem.newReader(jsonContents.getRawContent());
        try {
            IonType type = reader.next();
            if (type != IonType.STRUCT) {
                throw SdkClientException.builder()
                                        .message(String.format("Can only get error codes from structs (saw %s), request id %s",
                                                 type, getRequestId(response)))
                                        .build();
            }

            boolean errorCodeSeen = false;
            String errorCode = null;
            String[] annotations = reader.getTypeAnnotations();
            for (String annotation : annotations) {
                if (annotation.startsWith(TYPE_PREFIX)) {
                    if (errorCodeSeen) {
                        throw SdkClientException.builder()
                                                .message(String.format("Multiple error code annotations found for request id %s",
                                                                   getRequestId(response)))
                                                .build();
                    } else {
                        errorCodeSeen = true;
                        errorCode = annotation.substring(TYPE_PREFIX.length());
                    }
                }
            }

            return errorCode;
        } finally {
            IoUtils.closeQuietly(reader, log);
        }
    }
}
