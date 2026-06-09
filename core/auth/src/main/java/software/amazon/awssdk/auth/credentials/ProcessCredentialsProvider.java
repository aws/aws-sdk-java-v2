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

package software.amazon.awssdk.auth.credentials;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Platform;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * A credentials provider that can load credentials from an external process. This is used to support the credential_process
 * setting in the profile credentials file. See
 * <a href="https://docs.aws.amazon.com/cli/latest/topic/config-vars.html#sourcing-credentials-from-external-processes">sourcing credentials
 * from external processes</a> for more information.
 *
 * <p>
 * This class can be initialized using {@link #builder()}.
 *
 * <p>
 * Available settings:
 * <ul>
 *     <li>Command - The command that should be executed to retrieve credentials.</li>
 *     <li>CredentialRefreshThreshold - <b>Deprecated.</b> Previously configured the amount of time between when the
 *     credentials expire and when the credentials should start to be refreshed. The provider now uses a default prefetch time
 *     of 5 minutes before expiry and a stale time of 1 minute before expiry. If explicitly set, the value is honored as the
 *     prefetch time for backward compatibility.</li>
 *     <li>ProcessOutputLimit - The maximum amount of data that can be returned by the external process before an exception is
 *     raised. Default: 64000 bytes (64KB).</li>
 * </ul>
 */
@SdkPublicApi
public final class ProcessCredentialsProvider
    implements AwsCredentialsProvider,
               SdkAutoCloseable,
               ToCopyableBuilder<ProcessCredentialsProvider.Builder, ProcessCredentialsProvider> {
    private static final String CLASS_NAME = "ProcessCredentialsProvider";
    private static final String PROVIDER_NAME = BusinessMetricFeatureId.CREDENTIALS_PROCESS.value();
    private static final JsonNodeParser PARSER = JsonNodeParser.builder()
                                                               .removeErrorLocations(true)
                                                               .build();
    private static final Duration PROCESS_STALE_TIME = Duration.ofMinutes(1);
    private static final Duration PROCESS_PREFETCH_TIME = Duration.ofMinutes(5);

    private final List<String> executableCommand;
    private final Duration credentialRefreshThreshold;
    private final boolean credentialRefreshThresholdExplicitlySet;
    private final long processOutputLimit;
    private final String staticAccountId;

    private final CachedSupplier<AwsCredentials> processCredentialCache;

    private final String commandFromBuilder;

    private final List<String> commandAsListOfStringsFromBuilder;

    private final Boolean asyncCredentialUpdateEnabled;

    private final String sourceChain;
    private final String providerName;
    private final Duration staleTime;
    private final Duration prefetchTime;

    /**
     * @see #builder()
     */
    private ProcessCredentialsProvider(Builder builder) {
        this.executableCommand = executableCommand(builder);
        this.processOutputLimit = Validate.isPositive(builder.processOutputLimit, "processOutputLimit");
        this.credentialRefreshThreshold = Validate.isPositive(builder.credentialRefreshThreshold, "expirationBuffer");
        this.credentialRefreshThresholdExplicitlySet = builder.credentialRefreshThresholdExplicitlySet;
        this.commandFromBuilder = builder.command;
        this.commandAsListOfStringsFromBuilder = builder.commandAsListOfStrings;
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.staticAccountId = builder.staticAccountId;
        this.sourceChain = builder.sourceChain;
        this.providerName = StringUtils.isEmpty(builder.sourceChain)
            ? PROVIDER_NAME 
            : builder.sourceChain + "," + PROVIDER_NAME;
        this.staleTime = Optional.ofNullable(builder.staleTime).orElse(PROCESS_STALE_TIME);
        this.prefetchTime = Optional.ofNullable(builder.prefetchTime).orElse(PROCESS_PREFETCH_TIME);
        Validate.isTrue(this.staleTime.compareTo(this.prefetchTime) < 0,
                        "staleTime (%s) must be less than prefetchTime (%s).", this.staleTime, this.prefetchTime);

        CachedSupplier.Builder<AwsCredentials> cacheBuilder = CachedSupplier.builder(this::refreshCredentials)
                                                                            .cachedValueName(toString());
        if (builder.asyncCredentialUpdateEnabled) {
            cacheBuilder.prefetchStrategy(new NonBlocking("process-credentials-provider"));
        }

        this.processCredentialCache = cacheBuilder.build();
    }

    private List<String> executableCommand(Builder builder) {
        if (builder.commandAsListOfStrings != null) {
            return Collections.unmodifiableList(builder.commandAsListOfStrings);
        } else {
            List<String> cmd = new ArrayList<>();

            if (Platform.isWindows()) {
                cmd.add("cmd.exe");
                cmd.add("/C");
            } else {
                cmd.add("sh");
                cmd.add("-c");
            }

            String builderCommand = Validate.paramNotNull(builder.command, "command");
            cmd.add(builderCommand);
            return Collections.unmodifiableList(cmd);
        }
    }

    /**
     * Retrieve a new builder that can be used to create and configure a {@link ProcessCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return processCredentialCache.get();
    }

    private RefreshResult<AwsCredentials> refreshCredentials() {
        try {
            String processOutput = executeCommand();
            JsonNode credentialsJson = parseProcessOutput(processOutput);

            AwsCredentials credentials = credentials(credentialsJson);
            Instant credentialExpirationTime = credentialExpirationTime(credentialsJson);

            return RefreshResult.builder(credentials)
                                .staleTime(staleTime(credentialExpirationTime))
                                .prefetchTime(prefetchTime(credentialExpirationTime))
                                .build();
        } catch (InterruptedException e) {
            throw new IllegalStateException("Process-based credential refreshing has been interrupted.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh process-based credentials.", e);
        }
    }

    private Instant staleTime(Instant expiration) {
        if (expiration == null || expiration.equals(Instant.MAX)) {
            return Instant.MAX;
        }
        return expiration.minus(staleTime);
    }

    private Instant prefetchTime(Instant expiration) {
        if (expiration == null || expiration.equals(Instant.MAX)) {
            return Instant.MAX;
        }
        if (credentialRefreshThresholdExplicitlySet) {
            return expiration.minusMillis(credentialRefreshThreshold.toMillis());
        }
        return expiration.minus(prefetchTime);
    }

    /**
     * Parse the output from the credentials process.
     */
    private JsonNode parseProcessOutput(String processOutput) {
        JsonNode credentialsJson = PARSER.parse(processOutput);

        if (!credentialsJson.isObject()) {
            throw new IllegalStateException("Process did not return a JSON object.");
        }

        JsonNode version = credentialsJson.field("Version").orElse(null);
        if (version == null || !version.isNumber() || !version.asNumber().equals("1")) {
            throw new IllegalStateException("Unsupported credential version: " + version);
        }
        return credentialsJson;
    }

    /**
     * Parse the process output to retrieve the credentials.
     */
    private AwsCredentials credentials(JsonNode credentialsJson) {
        String accessKeyId = getText(credentialsJson, "AccessKeyId");
        String secretAccessKey = getText(credentialsJson, "SecretAccessKey");
        String sessionToken = getText(credentialsJson, "SessionToken");
        String accountId = getText(credentialsJson, "AccountId");

        Validate.notEmpty(accessKeyId, "AccessKeyId cannot be empty.");
        Validate.notEmpty(secretAccessKey, "SecretAccessKey cannot be empty.");

        String resolvedAccountId = accountId == null ? this.staticAccountId : accountId;

        return sessionToken != null ?
               AwsSessionCredentials.builder()
                                    .accessKeyId(accessKeyId)
                                    .secretAccessKey(secretAccessKey)
                                    .sessionToken(sessionToken)
                                    .expirationTime(credentialExpirationTime(credentialsJson))
                                    .accountId(resolvedAccountId)
                                    .providerName(this.providerName)
                                    .build() :
               AwsBasicCredentials.builder()
                                  .accessKeyId(accessKeyId)
                                  .secretAccessKey(secretAccessKey)
                                  .accountId(resolvedAccountId)
                                  .providerName(this.providerName)
                                  .build();
    }

    /**
     * Parse the process output to retrieve the expiration date and time.
     */
    private Instant credentialExpirationTime(JsonNode credentialsJson) {
        String expiration = getText(credentialsJson, "Expiration");

        if (expiration != null) {
            return DateUtils.parseIso8601Date(expiration);
        } else {
            return Instant.MAX;
        }
    }

    /**
     * Get a textual value from a json object.
     */
    private String getText(JsonNode jsonObject, String nodeName) {
        return jsonObject.field(nodeName).map(JsonNode::text).orElse(null);
    }

    /**
     * Execute the external process to retrieve credentials.
     */
    private String executeCommand() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(executableCommand);

        ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();

        Process process = processBuilder.start();
        try {
            IoUtils.copy(process.getInputStream(), commandOutput, processOutputLimit);

            process.waitFor();

            if (process.exitValue() != 0) {
                try (InputStream errorStream = process.getErrorStream()) {
                    String errorMessage = IoUtils.toUtf8String(errorStream);
                    throw new IllegalStateException(String.format("Command returned non-zero exit value (%s) with error message: "
                                                                  + "%s", process.exitValue(), errorMessage));
                }
            }

            return new String(commandOutput.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            process.destroy();
        }
    }

    @Override
    public void close() {
        processCredentialCache.close();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Used to configure and create a {@link ProcessCredentialsProvider}. See {@link #builder()} creation.
     */
    public static class Builder implements CopyableBuilder<Builder, ProcessCredentialsProvider> {
        private Boolean asyncCredentialUpdateEnabled = false;
        private String command;
        private List<String> commandAsListOfStrings;
        private Duration credentialRefreshThreshold = Duration.ofSeconds(15);
        private boolean credentialRefreshThresholdExplicitlySet = false;
        private long processOutputLimit = 64000;
        private String staticAccountId;
        private String sourceChain;
        private Duration staleTime;
        private Duration prefetchTime;

        /**
         * @see #builder()
         */
        private Builder() {
        }

        private Builder(ProcessCredentialsProvider provider) {
            this.asyncCredentialUpdateEnabled = provider.asyncCredentialUpdateEnabled;
            this.command = provider.commandFromBuilder;
            this.commandAsListOfStrings = provider.commandAsListOfStringsFromBuilder;
            this.credentialRefreshThreshold = provider.credentialRefreshThreshold;
            this.credentialRefreshThresholdExplicitlySet = provider.credentialRefreshThresholdExplicitlySet;
            this.processOutputLimit = provider.processOutputLimit;
            this.staticAccountId = provider.staticAccountId;
            this.sourceChain = provider.sourceChain;
            this.staleTime = provider.staleTime;
            this.prefetchTime = provider.prefetchTime;
        }

        /**
         * Configure whether the provider should fetch credentials asynchronously in the background. When enabled, a
         * dedicated thread performs credential refreshes during the advisory refresh window (defined by
         * {@link #prefetchTime(Duration)}), so that callers are less likely to block waiting for credentials. Additional
         * resources (a thread) are used to maintain the provider.
         *
         * <p>Regardless of this setting, callers will block if credentials enter the mandatory refresh window (defined by
         * {@link #staleTime(Duration)}).
         *
         * <p>By default, this is disabled.</p>
         */
        @SuppressWarnings("unchecked")
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return this;
        }

        /**
         * Configure the amount of time, relative to credential expiration, that defines the mandatory refresh window. When
         * the cached credentials are within this window (i.e., their remaining lifetime is less than this duration), the
         * provider will block all callers until a refresh attempt completes. If the refresh attempt fails, the provider
         * raises an exception to the caller.
         *
         * <p>This value must be less than {@link #prefetchTime(Duration)}.
         *
         * <p>By default, this is 1 minute.</p>
         *
         * @param staleTime the duration before expiration that triggers mandatory (blocking) refresh
         */
        public Builder staleTime(Duration staleTime) {
            this.staleTime = staleTime;
            return this;
        }

        /**
         * Configure the amount of time, relative to credential expiration, that defines the advisory refresh window. When
         * the cached credentials are within this window (i.e., their remaining lifetime is less than this duration), the
         * provider will attempt to refresh them proactively. If the refresh fails during the advisory window, the provider
         * returns the existing cached credentials. If the refresh fails after credentials have entered the mandatory refresh
         * window (defined by {@link #staleTime(Duration)}), the provider raises an exception.
         *
         * <p>When {@link #asyncCredentialUpdateEnabled(Boolean)} is true, advisory refreshes happen in a background thread
         * and callers immediately receive the current cached credentials. When it is false, one caller will block to perform
         * the refresh while other callers receive the current cached credentials.
         *
         * <p>This value must be greater than {@link #staleTime(Duration)}.
         *
         * <p>By default, this is 5 minutes.</p>
         *
         * @param prefetchTime the duration before expiration that triggers advisory (proactive) refresh
         */
        public Builder prefetchTime(Duration prefetchTime) {
            this.prefetchTime = prefetchTime;
            return this;
        }

        /**
         * Configure the command that should be executed to retrieve credentials.
         * See {@link ProcessBuilder} for details on how this command is used.
         *
         * @deprecated The recommended approach is to specify the command as a list of Strings, using {@link #command(List)}
         * instead, which makes it easier to programmatically add parameters to commands without needing to escape those
         * parameters to protect against command injection.
         */
        @Deprecated
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        /**
         * Configure the command that should be executed to retrieve credentials, as a list of strings.
         * See {@link ProcessBuilder} for details on how this command is used.
         */
        public Builder command(List<String> commandAsListOfStrings) {
            this.commandAsListOfStrings = commandAsListOfStrings;
            return this;
        }

        /**
         * Configure the amount of time between when the credentials expire and when the credentials should start to be
         * refreshed. This allows the credentials to be refreshed *before* they are reported to expire.
         *
         * <p>Default: 15 seconds.</p>
         *
         * @deprecated The provider now uses a default prefetch time of 5 minutes before expiry, aligned with other
         * credential providers. If this method is called, the specified value will be honored as the prefetch time for
         * backward compatibility.
         */
        @Deprecated
        public Builder credentialRefreshThreshold(Duration credentialRefreshThreshold) {
            this.credentialRefreshThreshold = credentialRefreshThreshold;
            this.credentialRefreshThresholdExplicitlySet = true;
            return this;
        }

        /**
         * Configure the maximum amount of data that can be returned by the external process before an exception is
         * raised.
         *
         * <p>Default: 64000 bytes (64KB).</p>
         */
        public Builder processOutputLimit(long outputByteLimit) {
            this.processOutputLimit = outputByteLimit;
            return this;
        }

        /**
         * Configure a static account id for this credentials provider. Account id for ProcessCredentialsProvider is only
         * relevant in a context where a service constructs endpoint URL containing an account id.
         * This option should ONLY be used if the provider should return credentials with account id, and the process does not
         * output account id. If a static account ID is configured, and the process also returns an account
         * id, the process output value overrides the static value. If used, the static account id MUST match the credentials
         * returned by the process.
         */
        public Builder staticAccountId(String staticAccountId) {
            this.staticAccountId = staticAccountId;
            return this;
        }

        /**
         * An optional string denoting previous credentials providers that are chained with this one.
         * <p><b>Note:</b> This method is primarily intended for use by AWS SDK internal components
         * and should not be used directly by external users.</p>
         */
        public Builder sourceChain(String sourceChain) {
            this.sourceChain = sourceChain;
            return this;
        }

        public ProcessCredentialsProvider build() {
            return new ProcessCredentialsProvider(this);
        }
    }

    @Override
    public String toString() {
        return ToString.builder(CLASS_NAME)
                       .add("cmd", executableCommand)
                       .build();
    }
}