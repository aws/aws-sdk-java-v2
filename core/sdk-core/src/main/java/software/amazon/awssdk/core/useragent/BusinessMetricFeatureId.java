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

package software.amazon.awssdk.core.useragent;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * An enum class representing a short form of identity providers to record in the UA string.
 *
 * Unimplemented metrics: I,J,K,M,O,S,U-c
 * Unsupported metrics (these will never be added): A,H,x,y,1,2
 */
@SdkProtectedApi
public enum BusinessMetricFeatureId {

    WAITER("B"),
    PAGINATOR("C"),
    RETRY_MODE_LEGACY("D"),
    RETRY_MODE_STANDARD("E"),
    RETRY_MODE_ADAPTIVE("F"),
    S3_TRANSFER("G"),
    GZIP_REQUEST_COMPRESSION("L"), //TODO(metrics): Not working, compression happens after header
    ENDPOINT_OVERRIDE("N"),
    ACCOUNT_ID_MODE_PREFERRED("P"),
    ACCOUNT_ID_MODE_DISABLED("Q"),
    ACCOUNT_ID_MODE_REQUIRED("R"),
    RESOLVED_ACCOUNT_ID("T"),
    DDB_MAPPER("d"),
    CREDENTIALS_CODE("e"), //StaticCredentialsProvider
    CREDENTIALS_JVM_SYSTEM_PROPERTIES("f"), //SystemPropertyCredentialsProvider
    CREDENTIALS_ENV_VARS("g"), //EnvironmentVariableCredentialsProvider
    CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN("h"), //WebIdentityTokenFileCredentialsProvider
    CREDENTIALS_STS_ASSUME_ROLE("i"), //StsAssumeRoleCredentialsProvider
    CREDENTIALS_STS_ASSUME_ROLE_SAML("j"), //StsAssumeRoleWithSamlCredentialsProvider
    CREDENTIALS_STS_ASSUME_ROLE_WEB_ID("k"), //StsAssumeRoleWithWebIdentityCredentialsProvider
    CREDENTIALS_STS_FEDERATION_TOKEN("l"), //StsGetFederationTokenCredentialsProvider
    CREDENTIALS_STS_SESSION_TOKEN("m"), //StsGetSessionTokenCredentialsProvider
    CREDENTIALS_PROFILE("n"), // ProfileCredentialsProvider and static credentials/session credentials
    CREDENTIALS_PROFILE_SOURCE_PROFILE("o"), //ProfileCredentialsProvider + other providers
    CREDENTIALS_PROFILE_NAMED_PROVIDER("p"), //ProfileCredentialsProvider + InstanceProfile or ContainerCredentialsProvider
    CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN("q"), //ProfileCredentialsProvider + StsAssumeRoleWithWebIdentityCredentialsProvider
    CREDENTIALS_PROFILE_SSO("r"), //ProfileCredentialsProvider + SsoCredentialsProvider
    CREDENTIALS_SSO("s"), //SsoCredentialsProvider
    CREDENTIALS_PROFILE_SSO_LEGACY("t"), //ProfileCredentialsProvider + SsoCredentialsProvider
    CREDENTIALS_SSO_LEGACY("u"), //Not used, "CREDENTIALS_SSO" will always be applied. For legacy, look for "t,s"
    CREDENTIALS_PROFILE_PROCESS("v"), //ProfileCredentialsProvider + ProcessCredentialsProvider
    CREDENTIALS_PROCESS("w"), //ProcessCredentialsProvider
    CREDENTIALS_HTTP("z"), //ContainerCredentialsProvider
    CREDENTIALS_IMDS("0"), //InstanceProfileCredentialsProvider
    UNKNOWN("Unknown");

    private static final Map<String, BusinessMetricFeatureId> VALUE_MAP =
        EnumUtils.uniqueIndex(BusinessMetricFeatureId.class, BusinessMetricFeatureId::toString);
    private final String value;

    BusinessMetricFeatureId(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
