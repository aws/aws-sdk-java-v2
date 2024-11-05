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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Platform;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} that
 * <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-process.html">loads credentials from an
 * external process</a>.
 *
 * <p>
 * This credential provider caches the credential result, and will only invoke the process periodically to keep the credential
 * "fresh". As a result, it is recommended that you create a single credentials provider of this type and reuse it throughout
 * your application. You may notice small latency increases on requests that refresh the cached credentials. To avoid this
 * latency increase, you can enable async refreshing with {@link Builder#asyncCredentialUpdateEnabled(Boolean)}. If you enable
 * this setting, you must {@link #close()} the credential provider if you are done using it, to disable the background
 * refreshing task. If you fail to do this, your application could run out of resources.
 * <p>
 * This credentials provider is used by the {@link ProfileCredentialsProvider} if the {@code credential_process} profile
 * property is configured. Because the {@code ProfileCredentialsProvider} is included in the {@link DefaultCredentialsProvider},
 * this credentials provider is considered included as well. When configured with {@code credential_process}, the process is
 * executed using a shell ({@code cmd.exe /C [Command]} in Windows, {@code sh -c [Command]} elsewhere).
 * <p>
 * This can be created using {@link ProcessCredentialsProvider#builder()}:
 * {@snippet :
 * ProcessCredentialsProvider credentialsProvider =
 *     ProcessCredentialsProvider.builder()
 *                               .command(Arrays.asList("/opt/example-path/example-script", "param1", "param2"))
 *                               .build();
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 *}
 */
@SdkPublicApi
public final class ProcessCredentialsProvider
    implements AwsCredentialsProvider,
               SdkAutoCloseable,
               ToCopyableBuilder<ProcessCredentialsProvider.Builder, ProcessCredentialsProvider> {
    private static final String PROVIDER_NAME = "ProcessCredentialsProvider";
    private static final JsonNodeParser PARSER = JsonNodeParser.builder()
                                                               .removeErrorLocations(true)
                                                               .build();

    private final List<String> executableCommand;
    private final Duration credentialRefreshThreshold;
    private final long processOutputLimit;
    private final String staticAccountId;

    private final CachedSupplier<AwsCredentials> processCredentialCache;

    private final String commandFromBuilder;

    private final List<String> commandAsListOfStringsFromBuilder;

    private final Boolean asyncCredentialUpdateEnabled;

    /**
     * @see #builder()
     */
    private ProcessCredentialsProvider(Builder builder) {
        this.executableCommand = executableCommand(builder);
        this.processOutputLimit = Validate.isPositive(builder.processOutputLimit, "processOutputLimit");
        this.credentialRefreshThreshold = Validate.isPositive(builder.credentialRefreshThreshold, "expirationBuffer");
        this.commandFromBuilder = builder.command;
        this.commandAsListOfStringsFromBuilder = builder.commandAsListOfStrings;
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.staticAccountId = builder.staticAccountId;

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
                                .staleTime(credentialExpirationTime)
                                .prefetchTime(credentialExpirationTime.minusMillis(credentialRefreshThreshold.toMillis()))
                                .build();
        } catch (InterruptedException e) {
            throw new IllegalStateException("Process-based credential refreshing has been interrupted.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to refresh process-based credentials.", e);
        }
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
                                    .providerName(PROVIDER_NAME)
                                    .build() :
               AwsBasicCredentials.builder()
                                  .accessKeyId(accessKeyId)
                                  .secretAccessKey(secretAccessKey)
                                  .accountId(resolvedAccountId)
                                  .providerName(PROVIDER_NAME)
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
        private long processOutputLimit = 64000;
        private String staticAccountId;

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
            this.processOutputLimit = provider.processOutputLimit;
            this.staticAccountId = provider.staticAccountId;
        }

        /**
         * Configure whether the provider should fetch credentials asynchronously in the background. If this is true,
         * threads are less likely to block when credentials are loaded, but additional resources are used to maintain
         * the provider.
         *
         * <p>By default, this is disabled.</p>
         */
        @SuppressWarnings("unchecked")
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
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
         */
        public Builder credentialRefreshThreshold(Duration credentialRefreshThreshold) {
            this.credentialRefreshThreshold = credentialRefreshThreshold;
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

        public ProcessCredentialsProvider build() {
            return new ProcessCredentialsProvider(this);
        }
    }

    @Override
    public String toString() {
        return ToString.builder(PROVIDER_NAME)
                       .add("cmd", executableCommand)
                       .build();
    }
}