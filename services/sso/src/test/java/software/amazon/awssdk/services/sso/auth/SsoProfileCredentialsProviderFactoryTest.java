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

package software.amazon.awssdk.services.sso.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.services.sso.internal.SsoAccessTokenProvider;

/**
 * Validate the code path of creating the {@link SsoCredentialsProvider} with {@link SsoProfileCredentialsProviderFactory}.
 */
public class SsoProfileCredentialsProviderFactoryTest {

    @Test
    public void createSsoCredentialsProviderWithFactorySucceed() throws IOException {
        String startUrl = "https//d-abc123.awsapps.com/start";
        String generatedTokenFileName = "6a888bdb653a4ba345dd68f21b896ec2e218c6f4.json";

        Map<String, String> properties = new HashMap<>();
        properties.put("sso_account_id", "accountId");
        properties.put("sso_region", "region");
        properties.put("sso_role_name", "roleName");
        properties.put("sso_start_url", "https//d-abc123.awsapps.com/start");
        Profile profile = Profile.builder().name("foo").properties(properties).build();

        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\", \n" +
                           "\"startUrl\": \""+ startUrl +"\"\n" +
                           "}";
        SsoAccessTokenProvider tokenProvider = new SsoAccessTokenProvider(
            prepareTestCachedTokenFile(tokenFile, generatedTokenFileName));

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        assertThat(factory.create(profile, tokenProvider)).isInstanceOf(AwsCredentialsProvider.class);
    }

    private Path prepareTestCachedTokenFile(String tokenFileContent, String generatedTokenFileName) throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path fileDirectory = fs.getPath("./foo");

        Files.createDirectory(fileDirectory);
        Path cachedTokenFilePath = fileDirectory.resolve(generatedTokenFileName);
        Files.write(cachedTokenFilePath, ImmutableList.of(tokenFileContent), StandardCharsets.UTF_8);

        return cachedTokenFilePath;
    }
}