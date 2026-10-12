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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.List;
import java.util.Objects;

/**
 * IT-only record for createTable limit validation tests requiring many indexed attributes.
 */
public class VectorCreateTableLimitRecord {
    private String pk;
    private String sk;
    private List<Float> embeddingCosine;
    private final String[] filterAttributes = new String[19];
    private final String[] projectionAttributes = new String[21];

    public String getPk() {
        return pk;
    }

    public VectorCreateTableLimitRecord setPk(String pk) {
        this.pk = pk;
        return this;
    }

    public String getSk() {
        return sk;
    }

    public VectorCreateTableLimitRecord setSk(String sk) {
        this.sk = sk;
        return this;
    }

    public List<Float> getEmbeddingCosine() {
        return embeddingCosine;
    }

    public VectorCreateTableLimitRecord setEmbeddingCosine(List<Float> embeddingCosine) {
        this.embeddingCosine = embeddingCosine;
        return this;
    }

    public String getFilterAttribute(int index) {
        return filterAttributes[index];
    }

    public VectorCreateTableLimitRecord setFilterAttribute(int index, String value) {
        filterAttributes[index] = value;
        return this;
    }

    public String getProjectionAttribute(int index) {
        return projectionAttributes[index];
    }

    public VectorCreateTableLimitRecord setProjectionAttribute(int index, String value) {
        projectionAttributes[index] = value;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VectorCreateTableLimitRecord that = (VectorCreateTableLimitRecord) o;
        return Objects.equals(pk, that.pk)
               && Objects.equals(sk, that.sk)
               && Objects.equals(embeddingCosine, that.embeddingCosine);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk, sk, embeddingCosine);
    }
}
