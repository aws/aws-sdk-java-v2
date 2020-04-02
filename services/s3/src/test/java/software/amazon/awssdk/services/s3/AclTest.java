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

package software.amazon.awssdk.services.s3;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutBucketAclRequest;
import software.amazon.awssdk.services.s3.model.Type;

public class AclTest {
    private static final String OWNER_ID = "123456";
    private static final String OWNER_DISPLAY_NAME = "foobar";
    private static final String READ_ONLY_USER_ID = "7891011";
    private static final String READ_ONLY_USER_DISPLAY_NAME = "helloworld";
    private static final String MOCK_ACL_RESPONSE = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?><AccessControlPolicy xmlns=\"http://s3.amazonaws"
                                                                  + ".com/doc/2006-03-01/\"><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><DisplayName>%s</DisplayName><ID>%s</ID></Grantee><Permission>FULL_CONTROL</Permission></Grant><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><DisplayName>%s</DisplayName><ID>%s</ID></Grantee><Permission>READ</Permission></Grant></AccessControlList><Owner><DisplayName>%s</DisplayName><ID>%s</ID></Owner></AccessControlPolicy>",
                                                                  OWNER_DISPLAY_NAME,OWNER_ID, READ_ONLY_USER_DISPLAY_NAME, READ_ONLY_USER_ID, OWNER_DISPLAY_NAME, OWNER_ID);

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private S3Client s3Client;

    @Before
    public void setup() {
        URI endpoint = URI.create("http://localhost:" + mockServer.port());
        s3Client = S3Client.builder()
                           .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                           .region(Region.US_WEST_2)
                           .endpointOverride(endpoint)
                           .build();
    }

    @Test
    public void putBucketAcl_marshalling() {
        stubFor(put(anyUrl())
                    .willReturn(aResponse().withStatus(200)));

        s3Client.putBucketAcl(request());
        verify(anyRequestedFor(anyUrl()).withRequestBody(new ContainsPattern(MOCK_ACL_RESPONSE)));
    }

    @Test
    public void getBucketAcl_shouldUnmarshallCorrectly() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withBody(MOCK_ACL_RESPONSE).withStatus(200)));

        GetBucketAclResponse bucketAcl = s3Client.getBucketAcl(b -> b.bucket("test"));
        assertThat(bucketAcl.owner()).isEqualTo(request().accessControlPolicy().owner());
        assertThat(bucketAcl.grants()).isEqualTo(request().accessControlPolicy().grants());
    }

    private PutBucketAclRequest request() {

        List<Grant> grants = new ArrayList<>();
        grants.add(Grant.builder()
                        .grantee(g -> g.type(Type.CANONICAL_USER).id(OWNER_ID).displayName(OWNER_DISPLAY_NAME))
                        .permission(Permission.FULL_CONTROL)
                        .build());
        grants.add(Grant.builder()
                        .grantee(g -> g.type(Type.CANONICAL_USER).id(READ_ONLY_USER_ID).displayName(READ_ONLY_USER_DISPLAY_NAME))
                        .permission(Permission.READ)
                        .build());
        return PutBucketAclRequest.builder()
                                  .bucket("bucket")
                                  .accessControlPolicy(b -> b.grants(grants)
                                                             .owner(o -> o.id(OWNER_ID).displayName(OWNER_DISPLAY_NAME))
                                                             .build()).build();

    }
}
