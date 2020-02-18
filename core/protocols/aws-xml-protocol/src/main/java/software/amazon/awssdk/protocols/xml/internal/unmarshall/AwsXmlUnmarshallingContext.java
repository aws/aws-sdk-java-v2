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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

/**
 * A data class to hold all the context of an unmarshalling stage for the AWS XML protocol as orchestrated by
 * {@link AwsXmlPredicatedResponseHandler}.
 */
@SdkInternalApi
public class AwsXmlUnmarshallingContext {
    private final SdkHttpFullResponse sdkHttpFullResponse;
    private final XmlElement parsedXml;
    private final ExecutionAttributes executionAttributes;
    private final Boolean isResponseSuccess;
    private final XmlElement parsedErrorXml;

    private AwsXmlUnmarshallingContext(Builder builder) {
        this.sdkHttpFullResponse = builder.sdkHttpFullResponse;
        this.parsedXml = builder.parsedXml;
        this.executionAttributes = builder.executionAttributes;
        this.isResponseSuccess = builder.isResponseSuccess;
        this.parsedErrorXml = builder.parsedErrorXml;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The HTTP response.
     */
    public SdkHttpFullResponse sdkHttpFullResponse() {
        return sdkHttpFullResponse;
    }

    /**
     * The parsed XML of the body, or null if there was no body.
     */
    public XmlElement parsedRootXml() {
        return parsedXml;
    }

    /**
     * The {@link ExecutionAttributes} associated with this request.
     */
    public ExecutionAttributes executionAttributes() {
        return executionAttributes;
    }

    /**
     * true if the response indicates success; false if not; null if that has not been determined yet
     */
    public Boolean isResponseSuccess() {
        return isResponseSuccess;
    }

    /**
     * The parsed XML of just the error. null if not found or determined yet.
     */
    public XmlElement parsedErrorXml() {
        return parsedErrorXml;
    }

    public Builder toBuilder() {
        return builder().sdkHttpFullResponse(this.sdkHttpFullResponse)
                        .parsedXml(this.parsedXml)
                        .executionAttributes(this.executionAttributes)
                        .isResponseSuccess(this.isResponseSuccess)
                        .parsedErrorXml(this.parsedErrorXml);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AwsXmlUnmarshallingContext that = (AwsXmlUnmarshallingContext) o;

        if (sdkHttpFullResponse != null ? ! sdkHttpFullResponse.equals(that.sdkHttpFullResponse) :
            that.sdkHttpFullResponse != null) {
            return false;
        }
        if (parsedXml != null ? ! parsedXml.equals(that.parsedXml) : that.parsedXml != null) {
            return false;
        }
        if (executionAttributes != null ? ! executionAttributes.equals(that.executionAttributes) :
            that.executionAttributes != null) {
            return false;
        }
        if (isResponseSuccess != null ? ! isResponseSuccess.equals(that.isResponseSuccess) :
            that.isResponseSuccess != null) {
            return false;
        }
        return parsedErrorXml != null ? parsedErrorXml.equals(that.parsedErrorXml) : that.parsedErrorXml == null;
    }

    @Override
    public int hashCode() {
        int result = sdkHttpFullResponse != null ? sdkHttpFullResponse.hashCode() : 0;
        result = 31 * result + (parsedXml != null ? parsedXml.hashCode() : 0);
        result = 31 * result + (executionAttributes != null ? executionAttributes.hashCode() : 0);
        result = 31 * result + (isResponseSuccess != null ? isResponseSuccess.hashCode() : 0);
        result = 31 * result + (parsedErrorXml != null ? parsedErrorXml.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private SdkHttpFullResponse sdkHttpFullResponse;
        private XmlElement parsedXml;
        private ExecutionAttributes executionAttributes;
        private Boolean isResponseSuccess;
        private XmlElement parsedErrorXml;

        private Builder() {
        }

        public Builder sdkHttpFullResponse(SdkHttpFullResponse sdkHttpFullResponse) {
            this.sdkHttpFullResponse = sdkHttpFullResponse;
            return this;
        }

        public Builder parsedXml(XmlElement parsedXml) {
            this.parsedXml = parsedXml;
            return this;
        }

        public Builder executionAttributes(ExecutionAttributes executionAttributes) {
            this.executionAttributes = executionAttributes;
            return this;
        }

        public Builder isResponseSuccess(Boolean isResponseSuccess) {
            this.isResponseSuccess = isResponseSuccess;
            return this;
        }

        public Builder parsedErrorXml(XmlElement parsedErrorXml) {
            this.parsedErrorXml = parsedErrorXml;
            return this;
        }

        public AwsXmlUnmarshallingContext build() {
            return new AwsXmlUnmarshallingContext(this);
        }
    }
}
