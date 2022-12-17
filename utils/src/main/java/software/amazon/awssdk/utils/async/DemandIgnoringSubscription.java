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

package software.amazon.awssdk.utils.async;

import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class DemandIgnoringSubscription implements Subscription {

    private final Subscription delegate;

    public DemandIgnoringSubscription(Subscription delegate) {
        this.delegate = delegate;
    }

    @Override
    public void request(long n) {
        // Ignore demand requests from downstream, they want too much.
        // We feed them the amount that we want.
    }

    @Override
    public void cancel() {
        delegate.cancel();
    }
}
