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

package software.amazon.awssdk.protocols.json.internal.dom;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Represents an explicit JSON null.
 */
@SdkInternalApi
public final class SdkNullNode implements SdkJsonNode {

    private static final SdkNullNode INSTANCE = new SdkNullNode();

    private SdkNullNode() {
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SdkNullNode;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "null";
    }

    static SdkNullNode instance() {
        return INSTANCE;
    }
}
