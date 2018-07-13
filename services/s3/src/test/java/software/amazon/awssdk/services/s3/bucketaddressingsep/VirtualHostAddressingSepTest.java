/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.bucketaddressingsep;

import static org.assertj.core.api.Assertions.assertThat;
import static utils.S3MockUtils.mockListObjectsResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;

@RunWith(Parameterized.class)
public class VirtualHostAddressingSepTest {
    private static final String TEST_FILE_PATH = "VirtualAddressingSepTestCases.json";
    private MockHttpClient mockHttpClient;
    private TestCaseModel testCaseModel;

    public VirtualHostAddressingSepTest(TestCaseModel testCaseModel) {
        this.testCaseModel = testCaseModel;
    }

    @Before
    public void setup() {
        mockHttpClient = new MockHttpClient();
    }

    @Parameterized.Parameters
    public static List<TestCaseModel> testInputs() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        InputStream jsonData = VirtualHostAddressingSepTest.class.getResourceAsStream(TEST_FILE_PATH);
        return objectMapper.readValue(jsonData,
                                      typeFactory.constructCollectionType(List.class, TestCaseModel.class));
    }

    @Test
    public void assertTestCase() throws UnsupportedEncodingException {
        final String bucket = testCaseModel.getBucket();
        final String expectedUri = testCaseModel.getExpectedUri();

        S3Client s3Client = constructClient(testCaseModel);

        mockHttpClient.stubNextResponse(mockListObjectsResponse());
        s3Client.listObjects(ListObjectsRequest.builder().bucket(bucket).build());

        assertThat(mockHttpClient.getLastRequest().getUri())
            .isEqualTo(URI.create(expectedUri));
    }

    private S3Client constructClient(TestCaseModel testCaseModel) {
        return S3Client.builder()
                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                       .httpClient(mockHttpClient)
                       .region(Region.of(testCaseModel.getRegion()))
                       .serviceConfiguration(c -> c.pathStyleAccessEnabled(testCaseModel.isPathStyle())
                                                   .accelerateModeEnabled(testCaseModel.isUseS3Accelerate())
                                                   .dualstackEnabled(testCaseModel.isUseDualstack()))
                       .build();
    }


    private static class TestCaseModel {
        private String bucket;
        private String configuredAddressingStyle;
        private String expectedUri;
        private String region;
        private boolean useDualstack;
        private boolean useS3Accelerate;

        public String getBucket() {
            return bucket;
        }

        @JsonProperty("Bucket")
        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getConfiguredAddressingStyle() {
            return configuredAddressingStyle;
        }

        @JsonProperty("ConfiguredAddressingStyle")
        public void setConfiguredAddressingStyle(String configuredAddressingStyle) {
            this.configuredAddressingStyle = configuredAddressingStyle;
        }

        public String getExpectedUri() {
            return expectedUri;
        }

        @JsonProperty("ExpectedUri")
        public void setExpectedUri(String expectedUri) {
            this.expectedUri = expectedUri;
        }

        public String getRegion() {
            return region;
        }

        @JsonProperty("Region")
        public void setRegion(String region) {
            this.region = region;
        }

        public boolean isUseDualstack() {
            return useDualstack;
        }

        @JsonProperty("UseDualstack")
        public void setUseDualstack(boolean useDualstack) {
            this.useDualstack = useDualstack;
        }

        public boolean isUseS3Accelerate() {
            return useS3Accelerate;
        }

        @JsonProperty("UseS3Accelerate")
        public void setUseS3Accelerate(boolean useS3Accelerate) {
            this.useS3Accelerate = useS3Accelerate;
        }

        public boolean isPathStyle() {
            return configuredAddressingStyle.equals("path");
        }
    }
}