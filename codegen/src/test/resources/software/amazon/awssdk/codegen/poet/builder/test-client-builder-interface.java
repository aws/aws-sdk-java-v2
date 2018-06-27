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

package software.amazon.awssdk.services.json;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

/**
 * This includes configuration specific to Json Service that is supported by both {@link JsonClientBuilder} and
 * {@link JsonAsyncClientBuilder}.
 */
@Generated("software.amazon.awssdk:codegen")
public interface JsonBaseClientBuilder<B extends JsonBaseClientBuilder<B, C>, C> extends AwsClientBuilder<B, C> {
    B serviceConfiguration(ServiceConfiguration serviceConfiguration);

    default B serviceConfiguration(Consumer<ServiceConfiguration.Builder> serviceConfiguration) {
        return serviceConfiguration(ServiceConfiguration.builder().applyMutation(serviceConfiguration).build());
    }
}
