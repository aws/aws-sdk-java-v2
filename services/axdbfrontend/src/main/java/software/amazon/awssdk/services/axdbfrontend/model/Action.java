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

package software.amazon.awssdk.services.axdbfrontend.model;

import java.io.Serializable;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Enumerations of possible actions that can be performed on an AxdbFrontend database.
 */
@SdkPublicApi
public enum Action implements Serializable {
    DB_CONNECT("DbConnect"),
    DB_CONNECT_SUPERUSER("DbConnectSuperuser");

    private final String action;

    Action(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public static Action variant(String value) {
        for (Action action : Action.values()) {
            if (value.equalsIgnoreCase(action.name())) {
                return action;
            }
        }
        throw new IllegalArgumentException("Invalid action: " + value);
    }
}