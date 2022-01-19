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

package software.amazon.awssdk.codegen.utils;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.AuthType;

public final class BearerAuthUtils {
    private BearerAuthUtils() {
    }

    /**
     * Returns {@code true} if the service as a whole or any of its operations uses {@code bearer} auth type.
     */
    public static boolean usesBearerAuth(IntermediateModel model) {
        if (model.getMetadata().getAuthType() == AuthType.BEARER) {
            return true;
        }

        return model.getOperations().values().stream()
                    .map(OperationModel::getAuthType)
                    .anyMatch(authType -> authType == AuthType.BEARER);
    }
}
