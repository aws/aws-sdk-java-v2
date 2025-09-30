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
import software.amazon.awssdk.utils.CollectionUtils;

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

    public static boolean usesSigv4aAuth(IntermediateModel model) {
        if (isServiceSigv4a(model)) {
            return true;
        }
        return model.getOperations()
                    .values()
                    .stream()
                    .anyMatch(operationModel -> operationModel.getAuth().stream().anyMatch(authType -> authType == AuthType.V4A));
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
     * Returns {@code true} if and only if the operation should use bearer auth as the first preferred auth scheme.
     */
    public static boolean isOpBearerAuthPreferred(IntermediateModel model, OperationModel opModel) {
        return opModel.getAuthType() == AuthType.BEARER // single modeled auth on operation is bearer
            // auth array, first auth type is bearer
            || (opModel.getAuth() != null && !opModel.getAuth().isEmpty() && opModel.getAuth().get(0) == AuthType.BEARER)
            // service is only bearer and operation doesn't override
            || (model.getMetadata().getAuthType() == AuthType.BEARER && hasNoAuthType(opModel))
            // service is only bearer first and operation doesn't override
            || (model.getMetadata().getAuth() != null && !model.getMetadata().getAuth().isEmpty()
                && model.getMetadata().getAuth().get(0) == AuthType.BEARER && hasNoAuthType(opModel));
    }

    private static boolean isServiceBearerAuth(IntermediateModel model) {
        return model.getMetadata().getAuthType() == AuthType.BEARER ||
               (model.getMetadata().getAuth() != null && model.getMetadata().getAuth().contains(AuthType.BEARER));
    }

    private static boolean isServiceSigv4a(IntermediateModel model) {
        return model.getMetadata().getAuth().stream().anyMatch(authType -> authType == AuthType.V4A);
    }

    private static boolean isServiceAwsAuthType(IntermediateModel model) {
        AuthType authType = model.getMetadata().getAuthType();
        if (authType == null && !CollectionUtils.isNullOrEmpty(model.getMetadata().getAuth())) {
            return model.getMetadata().getAuth().stream()
                        .map(AuthType::value)
                        .map(AuthType::fromValue)
                        .anyMatch(AuthUtils::isAuthTypeAws);
        }
        return isAuthTypeAws(authType);
    }

    private static boolean isAuthTypeAws(AuthType authType) {
        if (authType == null) {
            return false;
        }

        switch (authType) {
            case V4A:
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
