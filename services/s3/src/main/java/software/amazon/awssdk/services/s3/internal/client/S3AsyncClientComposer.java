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
import software.amazon.awssdk.awscore.internal.client.ClientComposer;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClient;
import software.amazon.awssdk.utils.ConditionalDecorator;

@SdkInternalApi
public class S3AsyncClientComposer implements ClientComposer<S3AsyncClient> {

    public S3AsyncClientComposer() {
    }

    @Override
    public S3AsyncClient compose(S3AsyncClient base, SdkClientConfiguration clientConfiguration) {
        List<ConditionalDecorator<S3AsyncClient>> decorators = new ArrayList<>();
        decorators.add(ConditionalDecorator.create(isCrossRegionEnabledAsync(clientConfiguration),
                                                   S3CrossRegionAsyncClient::new));
        return ConditionalDecorator.decorate(base, decorators);
    }

    private Predicate<S3AsyncClient> isCrossRegionEnabledAsync(SdkClientConfiguration clientConfiguration) {
        return client -> ((S3Configuration) clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION))
            .crossRegionAccessEnabled();
    }

}
