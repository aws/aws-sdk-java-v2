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

package software.amazon.awssdk.services.sfn.builder.conditions;

import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Base class for Binary String conditions. Handles marshalling a {@link JsonNode} into String.
 */
@SdkInternalApi
abstract class BinaryStringConditionBuilder extends BinaryConditionBuilder {

    abstract BinaryStringConditionBuilder expectedValue(String expectedValue);

    @Override
    final BinaryStringConditionBuilder expectedValue(JsonNode expectedValue) {
        expectedValue(expectedValue.asText());
        return this;
    }
}
