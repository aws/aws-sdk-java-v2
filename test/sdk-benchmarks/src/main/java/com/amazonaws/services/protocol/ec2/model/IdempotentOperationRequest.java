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
package com.amazonaws.services.protocol.ec2.model;

import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import java.io.Serializable;


import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.services.protocol.ec2.model.transform.IdempotentOperationRequestMarshaller;

/**
 * 
 */

public class IdempotentOperationRequest extends AmazonWebServiceRequest implements Serializable, Cloneable, DryRunSupportedRequest<IdempotentOperationRequest> {

    private String idempotencyToken;

    /**
     * @param idempotencyToken
     */

    public void setIdempotencyToken(String idempotencyToken) {
        this.idempotencyToken = idempotencyToken;
    }

    /**
     * @return
     */

    public String getIdempotencyToken() {
        return this.idempotencyToken;
    }

    /**
     * @param idempotencyToken
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public IdempotentOperationRequest withIdempotencyToken(String idempotencyToken) {
        setIdempotencyToken(idempotencyToken);
        return this;
    }

    /**
     * This method is intended for internal use only. Returns the marshaled request configured with additional
     * parameters to enable operation dry-run.
     */
    @Override
    public Request<IdempotentOperationRequest> getDryRunRequest() {
        Request<IdempotentOperationRequest> request = new IdempotentOperationRequestMarshaller().marshall(this);
        request.addParameter("DryRun", Boolean.toString(true));
        return request;
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
        if (getIdempotencyToken() != null)
            sb.append("IdempotencyToken: ").append(getIdempotencyToken());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof IdempotentOperationRequest == false)
            return false;
        IdempotentOperationRequest other = (IdempotentOperationRequest) obj;
        if (other.getIdempotencyToken() == null ^ this.getIdempotencyToken() == null)
            return false;
        if (other.getIdempotencyToken() != null && other.getIdempotencyToken().equals(this.getIdempotencyToken()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getIdempotencyToken() == null) ? 0 : getIdempotencyToken().hashCode());
        return hashCode;
    }

    @Override
    public IdempotentOperationRequest clone() {
        return (IdempotentOperationRequest) super.clone();
    }
}
