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


import com.amazonaws.AmazonWebServiceRequest;

/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/OperationWithExplicitPayloadBlob"
 *      target="_top">AWS API Documentation</a>
 */

public class OperationWithExplicitPayloadBlobRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private java.nio.ByteBuffer payloadMember;

    /**
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * <p>
     * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content or position of the byte buffer will
     * be seen by all objects that have a reference to this object. It is recommended to call ByteBuffer.duplicate() or
     * ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer. This behavior will be changed in a future
     * major version of the SDK.
     * </p>
     * 
     * @param payloadMember
     */

    public void setPayloadMember(java.nio.ByteBuffer payloadMember) {
        this.payloadMember = payloadMember;
    }

    /**
     * <p>
     * {@code ByteBuffer}s are stateful. Calling their {@code get} methods changes their {@code position}. We recommend
     * using {@link java.nio.ByteBuffer#asReadOnlyBuffer()} to create a read-only view of the buffer with an independent
     * {@code position}, and calling {@code get} methods on this rather than directly on the returned {@code ByteBuffer}
     * . Doing so will ensure that anyone else using the {@code ByteBuffer} will not be affected by changes to the
     * {@code position}.
     * </p>
     * 
     * @return
     */

    public java.nio.ByteBuffer getPayloadMember() {
        return this.payloadMember;
    }

    /**
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * <p>
     * Warning: ByteBuffers returned by the SDK are mutable. Changes to the content or position of the byte buffer will
     * be seen by all objects that have a reference to this object. It is recommended to call ByteBuffer.duplicate() or
     * ByteBuffer.asReadOnlyBuffer() before using or reading from the buffer. This behavior will be changed in a future
     * major version of the SDK.
     * </p>
     * 
     * @param payloadMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public OperationWithExplicitPayloadBlobRequest withPayloadMember(java.nio.ByteBuffer payloadMember) {
        setPayloadMember(payloadMember);
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
        if (getPayloadMember() != null)
            sb.append("PayloadMember: ").append(getPayloadMember());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof OperationWithExplicitPayloadBlobRequest == false)
            return false;
        OperationWithExplicitPayloadBlobRequest other = (OperationWithExplicitPayloadBlobRequest) obj;
        if (other.getPayloadMember() == null ^ this.getPayloadMember() == null)
            return false;
        if (other.getPayloadMember() != null && other.getPayloadMember().equals(this.getPayloadMember()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getPayloadMember() == null) ? 0 : getPayloadMember().hashCode());
        return hashCode;
    }

    @Override
    public OperationWithExplicitPayloadBlobRequest clone() {
        return (OperationWithExplicitPayloadBlobRequest) super.clone();
    }

}
