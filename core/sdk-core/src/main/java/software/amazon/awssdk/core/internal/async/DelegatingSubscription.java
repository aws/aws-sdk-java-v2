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

package software.amazon.awssdk.core.internal.async;

import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class DelegatingSubscription implements Subscription {

    private final Subscription s;

    private DelegatingSubscription(Subscription s) {
        this.s = s;
    }

    @Override
    public void request(long l) {
        s.request(l);
    }

    @Override
    public void cancel() {
        s.cancel();
    }

    public static DelegatingSubscription create(Subscription s) {
        return new DelegatingSubscription(s);
    }
}
