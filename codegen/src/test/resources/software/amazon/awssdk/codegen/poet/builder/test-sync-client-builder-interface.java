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

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;

/**
 * A builder for creating an instance of {@link JsonClient}. This can be created with the static
 * {@link JsonClient#builder()} method.
 */
@Generated("software.amazon.awssdk:codegen")
public interface JsonClientBuilder extends AwsSyncClientBuilder<JsonClientBuilder, JsonClient>,
                                           JsonBaseClientBuilder<JsonClientBuilder, JsonClient> {
}
