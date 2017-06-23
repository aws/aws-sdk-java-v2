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

package software.amazon.awssdk.client.builder;

import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

/**
 * Base class for all service specific sync client builders.
 *
 * @param <SubclassT>    Concrete builder type, used for better fluent methods.
 * @param <TypeToBuildT> Client interface this builder can build.
 */
@NotThreadSafe
@SdkProtectedApi
public abstract class AwsSyncClientBuilder<SubclassT extends AwsSyncClientBuilder, TypeToBuildT>
        extends AwsClientBuilder<SubclassT, TypeToBuildT> {
    protected AwsSyncClientBuilder() {
        super();
    }

    @SdkInternalApi
    protected AwsSyncClientBuilder(AwsRegionProvider regionProvider) {
        super(regionProvider);
    }


    @Override
    public final TypeToBuildT build() {
        return build(getSyncClientParams());
    }

    /**
     * Overriden by subclasses to call the client constructor.
     *
     * @param clientParams Client Params to create client with
     * @return Built client.
     */
    protected abstract TypeToBuildT build(AwsSyncClientParams clientParams);

}
