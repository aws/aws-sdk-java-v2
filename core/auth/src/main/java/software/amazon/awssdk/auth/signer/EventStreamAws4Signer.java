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

package software.amazon.awssdk.auth.signer;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.signer.internal.BaseEventStreamAsyncAws4Signer;

@SdkProtectedApi
public final class EventStreamAws4Signer extends BaseEventStreamAsyncAws4Signer {
    private EventStreamAws4Signer() {
    }

    public static EventStreamAws4Signer create() {
        return new EventStreamAws4Signer();
    }
}
