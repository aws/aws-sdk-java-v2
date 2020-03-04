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
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@SdkPublicApi
public enum AttributeValueType {
    B(ScalarAttributeType.B),        // binary
    BOOL,                            // boolean
    BS,                              // binary set
    L,                               // list
    M,                               // documentMap
    N(ScalarAttributeType.N),        // number
    NS,                              // number set
    S(ScalarAttributeType.S),        // string
    SS,                              // string set
    NULL;                            // null

    private final ScalarAttributeType scalarAttributeType;

    AttributeValueType() {
        this.scalarAttributeType = null;
    }

    AttributeValueType(ScalarAttributeType scalarAttributeType) {
        this.scalarAttributeType = scalarAttributeType;
    }

    public ScalarAttributeType scalarAttributeType() {
        return scalarAttributeType;
    }
}
