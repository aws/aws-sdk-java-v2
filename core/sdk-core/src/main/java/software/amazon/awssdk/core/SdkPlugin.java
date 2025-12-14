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

package software.amazon.awssdk.core;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * A plugin that modifies SDK client configuration at client creation time or per-request execution time.
 *
 * <p>Plugins provide an extensibility mechanism for customizing SDK client behavior without modifying core SDK code.
 * They can modify configuration such as retry policies, timeouts, execution interceptors, endpoints, and authentication
 * schemes.
 *
 * <p>Plugins can be applied at two levels:
 * <ul>
 *   <li><b>Client-level:</b> Applied once during client creation and affects all requests made by that client</li>
 *   <li><b>Request-level:</b> Applied per-request and can override client-level configuration for specific requests</li>
 * </ul>
 *
 * <p><b>When to use plugins vs direct configuration:</b>
 * <ul>
 *   <li>Use <b>direct configuration</b> for simple, one-time client setup specific to your application</li>
 *   <li>Use <b>plugins</b> when you need to:
 *     <ul>
 *       <li>Reuse the same configuration across multiple SDK clients</li>
 *       <li>Package configuration as a library or module for distribution</li>
 *       <li>Apply conditional or dynamic configuration logic</li>
 *       <li>Compose multiple configuration strategies together</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><b>Client-level plugin example:</b>
 * {@snippet :
 *   // Create a reusable plugin with multiple configuration settings
 *   SdkPlugin standardPlugin = config -> {
 *       config.endpointOverride(URI.create("https://127.0.0.1"))
 *             .overrideConfiguration(c -> c
 *                 .apiCallTimeout(Duration.ofSeconds(30))
 *                 .addExecutionInterceptor(new LoggingInterceptor()));
 *   };
 *
 *   // Apply to multiple clients
 *   S3Client s3Client = S3Client.builder()
 *       .addPlugin(standardPlugin)
 *       .build();
 *
 *   DynamoDbClient dynamoClient = DynamoDbClient.builder()
 *       .addPlugin(standardPlugin)
 *       .build();
 * }
 *
 * <p><b>Composing multiple plugins:</b>
 * {@snippet :
 *   // Core plugin always applied
 *   SdkPlugin corePlugin = config -> {
 *       config.overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(30)));
 *   };
 *
 *   // Optional feature plugins
 *   SdkPlugin compressionPlugin = config -> {
 *       config.overrideConfiguration(c -> c.compressionConfiguration(
 *           CompressionConfiguration.builder().requestCompressionEnabled(true).build()));
 *   };
 *
 *   SdkPlugin tracingPlugin = config -> {
 *       config.overrideConfiguration(c -> c.addExecutionInterceptor(new TracingInterceptor()));
 *   };
 *
 *   // Conditionally compose plugins based on feature flags
 *   S3Client.Builder builder = S3Client.builder().addPlugin(corePlugin);
 *   if (compressionEnabled) {
 *       builder.addPlugin(compressionPlugin);
 *   }
 *   if (tracingEnabled) {
 *       builder.addPlugin(tracingPlugin);
 *   }
 *   S3Client client = builder.build();
 * }
 *
 * <p>Plugins are invoked after default configuration is applied, allowing them to override SDK defaults.
 * Multiple plugins can be registered and are executed in the order they were added.
 *
 * <p><b>Configuration precedence (highest to lowest):</b>
 * <ol>
 *   <li>Plugin settings (applied last, highest precedence)</li>
 *   <li>Direct client builder settings (e.g., {@code .overrideConfiguration()})</li>
 *   <li>Service-specific defaults</li>
 *   <li>Global SDK defaults</li>
 * </ol>
 *
 * <p><b>Note:</b> Request-level plugins have different precedence behavior. Request-level override configuration
 * takes precedence over request-level plugin settings, meaning direct request configuration will override plugin
 * settings for that request.
 *
 * @see software.amazon.awssdk.core.client.builder.SdkClientBuilder#addPlugin(SdkPlugin)
 * @see software.amazon.awssdk.core.RequestOverrideConfiguration.Builder#addPlugin(SdkPlugin)
 */
@SdkPublicApi
@ThreadSafe
@FunctionalInterface
public interface SdkPlugin extends SdkAutoCloseable {

    /**
     * Modifies the provided client configuration.
     *
     * <p>This method is invoked by the SDK to allow the plugin to customize the client configuration.
     * Implementations can modify any aspect of the configuration exposed through the builder, including
     * override configuration, endpoints, and authentication schemes.
     *
     * @param config the configuration builder to modify
     */
    void configureClient(SdkServiceClientConfiguration.Builder config);

    @Override
    default void close() {
    }
}
