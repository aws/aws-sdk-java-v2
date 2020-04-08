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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import static software.amazon.awssdk.awscore.util.AwsHeader.AWS_REQUEST_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

/**
 * A transformer function that takes a parsed XML response and converts it into an {@link AwsResponse}. Used
 * as a component in the {@link AwsXmlPredicatedResponseHandler}.
 */
@SdkInternalApi
public final class AwsXmlResponseTransformer<T extends AwsResponse>
        implements Function<AwsXmlUnmarshallingContext, T> {

    private static final String X_AMZN_REQUEST_ID_HEADER = "x-amzn-RequestId";

    private final XmlProtocolUnmarshaller unmarshaller;
    private final Function<SdkHttpFullResponse, SdkPojo> pojoSupplier;

    public AwsXmlResponseTransformer(XmlProtocolUnmarshaller unmarshaller,
                                     Function<SdkHttpFullResponse, SdkPojo> pojoSupplier) {
        this.unmarshaller = unmarshaller;
        this.pojoSupplier = pojoSupplier;
    }

    @Override
    public T apply(AwsXmlUnmarshallingContext context) {
        return unmarshallResponse(context.sdkHttpFullResponse(), context.parsedRootXml());
    }

    @SuppressWarnings("unchecked")
    private T unmarshallResponse(SdkHttpFullResponse response, XmlElement parsedXml) {
        SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Unmarshalling parsed service response XML.");
        T result = unmarshaller.unmarshall(pojoSupplier.apply(response), parsedXml, response);
        SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Done unmarshalling parsed service response.");
        AwsResponseMetadata responseMetadata = generateResponseMetadata(response);
        return (T) result.toBuilder().responseMetadata(responseMetadata).build();
    }

    /**
     * Create the default {@link AwsResponseMetadata}. This might be wrapped by a service
     * specific metadata object to provide modeled access to additional metadata. (See S3 and Kinesis).
     */
    private AwsResponseMetadata generateResponseMetadata(SdkHttpResponse response) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(AWS_REQUEST_ID,
                     response.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER).orElse(null));

        response.headers().forEach((key, value) -> metadata.put(key, value.get(0)));
        return DefaultAwsResponseMetadata.create(metadata);
    }
}
