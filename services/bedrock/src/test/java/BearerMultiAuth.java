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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.StaticTokenProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.BedrockClientBuilder;
import software.amazon.awssdk.services.bedrock.model.ListCustomModelsRequest;
import software.amazon.awssdk.services.bedrock.model.ListCustomModelsResponse;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;


public class BearerMultiAuth {
    private static final AuthenticationInterceptor interceptor = new AuthenticationInterceptor();

    private final String accessKey;
    private final String secretKey;
    private static final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    public BearerMultiAuth() {
        ProfileCredentialsProvider profileCredentialsProvider = ProfileCredentialsProvider.create("default-Feb15-2025");
        this.accessKey = profileCredentialsProvider.resolveCredentials().accessKeyId();
        this.secretKey = profileCredentialsProvider.resolveCredentials().secretAccessKey();
    }


    @Test
    void whenNoCredentialProvided_shouldThrowSdkClientException() {
        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        // Act & Assert
        SdkClientException exception = assertThrows(SdkClientException.class, () ->
                                                        client.listCustomModels(ListCustomModelsRequest.builder().build()),
                                                    "Should throw SdkClientException when no credentials are provided"
        );

        assertTrue(exception.getMessage().contains("Unable to load credentials"),
                   "Exception message should indicate credentials loading failure");
    }

    @Test
    void operationCredentials() {

        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        // Act
        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().overrideConfiguration(r ->r.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))).build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }


    @Test
    void clientLevelConfigurations() {

        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey,secretKey)))
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        // Act
        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }


    @Test
    void systemPropertyCredentials() {

        System.setProperty("aws.accessKeyId", accessKey);
        System.setProperty("aws.secretAccessKey", secretKey);

        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        // Act
        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }


    @Test
    void environmentVariableCredentials() {


        environmentVariableHelper.set(SdkSystemSetting.AWS_ACCESS_KEY_ID, accessKey);
        environmentVariableHelper.set(SdkSystemSetting.AWS_SECRET_ACCESS_KEY, secretKey);

        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        // Act
        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }



    @Test
    void profileFileCredentials() {


        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                                .defaultProfileFile(createCredentialProfileFile(accessKey,secretKey)))
                                            .build();

        // Act
        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }


    @Test
    void clientTokenProvider() {


        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        // Act
        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }


    @Test
    void operationCredentialsAndClientTokenProvider() {


        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().overrideConfiguration(r ->r.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))).build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }

    @Test
    void clientCredentialsAndClientTokenProvider() {

        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey,secretKey)))
                                            .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }



    @Test
    void systemPropertyCredentialsAndClientTokenProvider() {

        System.setProperty("aws.accessKeyId", accessKey);
        System.setProperty("aws.secretAccessKey", secretKey);

        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))
                                            .build();

        // Act
        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );




    }

    @Test
    void environmentVariableCredentialsAndClientTokenProvider() {


        environmentVariableHelper.set(SdkSystemSetting.AWS_ACCESS_KEY_ID, accessKey);
        environmentVariableHelper.set(SdkSystemSetting.AWS_SECRET_ACCESS_KEY, secretKey);

        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))

                                            .build();

        // Act
        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }


    @Test
    void profileFileCredentialsAndClientTokenProvider() {


        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                                                         .defaultProfileFile(createCredentialProfileFile(accessKey,secretKey)))
                                            .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))

                                            .build();

        // Act
        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }


    @Test
    void nullOperationCredentialsAndClientTokenProvider() {


        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().overrideConfiguration(r ->r.credentialsProvider(StaticCredentialsProvider.create(null))).build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }

    @Test
    void nullOperationNullClientCredentialsAndClientTokenProvider() {


        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))
            .credentialsProvider(null)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().overrideConfiguration(r ->r.credentialsProvider(null)).build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }


    @Test
    void nullClientCredentialsAndClientTokenProvider() {

        //
        // StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(null,
        //                                                                                                             null));
        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))
                                            .credentialsProvider(null)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        ListCustomModelsResponse response = client.listCustomModels(ListCustomModelsRequest.builder().build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }



    @Test
    void operationCredentialsAndNullClientTokenProvider() {


        StaticTokenProvider tokenProvider = StaticTokenProvider.create(() -> null);
        BedrockClient client = BedrockClient.builder()
                                            .region(Region.US_EAST_1)
                                            .tokenProvider(tokenProvider)
                                            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .build();

        ListCustomModelsResponse response = client.listCustomModels(
            ListCustomModelsRequest.builder().overrideConfiguration(r ->r.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))).build());

        String authHeader = interceptor.getAuthorizationHeader();
        assertAll(
            () -> assertNotNull(response, "Response should not be null"),
            () -> assertFalse(response.modelSummaries().isEmpty(), "Custom models list should not be empty"),
            () -> assertTrue(authHeader.startsWith("AWS4-HMAC-SHA256"),
                             "Authorization header should use AWS4-HMAC-SHA256 signature")

        );
    }




    @AfterEach
    void clear(){
        environmentVariableHelper.reset();
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");

    }



    private static ProfileFile createCredentialProfileFile(String accessKey, String secretKey) {
        StringBuilder profileFileContent = new StringBuilder();
        profileFileContent.append("[default]\n");

        if (accessKey != null) {
            profileFileContent.append("aws_access_key_id = ").append(accessKey).append("\n");
        }

        if (secretKey != null) {
            profileFileContent.append("aws_secret_access_key = ").append(secretKey).append("\n");
        }

        return ProfileFile.builder()
                          .type(ProfileFile.Type.CREDENTIALS)
                          .content(profileFileContent.toString())
                          .build();
    }



    private static class AuthenticationInterceptor implements ExecutionInterceptor {
        private Optional<String> authHeader = Optional.empty();

        String getAuthorizationHeader() {
            return authHeader.orElse(null);
        }

        void reset() {
            authHeader = Optional.empty();
        }

        @Override
        public void afterTransmission(Context.AfterTransmission context,
                                      ExecutionAttributes executionAttributes) {
            authHeader = context.httpRequest().firstMatchingHeader("Authorization");
        }
    }





}
