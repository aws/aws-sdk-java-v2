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

public final class AuthUtils {
    private AuthUtils() {
    }

    /**
     * Returns {@code true} if the service as a whole or any of its operations uses {@code bearer} auth type.
     */
    public static boolean usesBearerAuth(IntermediateModel model) {
        if (isServiceBearerAuth(model)) {
            return true;
        }

        return model.getOperations().values().stream()
                    .map(OperationModel::getAuthType)
                    .anyMatch(authType -> authType == AuthType.BEARER);
    }

    public static boolean usesAwsAuth(IntermediateModel model) {
        if (isServiceAwsAuthType(model)) {
            return true;
        }

        return model.getOperations().values().stream()
                    .map(OperationModel::getAuthType)
                    .anyMatch(AuthUtils::isAuthTypeAws);
    }

    /**
     * Returns {@code true} if the operation should use bearer auth.
     */
    public static boolean isOpBearerAuth(IntermediateModel model, OperationModel opModel) {
        if (opModel.getAuthType() == AuthType.BEARER) {
            return true;
        }
        return isServiceBearerAuth(model) && hasNoAuthType(opModel);
    }

    private static boolean isServiceBearerAuth(IntermediateModel model) {
        return model.getMetadata().getAuthType() == AuthType.BEARER;
    }

    private static boolean isServiceAwsAuthType(IntermediateModel model) {
        AuthType authType = model.getMetadata().getAuthType();
        return isAuthTypeAws(authType);
    }

    private static boolean isAuthTypeAws(AuthType authType) {
        if (authType == null) {
            return false;
        }

        switch (authType) {
            case V4:
            case S3:
            case S3V4:
                return true;
            default:
                return false;
        }
    }

    private static boolean hasNoAuthType(OperationModel opModel) {
        return opModel.getAuthType() == null || opModel.getAuthType() == AuthType.NONE;
    }
}
