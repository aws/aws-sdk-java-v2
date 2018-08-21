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
import java.util.Date;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sfn.builder.internal.DateModule;

@SdkInternalApi
abstract class BinaryTimestampConditionBuilder extends BinaryConditionBuilder {

    abstract BinaryTimestampConditionBuilder expectedValue(Date expectedValue);

    @Override
    final BinaryTimestampConditionBuilder expectedValue(JsonNode expectedValue) {
        expectedValue(DateModule.fromJson(expectedValue.asText()));
        return this;
    }
}
