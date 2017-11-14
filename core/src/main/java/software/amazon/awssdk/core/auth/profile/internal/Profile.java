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

package software.amazon.awssdk.core.auth.profile.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.auth.AwsSessionCredentials;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.auth.profile.internal.securitytoken.RoleInfo;

/**
 * Contains the information stored in an AWS profile, such as AWS security
 * credentials.
 */
@Immutable
@Deprecated
public class Profile {

    /** The name of this profile. */
    private final String profileName;

    /** Profile properties. */
    private final Map<String, String> properties;

    /** Holds the AWS Credentials for the profile. */
    private final AwsCredentialsProvider awsCredentials;

    public Profile(String profileName, AwsCredentials awsCredentials) {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put(ProfileKeyConstants.AWS_ACCESS_KEY_ID, awsCredentials.accessKeyId());
        properties.put(ProfileKeyConstants.AWS_SECRET_ACCESS_KEY, awsCredentials.secretAccessKey());

        if (awsCredentials instanceof AwsSessionCredentials) {
            AwsSessionCredentials sessionCred = (AwsSessionCredentials) awsCredentials;
            properties.put(ProfileKeyConstants.AWS_SESSION_TOKEN, sessionCred.sessionToken());
        }

        this.profileName = profileName;
        this.properties = properties;
        this.awsCredentials = StaticCredentialsProvider.create(awsCredentials);
    }

    public Profile(String profileName, String sourceProfile, AwsCredentialsProvider awsCredentials, RoleInfo roleInfo) {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put(ProfileKeyConstants.SOURCE_PROFILE, sourceProfile);
        properties.put(ProfileKeyConstants.ROLE_ARN, roleInfo.getRoleArn());

        if (roleInfo.getRoleSessionName() != null) {
            properties.put(ProfileKeyConstants.ROLE_SESSION_NAME, roleInfo.getRoleSessionName());
        }

        if (roleInfo.getExternalId() != null) {
            properties.put(ProfileKeyConstants.EXTERNAL_ID, roleInfo.getExternalId());
        }

        this.profileName = profileName;
        this.properties = properties;
        this.awsCredentials = awsCredentials;
    }

    public Profile(String profileName, Map<String, String> properties,
                   AwsCredentialsProvider awsCredentials) {
        this.profileName = profileName;
        this.properties = properties;
        this.awsCredentials = awsCredentials;
    }

    public String getProfileName() {
        return profileName;
    }

    public AwsCredentials getCredentials() {
        return awsCredentials.getCredentials();
    }

    /**
     * Returns a map of profile properties included in this Profile instance.
     * The returned properties corresponds to how this profile is described in
     * the credential profiles file, i.e., profiles with basic credentials
     * consist of two properties {"aws_access_key_id", "aws_secret_access_key"}
     * and profiles with session credentials have three properties, with an
     * additional "aws_session_token" property.
     */
    public Map<String, String> getProperties() {
        return new LinkedHashMap<String, String>(properties);
    }

    /**
     * Returns the value of a specific property that is included in this Profile instance.
     * @see Profile#getProperties()
     */
    public String getPropertyValue(String propertyName) {
        return getProperties().get(propertyName);
    }
}
