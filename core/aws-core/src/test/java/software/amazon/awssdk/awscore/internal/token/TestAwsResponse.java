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

package software.amazon.awssdk.awscore.internal.token;

import java.time.Instant;
import java.util.List;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkField;

public class TestAwsResponse extends AwsResponse {

    private final String accessToken;
    private final Instant expiryTime;
    private final String startUrl;

    protected TestAwsResponse(Builder builder) {
        super(builder);
        this.accessToken = builder.accessToken;
        this.expiryTime = builder.expiryTime;
        this.startUrl = builder.startUrl;

    }

    public String getAccessToken() {
        return accessToken;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public static Builder builder(){
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return null;
    }

    public static class Builder extends BuilderImpl{
        public Builder() {
        }

        private String accessToken;
        private Instant expiryTime;
        private String startUrl;

        public Builder(TestAwsResponse testAwsResponse) {
            this.accessToken = testAwsResponse.accessToken;
            this.expiryTime = testAwsResponse.expiryTime;
            this.startUrl = testAwsResponse.startUrl;
        }


        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder expiryTime(Instant expiryTime) {
            this.expiryTime = expiryTime;
            return this;
        }

        public Builder startUrl(String startUrl) {
            this.startUrl = startUrl;
            return this;
        }
        @Override
        public TestAwsResponse build() {
            return new TestAwsResponse(this);
        }
    }
}
