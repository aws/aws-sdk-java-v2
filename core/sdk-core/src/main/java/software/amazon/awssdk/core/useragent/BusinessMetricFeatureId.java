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
 * Unimplemented metrics: I,K
 * Unsupported metrics (these will never be added): A,H
 */
@SdkProtectedApi
public enum BusinessMetricFeatureId {

    WAITER("B"),
    PAGINATOR("C"),
    RETRY_MODE_LEGACY("D"),
    RETRY_MODE_STANDARD("E"),
    RETRY_MODE_ADAPTIVE("F"),
    S3_TRANSFER("G"),
    GZIP_REQUEST_COMPRESSION("L"),
    PROTOCOL_RPC_V2_CBOR("M"),
    ENDPOINT_OVERRIDE("N"),
    S3_EXPRESS_BUCKET("J"),
    ACCOUNT_ID_MODE_PREFERRED("P"),
    ACCOUNT_ID_MODE_DISABLED("Q"),
    ACCOUNT_ID_MODE_REQUIRED("R"),
    SIGV4A_SIGNING("S"),
    RESOLVED_ACCOUNT_ID("T"),
    FLEXIBLE_CHECKSUMS_REQ_CRC32("U"),
    FLEXIBLE_CHECKSUMS_REQ_CRC32C("V"),
    FLEXIBLE_CHECKSUMS_REQ_CRC64("W"),
    FLEXIBLE_CHECKSUMS_REQ_SHA1("X"),
    FLEXIBLE_CHECKSUMS_REQ_SHA256("Y"),
    FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED("Z"),
    FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED("a"),
    FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED("b"),
    FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED("c"),
    FLEXIBLE_CHECKSUMS_REQ_MD5("AE"),
    FLEXIBLE_CHECKSUMS_REQ_SHA512("AF"),
    FLEXIBLE_CHECKSUMS_REQ_XXHASH3("AG"),
    FLEXIBLE_CHECKSUMS_REQ_XXHASH64("AH"),
    FLEXIBLE_CHECKSUMS_REQ_XXHASH128("AI"),
    DDB_MAPPER("d"),
    BEARER_SERVICE_ENV_VARS("3"),
    CREDENTIALS_CODE("e"),
    CREDENTIALS_JVM_SYSTEM_PROPERTIES("f"),
    CREDENTIALS_ENV_VARS("g"),
    CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN("h"),
    CREDENTIALS_STS_ASSUME_ROLE("i"),
    CREDENTIALS_STS_ASSUME_ROLE_SAML("j"),
    CREDENTIALS_STS_ASSUME_ROLE_WEB_ID("k"),
    CREDENTIALS_STS_FEDERATION_TOKEN("l"),
    CREDENTIALS_STS_SESSION_TOKEN("m"),
    CREDENTIALS_PROFILE("n"),
    CREDENTIALS_PROFILE_SOURCE_PROFILE("o"),
    CREDENTIALS_PROFILE_NAMED_PROVIDER("p"),
    CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN("q"),
    CREDENTIALS_PROFILE_SSO("r"),
    CREDENTIALS_SSO("s"),
    CREDENTIALS_PROFILE_SSO_LEGACY("t"),
    CREDENTIALS_SSO_LEGACY("u"),
    CREDENTIALS_PROFILE_PROCESS("v"),
    CREDENTIALS_PROCESS("w"),
    CREDENTIALS_HTTP("z"),
    CREDENTIALS_IMDS("0"),
    CREDENTIALS_PROFILE_LOGIN("AC"),
    CREDENTIALS_LOGIN("AD"),
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
