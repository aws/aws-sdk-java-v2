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

package software.amazon.awssdk.http.auth.aws.internal.signer;


import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.AWS4_TERMINATOR;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.formatDate;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.formatDateTime;

import java.time.Instant;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

@SdkInternalApi
@Immutable
public final class CredentialScope {
    private final String region;
    private final String service;
    private final Instant instant;

    public CredentialScope(String region, String service, Instant instant) {
        this.region = region;
        this.service = service;
        this.instant = instant;
    }

    public String getRegion() {
        return region;
    }

    public String getService() {
        return service;
    }

    public Instant getInstant() {
        return instant;
    }

    public String getDate() {
        return formatDate(instant);
    }

    public String getDatetime() {
        return formatDateTime(instant);
    }

    public String scope() {
        return getDate() + "/" + region + "/" + service + "/" + AWS4_TERMINATOR;
    }

    public String scope(AwsCredentialsIdentity credentials) {
        return credentials.accessKeyId() + "/" + scope();
    }
}
