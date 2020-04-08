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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.util.json.JacksonUtils;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Platform;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * A credentials provider that can load credentials from an external process. This is used to support the credential_process
 * setting in the profile credentials file. See
 * https://docs.aws.amazon.com/cli/latest/topic/config-vars.html#sourcing-credentials-from-external-processes for more
 * information.
 *
 * Created using {@link #builder()}.
 *
 * Available settings:
 * <ul>
 *     <li>Command - The command that should be executed to retrieve credentials.</li>
 *     <li>CredentialRefreshThreshold - The amount of time between when the credentials expire and when the credentials should
 *     start to be refreshed. This allows the credentials to be refreshed *before* they are reported to expire. Default: 15
 *     seconds.</li>
 *     <li>ProcessOutputLimit - The maximum amount of data that can be returned by the external process before an exception is
 *     raised. Default: 64000 bytes (64KB).</li>
 * </ul>
 */
@SdkPublicApi
public final class ProcessCredentialsProvider implements AwsCredentialsProvider {
    private final List<String> command;
    private final Duration credentialRefreshThreshold;
    private final long processOutputLimit;

    private final CachedSupplier<AwsCredentials> processCredentialCache;

    /**
     * @see #builder()
     */
    private ProcessCredentialsProvider(Builder builder) {
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

        this.command = Collections.unmodifiableList(cmd);
        this.processOutputLimit = Validate.isPositive(builder.processOutputLimit, "processOutputLimit");
        this.credentialRefreshThreshold = Validate.isPositive(builder.credentialRefreshThreshold, "expirationBuffer");

        CachedSupplier.Builder<AwsCredentials> cacheBuilder = CachedSupplier.builder(this::refreshCredentials);
        if (builder.asyncCredentialUpdateEnabled) {
            cacheBuilder.prefetchStrategy(new NonBlocking("process-credentials-provider"));
        }

        this.processCredentialCache = cacheBuilder.build();
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
        JsonNode credentialsJson = JacksonUtils.sensitiveJsonNodeOf(processOutput);

        if (!credentialsJson.isObject()) {
            throw new IllegalStateException("Process did not return a JSON object.");
        }

        JsonNode version = credentialsJson.get("Version");
        if (version == null || !version.isInt() || version.asInt() != 1) {
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

        Validate.notEmpty(accessKeyId, "AccessKeyId cannot be empty.");
        Validate.notEmpty(secretAccessKey, "SecretAccessKey cannot be empty.");

        if (sessionToken != null) {
            return AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken);
        } else {
            return AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        }
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
     * Get a textual value from a json object, throwing an exception if the node is missing or not textual.
     */
    private String getText(JsonNode jsonObject, String nodeName) {
        JsonNode subNode = jsonObject.get(nodeName);

        if (subNode == null) {
            return null;
        }

        if (!subNode.isTextual()) {
            throw new IllegalStateException(nodeName + " from credential process should be textual, but was " +
                                            subNode.getNodeType());
        }

        return subNode.asText();
    }

    /**
     * Execute the external process to retrieve credentials.
     */
    private String executeCommand() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        ByteArrayOutputStream commandOutput = new ByteArrayOutputStream();

        Process process = processBuilder.start();
        try {
            IoUtils.copy(process.getInputStream(), commandOutput, processOutputLimit);

            process.waitFor();

            if (process.exitValue() != 0) {
                throw new IllegalStateException("Command returned non-zero exit value: " + process.exitValue());
            }

            return new String(commandOutput.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            process.destroy();
        }
    }

    /**
     * Used to configure and create a {@link ProcessCredentialsProvider}. See {@link #builder()} creation.
     */
    public static class Builder {
        private Boolean asyncCredentialUpdateEnabled = false;
        private String command;
        private Duration credentialRefreshThreshold = Duration.ofSeconds(15);
        private long processOutputLimit = 64000;

        /**
         * @see #builder()
         */
        private Builder() {
        }

        /**
         * Configure whether the provider should fetch credentials asynchronously in the background. If this is true, threads are
         * less likely to block when credentials are loaded, but additional resources are used to maintain the provider.
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
         */
        public Builder command(String command) {
            this.command = command;
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

        public ProcessCredentialsProvider build() {
            return new ProcessCredentialsProvider(this);
        }
    }
}