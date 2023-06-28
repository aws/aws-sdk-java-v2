/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.auth.scheme.internal;

import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.interceptor.BaseAuthSchemeInterceptor;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.acm.auth.scheme.AcmAuthSchemeParams;
import software.amazon.awssdk.services.acm.auth.scheme.AcmAuthSchemeProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class AcmAuthSchemeInterceptor extends BaseAuthSchemeInterceptor {

    @Override
    protected List<AuthSchemeOption> resolveAuthOptions(ExecutionAttributes executionAttributes) {
        // Prepare the inputs for the auth scheme resolver. We always include the
        // operationName, and we include the region if the service is modeled with
        // @sigv4.
        String operation = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
        // TODO: Do this only if sigv4
        Region region = executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION);

        AcmAuthSchemeProvider authSchemeProvider =
            (AcmAuthSchemeProvider) executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER);

        return authSchemeProvider.resolveAuthScheme(AcmAuthSchemeParams.builder()
                                                                       .operation(operation)
                                                                       .region(region)
                                                                       .build());
    }
}
