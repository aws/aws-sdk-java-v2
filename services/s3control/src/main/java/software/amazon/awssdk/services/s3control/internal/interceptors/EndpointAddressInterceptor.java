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

package software.amazon.awssdk.services.s3control.internal.interceptors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3control.S3ControlConfiguration;

/**
 * Execution interceptor which modifies the HTTP request to S3 Control to
 * change the endpoint to the correct endpoint. This includes prefixing the AWS
 * account identifier and, when enabled, adding in FIPS and dualstack.
 */
@SdkInternalApi
public class EndpointAddressInterceptor implements ExecutionInterceptor {
    private static final Pattern HOSTNAME_COMPLIANT_PATTERN = Pattern.compile("[A-Za-z0-9\\-]+");
    private static final int HOSTNAME_MAX_LENGTH = 63;

    private static final String ENDPOINT_PREFIX = "s3-control";

    private static final String X_AMZ_ACCOUNT_ID = "x-amz-account-id";

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {
        SdkHttpRequest request = context.httpRequest();

        if (!request.headers().containsKey(X_AMZ_ACCOUNT_ID)) {
            throw SdkClientException.create("Account ID must be specified for all requests");
        }

        String accountId = request.headers().get(X_AMZ_ACCOUNT_ID).get(0);

        S3ControlConfiguration config = (S3ControlConfiguration) executionAttributes.getAttribute(
            AwsSignerExecutionAttribute.SERVICE_CONFIG);

        String host = resolveHost(request, accountId, config);

        return request.toBuilder()
                      .host(host)
                      .build();
    }

    private String resolveHost(SdkHttpRequest request, String accountId, S3ControlConfiguration configuration) {
        if (isDualstackEnabled(configuration) && isFipsEnabled(configuration)) {
            throw SdkClientException.create("Cannot use both Dual-Stack endpoints and FIPS endpoints");
        }
        String host = request.getUri().getHost();
        if (isDualstackEnabled(configuration)) {
            if (!host.contains(ENDPOINT_PREFIX)) {
                throw SdkClientException.create(String.format("The Dual-Stack option cannot be used with custom endpoints (%s)",
                                                              request.getUri()));
            }
            host = host.replace(ENDPOINT_PREFIX, String.format("%s.%s", ENDPOINT_PREFIX, "dualstack"));
        } else if (isFipsEnabled(configuration)) {
            if (!host.contains(ENDPOINT_PREFIX)) {
                throw SdkClientException.create(String.format("The FIPS option cannot be used with custom endpoints (%s)",
                                                              request.getUri()));
            }
            host = host.replace(ENDPOINT_PREFIX, String.format("%s-%s", ENDPOINT_PREFIX, "fips"));

        }
        validateComponentIsHostnameCompliant(accountId, "account id");
        return String.format("%s.%s", accountId, host);
    }

    private boolean isDualstackEnabled(S3ControlConfiguration configuration) {
        return configuration != null && configuration.dualstackEnabled();
    }

    private boolean isFipsEnabled(S3ControlConfiguration configuration) {
        return configuration != null && configuration.fipsModeEnabled();
    }

    private static void validateComponentIsHostnameCompliant(String component, String componentName) {
        if (component.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("An argument has been passed that is not valid: the required '%s' "
                              + "component is missing.", componentName));
        }

        if (component.length() > HOSTNAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                String.format("An argument has been passed that is not valid: the '%s' "
                              + "component exceeds the maximum length of %d characters.", componentName,
                              HOSTNAME_MAX_LENGTH));
        }

        Matcher m = HOSTNAME_COMPLIANT_PATTERN.matcher(component);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                String.format("An argument has been passed that is not valid: the '%s' "
                              + "component must only contain alphanumeric characters and dashes.", componentName));
        }
    }
}
