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

package software.amazon.awssdk.core.protocol.traits;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.util.IdempotentUtils;

/**
 * Trait that supplies a default UUID if no value is set by the customer. This can only be applied to string members.
 */
@SdkProtectedApi
public final class IdempotencyTrait implements Trait {

    private IdempotencyTrait() {
    }

    public String resolveValue(String val) {
        if (val == null) {
            return IdempotentUtils.getGenerator().get();
        } else {
            return val;
        }
    }

    public static IdempotencyTrait create() {
        return new IdempotencyTrait();
    }
}
