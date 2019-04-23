/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.profiles;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The properties used by the Java SDK from the credentials and config files.
 *
 * @see ProfileFile
 */
@SdkPublicApi
public final class ProfileProperty {
    /**
     * Property name for specifying the Amazon AWS Access Key
     */
    public static final String AWS_ACCESS_KEY_ID = "aws_access_key_id";

    /**
     * Property name for specifying the Amazon AWS Secret Access Key
     */
    public static final String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";

    /**
     * Property name for specifying the Amazon AWS Session Token
     */
    public static final String AWS_SESSION_TOKEN = "aws_session_token";

    /**
     * Property name for specifying the IAM role to assume
     */
    public static final String ROLE_ARN = "role_arn";

    /**
     * Property name for specifying the IAM role session name
     */
    public static final String ROLE_SESSION_NAME = "role_session_name";

    /**
     * Property name for specifying the IAM role external id
     */
    public static final String EXTERNAL_ID = "external_id";

    /**
     * Property name for specifying the profile credentials to use when assuming a role
     */
    public static final String SOURCE_PROFILE = "source_profile";

    /**
     * Property name for specifying the credential source to use when assuming a role
     */
    public static final String CREDENTIAL_SOURCE = "credential_source";

    /**
     * AWS Region to use when creating clients.
     */
    public static final String REGION = "region";

    /**
     * Property name for specifying the identification number of the MFA device
     */
    public static final String MFA_SERIAL = "mfa_serial";

    /**
     * Property name for specifying whether or not endpoint discovery is enabled.
     */
    public static final String ENDPOINT_DISCOVERY_ENABLED = "aws_endpoint_discovery_enabled";

    /**
     * An external process that should be invoked to load credentials.
     */
    public static final String CREDENTIAL_PROCESS = "credential_process";

    private ProfileProperty() {}
}
