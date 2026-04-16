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

package software.amazon.awssdk.messagemanager.sns.model;

import java.net.URI;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * An 'UnsubscribeConfirmation' message from SNS.
 * <p>
 * See the <a href="https://docs.aws.amazon.com/sns/latest/dg/http-unsubscribe-confirmation-json.html">API reference</a> for more
 * information.
 */
@SdkPublicApi
public final class SnsUnsubscribeConfirmation extends SnsMessage {
    private final URI subscribeUrl;
    private final String token;

    private SnsUnsubscribeConfirmation(BuilderImpl builder) {
        super(builder);
        this.subscribeUrl = builder.subscribeUrl;
        this.token = builder.token;
    }

    @Override
    public SnsMessageType type() {
        return SnsMessageType.UNSUBSCRIBE_CONFIRMATION;
    }

    /**
     * The URL that can be visited used to re-confirm subscription to the topic.
     */
    public URI subscribeUrl() {
        return subscribeUrl;
    }

    /**
     * A value that can be used with the
     * <a href="https://docs.aws.amazon.com/sns/latest/api/API_ConfirmSubscription.html">ConfirmSubscription</a>
     * API to reconfirm subscription to the topic.
     */
    public String token() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        SnsUnsubscribeConfirmation that = (SnsUnsubscribeConfirmation) o;
        return Objects.equals(subscribeUrl, that.subscribeUrl) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(subscribeUrl);
        result = 31 * result + Objects.hashCode(token);
        return result;
    }

    @Override
    public String toString() {
        return toStringBuilder("SnsUnsubscribeConfirmation")
            .add("SubscribeUrl", subscribeUrl())
            .add("Token", token())
            .build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SnsMessage.Builder<Builder> {

        /**
         * The URL that can be visited used to re-confirm subscription to the topic.
         */
        Builder subscribeUrl(URI subscribeUrl);

        /**
         * A value that can be used with the
         * <a href="https://docs.aws.amazon.com/sns/latest/api/API_ConfirmSubscription.html">ConfirmSubscription</a>
         * API to reconfirm subscription to the topic.
         */
        Builder token(String token);

        SnsUnsubscribeConfirmation build();
    }

    private static class BuilderImpl extends SnsMessage.BuilderImpl<Builder> implements Builder {
        private URI subscribeUrl;
        private String token;

        @Override
        public Builder subscribeUrl(URI subscribeUrl) {
            this.subscribeUrl = subscribeUrl;
            return this;
        }

        @Override
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        @Override
        public SnsUnsubscribeConfirmation build() {
            return new SnsUnsubscribeConfirmation(this);
        }
    }
}
