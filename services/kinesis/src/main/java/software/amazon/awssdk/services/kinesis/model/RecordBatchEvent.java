/*
 * Copyright 2013-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.kinesis.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.services.kinesis.transform.RecordBatchEventMarshaller;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class RecordBatchEvent implements StructuredPojo, ToCopyableBuilder<RecordBatchEvent.Builder, RecordBatchEvent> {
    private final List<Record> records;

    private final Long millisBehindLatest;

    private RecordBatchEvent(BuilderImpl builder) {
        this.records = builder.records;
        this.millisBehindLatest = builder.millisBehindLatest;
    }

    /**
     * Returns the value of the Records property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * 
     * @return The value of the Records property for this object.
     */
    public List<Record> records() {
        return records;
    }

    /**
     * Returns the value of the MillisBehindLatest property for this object.
     * 
     * @return The value of the MillisBehindLatest property for this object.
     */
    public Long millisBehindLatest() {
        return millisBehindLatest;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + ((records() == null) ? 0 : records().hashCode());
        hashCode = 31 * hashCode + ((millisBehindLatest() == null) ? 0 : millisBehindLatest().hashCode());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RecordBatchEvent)) {
            return false;
        }
        RecordBatchEvent other = (RecordBatchEvent) obj;
        if (other.records() == null ^ this.records() == null) {
            return false;
        }
        if (other.records() != null && !other.records().equals(this.records())) {
            return false;
        }
        if (other.millisBehindLatest() == null ^ this.millisBehindLatest() == null) {
            return false;
        }
        if (other.millisBehindLatest() != null && !other.millisBehindLatest().equals(this.millisBehindLatest())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (records() != null) {
            sb.append("Records: ").append(records()).append(",");
        }
        if (millisBehindLatest() != null) {
            sb.append("MillisBehindLatest: ").append(millisBehindLatest()).append(",");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Records":
            return Optional.of(clazz.cast(records()));
        case "MillisBehindLatest":
            return Optional.of(clazz.cast(millisBehindLatest()));
        default:
            return Optional.empty();
        }
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        RecordBatchEventMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, RecordBatchEvent> {
        /**
         * Sets the value of the Records property for this object.
         *
         * @param records
         *        The new value for the Records property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder records(Collection<Record> records);

        /**
         * Sets the value of the Records property for this object.
         *
         * @param records
         *        The new value for the Records property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder records(Record... records);

        /**
         * Sets the value of the MillisBehindLatest property for this object.
         *
         * @param millisBehindLatest
         *        The new value for the MillisBehindLatest property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder millisBehindLatest(Long millisBehindLatest);
    }

    static final class BuilderImpl implements Builder {
        private List<Record> records;

        private Long millisBehindLatest;

        private BuilderImpl() {
        }

        private BuilderImpl(RecordBatchEvent model) {
            records(model.records);
            millisBehindLatest(model.millisBehindLatest);
        }

        public final Collection<Record.Builder> getRecords() {
            return records != null ? records.stream().map(Record::toBuilder).collect(Collectors.toList()) : null;
        }

        @Override
        public final Builder records(Collection<Record> records) {
            this.records = RecordListCopier.copy(records);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder records(Record... records) {
            records(Arrays.asList(records));
            return this;
        }

        public final void setRecords(Collection<Record.BuilderImpl> records) {
            this.records = RecordListCopier.copyFromBuilder(records);
        }

        public final Long getMillisBehindLatest() {
            return millisBehindLatest;
        }

        @Override
        public final Builder millisBehindLatest(Long millisBehindLatest) {
            this.millisBehindLatest = millisBehindLatest;
            return this;
        }

        public final void setMillisBehindLatest(Long millisBehindLatest) {
            this.millisBehindLatest = millisBehindLatest;
        }

        @Override
        public RecordBatchEvent build() {
            return new RecordBatchEvent(this);
        }
    }
}
