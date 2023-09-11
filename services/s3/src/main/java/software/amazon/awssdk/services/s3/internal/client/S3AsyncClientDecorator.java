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

package software.amazon.awssdk.services.s3.internal.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartS3AsyncClient;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.ConditionalDecorator;

@SdkInternalApi
public class S3AsyncClientDecorator {
    public static final AttributeMap.Key<MultipartConfiguration> MULTIPART_CONFIGURATION_KEY =
        new AttributeMap.Key<MultipartConfiguration>(MultipartConfiguration.class){};
    public static final AttributeMap.Key<Boolean> MULTIPART_ENABLED_KEY =
        new AttributeMap.Key<Boolean>(Boolean.class){};

    public S3AsyncClientDecorator() {
    }

    public S3AsyncClient decorate(S3AsyncClient base,
                                  SdkClientConfiguration clientConfiguration,
                                  AttributeMap clientContextParams) {
        List<ConditionalDecorator<S3AsyncClient>> decorators = new ArrayList<>();
        decorators.add(ConditionalDecorator.create(
            isCrossRegionEnabledAsync(clientContextParams),
            S3CrossRegionAsyncClient::new));

        decorators.add(ConditionalDecorator.create(
            isMultipartEnable(clientContextParams),
            client -> {
                MultipartConfiguration multipartConfiguration = clientContextParams.get(MULTIPART_CONFIGURATION_KEY);
                return MultipartS3AsyncClient.create(client, multipartConfiguration);
            }));
        return ConditionalDecorator.decorate(base, decorators);
    }

    private Predicate<S3AsyncClient> isCrossRegionEnabledAsync(AttributeMap clientContextParams) {
        Boolean crossRegionEnabled = clientContextParams.get(S3ClientContextParams.CROSS_REGION_ACCESS_ENABLED);
        return client -> crossRegionEnabled != null && crossRegionEnabled.booleanValue();
    }

    private Predicate<S3AsyncClient> isMultipartEnable(AttributeMap clientContextParams) {
        Boolean multipartEnabled = clientContextParams.get(MULTIPART_ENABLED_KEY);
        return client -> multipartEnabled != null && multipartEnabled.booleanValue();
    }
}
