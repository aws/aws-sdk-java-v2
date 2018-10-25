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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Contains information needed to create a {@link AwsXmlResponseHandler} for the client.
 */
@NotThreadSafe
@SdkProtectedApi
public final class XmlOperationMetadata {

    private boolean hasStreamingSuccessResponse;
    private boolean useRootElement;

    public boolean isHasStreamingSuccessResponse() {
        return hasStreamingSuccessResponse;
    }

    public XmlOperationMetadata withHasStreamingSuccessResponse(boolean hasStreamingSuccessResponse) {
        this.hasStreamingSuccessResponse = hasStreamingSuccessResponse;
        return this;
    }

    public boolean useRootElement() {
        return useRootElement;
    }

    /**
     * This value indicates if the root element in the response xml document should be ignored
     * or not while unmarshalling the content. The value is true if the root element is a member of the output shape
     * and false if the root element can be ignored.
     *
     * This value is true if the output shape either has an explicit payload member or
     * the customization member "useRootXmlElementForResult" is set.
     */
    public XmlOperationMetadata useRootElement(boolean useRootElement) {
        this.useRootElement = useRootElement;
        return this;
    }
}
