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

package software.amazon.awssdk.protocols.query;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

/**
 * Protocol factory for the AWS/EC2 protocol.
 */
@SdkProtectedApi
public final class AwsEc2ProtocolFactory extends AwsQueryProtocolFactory {

    private AwsEc2ProtocolFactory(Builder builder) {
        super(builder);
    }

    @Override
    boolean isEc2() {
        return true;
    }

    /**
     * EC2 has a slightly different location for the <Error/> element than traditional AWS/Query.
     *
     * @param document Root XML document.
     * @return If error root is found than a fulfilled {@link Optional}, otherwise an empty one.
     */
    @Override
    Optional<XmlElement> getErrorRoot(XmlElement document) {
        return document.getOptionalElementByName("Errors")
                       .flatMap(e -> e.getOptionalElementByName("Error"));
    }

    /**
     * @return New builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AwsEc2ProtocolFactory}.
     */
    public static final class Builder extends AwsQueryProtocolFactory.Builder<Builder> {

        private Builder() {
        }

        @Override
        public AwsEc2ProtocolFactory build() {
            return new AwsEc2ProtocolFactory(this);
        }
    }
}
