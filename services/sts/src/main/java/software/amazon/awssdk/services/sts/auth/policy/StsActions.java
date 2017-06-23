/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.sts.auth.policy;

import software.amazon.awssdk.auth.policy.Action;
import software.amazon.awssdk.auth.policy.Statement;
import software.amazon.awssdk.services.sts.STSClient;

/**
 * The available AWS access control policy actions for Amazon Security Token Service.
 *
 * @see Statement#setActions(java.util.Collection)
 * @deprecated in favor of {@link software.amazon.awssdk.auth.policy.actions.SecurityTokenServiceActions}
 */
@Deprecated
public enum StsActions implements Action {

    /**
     * Action for assuming role to do cross-account access or federation.
     *
     * @see STSClient#assumeRole(AssumeRoleRequest)
     */
    AssumeRole("sts:AssumeRole"),


    /**
     * Action for assuming role with web federation to get a set of temporary
     * security credentials for users who have been authenticated in a mobile or
     * web application with a web identity provider.
     *
     * @see STSClient#assumeRoleWithWebIdentity(AssumeRoleWithWebIdentityRequest)
     */
    AssumeRoleWithWebIdentity("sts:AssumeRoleWithWebIdentity");

    private final String action;

    private StsActions(String action) {
        this.action = action;
    }

    /* (non-Javadoc)
     * @see software.amazon.awssdk.auth.policy.Action#getId()
     */
    public String getActionName() {
        return this.action;
    }

}
