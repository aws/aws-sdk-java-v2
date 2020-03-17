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

package software.amazon.awssdk.services.sts.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityTokenCredentialProperties;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.IdpCommunicationErrorException;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * An implementation of {@link WebIdentityTokenCredentialsProviderFactory} that allows users to assume a role
 * using a web identity token file specified in either a {@link Profile} or environment variables.
 */
@SdkProtectedApi
public final class StsWebIdentityCredentialsProviderFactory implements WebIdentityTokenCredentialsProviderFactory {

    public AwsCredentialsProvider create(WebIdentityTokenCredentialProperties credentialProperties) {
        return new StsWebIdentityCredentialsProvider(credentialProperties);
    }

    /**
     * A wrapper for a {@link StsAssumeRoleWithWebIdentityCredentialsProvider} that is returned by this factory when
     * {@link #create(WebIdentityTokenCredentialProperties)} is invoked. This wrapper is important because it ensures the parent
     * credentials provider is closed when the assume-role credentials provider is no longer needed.
     */
    private static final class StsWebIdentityCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
        private final StsClient stsClient;
        private final StsAssumeRoleWithWebIdentityCredentialsProvider credentialsProvider;

        private StsWebIdentityCredentialsProvider(WebIdentityTokenCredentialProperties credentialProperties) {
            String roleSessionName = credentialProperties.roleSessionName();
            String sessionName = roleSessionName != null ? roleSessionName : "aws-sdk-java-" + System.currentTimeMillis();

            OrRetryCondition retryCondition = OrRetryCondition.create(new StsRetryCondition(),
                                                                      RetryCondition.defaultRetryCondition());

            this.stsClient = StsClient.builder()
                                      .applyMutation(this::configureEndpoint)
                                      .credentialsProvider(AnonymousCredentialsProvider.create())
                                      .overrideConfiguration(o -> o.retryPolicy(r -> r.retryCondition(retryCondition)))
                                      .build();

            AssumeRoleWithWebIdentityRequest request = AssumeRoleWithWebIdentityRequest.builder()
                                                                                       .roleArn(credentialProperties.roleArn())
                                                                                       .roleSessionName(sessionName)
                                                                                       .build();

            AssumeRoleWithWebIdentityRequestSupplier supplier =
                new AssumeRoleWithWebIdentityRequestSupplier(request,
                                                             credentialProperties.webIdentityTokenFile());

            this.credentialsProvider =
                StsAssumeRoleWithWebIdentityCredentialsProvider.builder()
                                                               .stsClient(stsClient)
                                                               .refreshRequest(supplier)
                                                               .build();
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return this.credentialsProvider.resolveCredentials();
        }

        @Override
        public void close() {
            IoUtils.closeQuietly(credentialsProvider, null);
            IoUtils.closeQuietly(stsClient, null);
        }

        private void configureEndpoint(StsClientBuilder stsClientBuilder) {
            Region stsRegion;
            try {
                stsRegion = new DefaultAwsRegionProviderChain().getRegion();
            } catch (RuntimeException e) {
                stsRegion = null;
            }

            if (stsRegion != null) {
                stsClientBuilder.region(stsRegion);
            } else {
                stsClientBuilder.region(Region.US_EAST_1);
                stsClientBuilder.endpointOverride(URI.create("https://sts.amazonaws.com"));
            }
        }
    }

    private static final class AssumeRoleWithWebIdentityRequestSupplier implements Supplier {

        private final AssumeRoleWithWebIdentityRequest request;
        private final Path webIdentityTokenFile;

        AssumeRoleWithWebIdentityRequestSupplier(AssumeRoleWithWebIdentityRequest request,
                                                 Path webIdentityTokenFile) {
            this.request = request;
            this.webIdentityTokenFile = webIdentityTokenFile;
        }

        @Override
        public Object get() {
            return request.toBuilder().webIdentityToken(getToken(webIdentityTokenFile)).build();
        }

        private String getToken(Path file) {
            try (InputStream webIdentityTokenStream = Files.newInputStream(file)) {
                return IoUtils.toUtf8String(webIdentityTokenStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }


    private static final class StsRetryCondition implements RetryCondition {

        @Override
        public boolean shouldRetry(RetryPolicyContext context) {
            return context.exception() instanceof IdpCommunicationErrorException;
        }
    }
}
