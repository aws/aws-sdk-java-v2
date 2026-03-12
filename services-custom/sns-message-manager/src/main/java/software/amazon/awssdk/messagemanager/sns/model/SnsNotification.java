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
 * A 'Notification' message from SNS.
 * <p>
 * See the <a href="https://docs.aws.amazon.com/sns/latest/dg/http-notification-json.html">API reference</a> for more
 * information.
 */
@SdkPublicApi
public final class SnsNotification extends SnsMessage {
    private final String subject;
    private final URI unsubscribeUrl;

    SnsNotification(BuilderImpl builder) {
        super(builder);
        this.subject = builder.subject;
        this.unsubscribeUrl = builder.unsubscribeUrl;
    }

    @Override
    public SnsMessageType type() {
        return SnsMessageType.NOTIFICATION;
    }

    /**
     * The subject of the message. This may be {@code null}.
     */
    public String subject() {
        return subject;
    }

    /**
     * The URL that can be visited to unsubscribe from the topic.
     */
    public URI unsubscribeUrl() {
        return unsubscribeUrl;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        SnsNotification that = (SnsNotification) o;
        return Objects.equals(subject, that.subject)
               && Objects.equals(unsubscribeUrl, that.unsubscribeUrl);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(subject);
        result = 31 * result + Objects.hashCode(unsubscribeUrl);
        return result;
    }

    public interface Builder extends SnsMessage.Builder<Builder> {

        /**
         * The subject of the message. This may be {@code null}.
         */
        Builder subject(String subject);

        /**
         * The URL that can be visited to unsubscribe from the topic.
         */
        Builder unsubscribeUrl(URI unsubscribeUrl);

        SnsNotification build();
    }

    private static class BuilderImpl extends SnsMessage.BuilderImpl<Builder> implements Builder {
        private String subject;
        private URI unsubscribeUrl;

        @Override
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        @Override
        public Builder unsubscribeUrl(URI unsubscribeUrl) {
            this.unsubscribeUrl = unsubscribeUrl;
            return this;
        }

        @Override
        public SnsNotification build() {
            return new SnsNotification(this);
        }
    }
}
