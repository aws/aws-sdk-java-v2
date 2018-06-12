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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.services.kinesis.transform.SubscribeToShardEventMarshaller;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class SubscribeToShardEvent implements StructuredPojo,
        ToCopyableBuilder<SubscribeToShardEvent.Builder, SubscribeToShardEvent>, SubscribeToShardBaseEvent {
    private final List<Record> records;

    private final String continuationSequenceNumber;

    private final Long millisBehindLatest;

    private SubscribeToShardEvent(BuilderImpl builder) {
        this.records = builder.records;
        this.continuationSequenceNumber = builder.continuationSequenceNumber;
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
     * Returns the value of the ContinuationSequenceNumber property for this object.
     * 
     * @return The value of the ContinuationSequenceNumber property for this object.
     */
    public String continuationSequenceNumber() {
        return continuationSequenceNumber;
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
        hashCode = 31 * hashCode + Objects.hashCode(records());
        hashCode = 31 * hashCode + Objects.hashCode(continuationSequenceNumber());
        hashCode = 31 * hashCode + Objects.hashCode(millisBehindLatest());
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
        if (!(obj instanceof SubscribeToShardEvent)) {
            return false;
        }
        SubscribeToShardEvent other = (SubscribeToShardEvent) obj;
        return Objects.equals(records(), other.records())
                && Objects.equals(continuationSequenceNumber(), other.continuationSequenceNumber())
                && Objects.equals(millisBehindLatest(), other.millisBehindLatest());
    }

    @Override
    public String toString() {
        return ToString.builder("SubscribeToShardEvent").add("Records", records())
                .add("ContinuationSequenceNumber", continuationSequenceNumber()).add("MillisBehindLatest", millisBehindLatest())
                .build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Records":
            return Optional.of(clazz.cast(records()));
        case "ContinuationSequenceNumber":
            return Optional.of(clazz.cast(continuationSequenceNumber()));
        case "MillisBehindLatest":
            return Optional.of(clazz.cast(millisBehindLatest()));
        default:
            return Optional.empty();
        }
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        SubscribeToShardEventMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.visit(this);
    }

    public interface Builder extends CopyableBuilder<Builder, SubscribeToShardEvent> {
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
         * Sets the value of the Records property for this object.
         *
         * This is a convenience that creates an instance of the {@link List<Record>.Builder} avoiding the need to
         * create one manually via {@link List<Record>#builder()}.
         *
         * When the {@link Consumer} completes, {@link List<Record>.Builder#build()} is called immediately and its
         * result is passed to {@link #records(List<Record>)}.
         * 
         * @param records
         *        a consumer that will call methods on {@link List<Record>.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #records(List<Record>)
         */
        Builder records(Consumer<Record.Builder>... records);

        /**
         * Sets the value of the ContinuationSequenceNumber property for this object.
         *
         * @param continuationSequenceNumber
         *        The new value for the ContinuationSequenceNumber property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder continuationSequenceNumber(String continuationSequenceNumber);

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
        private List<Record> records = DefaultSdkAutoConstructList.getInstance();

        private String continuationSequenceNumber;

        private Long millisBehindLatest;

        private BuilderImpl() {
        }

        private BuilderImpl(SubscribeToShardEvent model) {
            records(model.records);
            continuationSequenceNumber(model.continuationSequenceNumber);
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

        @Override
        @SafeVarargs
        public final Builder records(Consumer<Record.Builder>... records) {
            records(Stream.of(records).map(c -> Record.builder().apply(c).build()).collect(Collectors.toList()));
            return this;
        }

        public final void setRecords(Collection<Record.BuilderImpl> records) {
            this.records = RecordListCopier.copyFromBuilder(records);
        }

        public final String getContinuationSequenceNumber() {
            return continuationSequenceNumber;
        }

        @Override
        public final Builder continuationSequenceNumber(String continuationSequenceNumber) {
            this.continuationSequenceNumber = continuationSequenceNumber;
            return this;
        }

        public final void setContinuationSequenceNumber(String continuationSequenceNumber) {
            this.continuationSequenceNumber = continuationSequenceNumber;
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
        public SubscribeToShardEvent build() {
            return new SubscribeToShardEvent(this);
        }
    }
}
