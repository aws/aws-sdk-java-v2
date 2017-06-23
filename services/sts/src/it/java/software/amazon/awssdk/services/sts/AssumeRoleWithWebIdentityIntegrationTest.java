/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.sts;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityResponse;
import software.amazon.awssdk.services.sts.model.InvalidIdentityTokenException;

@ReviewBeforeRelease("This could be useful to cleanup and present as a customer sample")
@Ignore
public class AssumeRoleWithWebIdentityIntegrationTest extends IntegrationTestBase {

    private static final String GOOGLE_OPENID_TOKEN =
            "eyJhbGciOiJSUzI1NiIsImtpZCI6ImRlMmYxYjQ0NTAwOGIyYTBlZjBmNTk5OWVjYTdkOGYzMDQyNDczYzQifQ.eyJpc3MiOiJhY2NvdW50cy5nb2" +
            "9nbGUuY29tIiwiYXRfaGFzaCI6IjJBaUJVS29lc1VoM1VDTGpCRzZaQ2ciLCJzdWIiOiIxMDUyNjUwOTAyNzk1NDY0MjAzMzgiLCJhdWQiOiI0MDc" +
            "0MDg3MTgxOTIuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhenAiOiI0MDc0MDg3MTgxOTIuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20i" +
            "LCJpYXQiOjEzNjY4NDIxMjYsImV4cCI6MTM2Njg0NjAyNn0.O30jakLkK3vOo5cfn2z2gl0MvzFALORmp5XfPMCVMeW9-Zc8R9ipm5VT5pUhzZ7Ea" +
            "pY3K_ctEAYMKv_cXU6TWfjFSDT8IHQrD1M6iIXeATkZRivTPKddWY6UHQUVe3_qbHNEUYWbQwdEBBZzxPPG-ULzmDOqN9WE4wDf5JiHwrE";

    private static final String FACEBOOK_APP_ID = "402452589861745";
    private static final String FACEBOOK_APP_SECRET = "7cbfb4cfaabda54b47e96135f122616d";
    private static final String FACEBOOK_URI = "https://graph.facebook.com/oauth/access_token?client_id=" + FACEBOOK_APP_ID +
                                               "&client_secret=" + FACEBOOK_APP_SECRET + "&grant_type=client_credentials";

    private static final String FACEBOOK_PROVIDER = "graph.facebook.com";

    private static final String ROLE_ARN = "arn:aws:iam::599169622985:role/aws-java-sdk-sts-test";
    private static final String SESSION_NAME = "javatest";


    @Test
    public void testGoogleOAuth() throws Exception {
        AssumeRoleWithWebIdentityRequest request =
                AssumeRoleWithWebIdentityRequest.builder().webIdentityToken(GOOGLE_OPENID_TOKEN)
                                                .roleArn(ROLE_ARN)
                                                .roleSessionName(SESSION_NAME).build();

        try {
            AssumeRoleWithWebIdentityResponse result = sts.assumeRoleWithWebIdentity(request);
            fail("Expected Expired token error");
        } catch (InvalidIdentityTokenException e) {
            // expected error
            return;
        }
    }

    @Test
    public void testFacebookOAuth() throws Exception {
        URL accessTokenURL = new URL(FACEBOOK_URI);
        HttpURLConnection connection = (HttpURLConnection) accessTokenURL.openConnection();
        connection.setDoOutput(true);
        connection.connect();
        String rawResponse = IOUtils.toString(connection.getInputStream());

        URL newUserURL = new URL("https://graph.facebook.com/" + FACEBOOK_APP_ID +
                                 "/accounts/test-users?installed=true&name=Foo%20Bar&locale=en_US&" +
                                 "permissions=read_stream&method=post&" + rawResponse);

        JsonNode json = new ObjectMapper().readTree(newUserURL);
        assert (json.has("access_token"));

        String token = json.get("access_token").asText();
        try {
            AssumeRoleWithWebIdentityRequest request = AssumeRoleWithWebIdentityRequest.builder().webIdentityToken(token)
                                                                                       .providerId(FACEBOOK_PROVIDER)
                                                                                       .roleArn(ROLE_ARN)
                                                                                       .roleSessionName(SESSION_NAME).build();

            AssumeRoleWithWebIdentityResponse result = sts.assumeRoleWithWebIdentity(request);

            System.out.println(result.credentials().accessKeyId());
            System.out.println(result.credentials().secretAccessKey());
            System.out.println(result.credentials().sessionToken());

            assertNotNull(result.credentials());
            assertNotNull(result.credentials().accessKeyId());
            assertNotNull(result.credentials().secretAccessKey());
            assertNotNull(result.credentials().sessionToken());
        } finally {
            URL deleteURL = new URL("https://graph.facebook.com/" + json.get("id").asText() +
                                    "?method=delete&access_token=" + token);
            connection = (HttpURLConnection) deleteURL.openConnection();
            connection.setDoOutput(true);
            connection.connect();
        }
    }
}
