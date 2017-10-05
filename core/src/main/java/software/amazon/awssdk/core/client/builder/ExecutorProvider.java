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

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;

/**
 * A supplier for {@link ScheduledExecutorService} instances that should be used for async processing within the SDK. This
 * supplier is invoked each time {@link ClientBuilder#build()} is invoked, allowing for different executor services to be used
 * with each client instance. Executors produced by the supplier are managed by the SDK and will be shutdown when the service
 * client is closed.
 */
@FunctionalInterface
@ReviewBeforeRelease("We should standardize where we use 'provider', 'supplier', etc.\n" + "" +
                     "Also may be worthwhile to also allow customer to provide just a ScheduledExecutorService" +
                     "that may be shared and is managed by them.")
public interface ExecutorProvider extends Supplier<ScheduledExecutorService> {

}
