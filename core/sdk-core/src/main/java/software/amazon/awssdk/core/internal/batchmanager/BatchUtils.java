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

package software.amazon.awssdk.core.internal.batchmanager;

import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class BatchUtils {

    private BatchUtils() {
    }

    public static String getAndIncrementId(AtomicInteger id) {
        int currentId;
        int newCurrentId;
        do {
            currentId = id.get();
            newCurrentId = currentId + 1;
            if (newCurrentId == Integer.MAX_VALUE) {
                newCurrentId = 0;
            }
        } while (!id.compareAndSet(currentId, newCurrentId));
        return Integer.toString(currentId);
    }
}
