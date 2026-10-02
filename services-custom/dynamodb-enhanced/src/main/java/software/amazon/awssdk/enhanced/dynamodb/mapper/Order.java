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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import software.amazon.awssdk.annotations.SdkPublicApi;

@SdkPublicApi
public enum Order {
    UNSPECIFIED(-1),
    FIRST(0),
    SECOND(1),
    THIRD(2),
    FOURTH(3);

    private final int index;

    Order(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
