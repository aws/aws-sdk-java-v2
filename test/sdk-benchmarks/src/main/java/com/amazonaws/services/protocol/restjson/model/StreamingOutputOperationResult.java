/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.services.protocol.restjson.model;

import java.io.Serializable;


/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/StreamingOutputOperation" target="_top">AWS
 *      API Documentation</a>
 */

public class StreamingOutputOperationResult extends com.amazonaws.AmazonWebServiceResult<com.amazonaws.ResponseMetadata> implements Serializable, Cloneable {

    private java.io.InputStream streamingMember;

    /**
     * @param streamingMember
     */

    public void setStreamingMember(java.io.InputStream streamingMember) {
        this.streamingMember = streamingMember;
    }

    /**
     * @return
     */

    public java.io.InputStream getStreamingMember() {
        return this.streamingMember;
    }

    /**
     * @param streamingMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public StreamingOutputOperationResult withStreamingMember(java.io.InputStream streamingMember) {
        setStreamingMember(streamingMember);
        return this;
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     *
     * @return A string representation of this object.
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getStreamingMember() != null)
            sb.append("StreamingMember: ").append(getStreamingMember());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof StreamingOutputOperationResult == false)
            return false;
        StreamingOutputOperationResult other = (StreamingOutputOperationResult) obj;
        if (other.getStreamingMember() == null ^ this.getStreamingMember() == null)
            return false;
        if (other.getStreamingMember() != null && other.getStreamingMember().equals(this.getStreamingMember()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getStreamingMember() == null) ? 0 : getStreamingMember().hashCode());
        return hashCode;
    }

    @Override
    public StreamingOutputOperationResult clone() {
        try {
            return (StreamingOutputOperationResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

}
