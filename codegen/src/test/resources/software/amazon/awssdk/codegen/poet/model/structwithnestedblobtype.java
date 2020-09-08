package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.adapter.StandardMemberCopier;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class StructWithNestedBlobType implements SdkPojo, Serializable,
                                                       ToCopyableBuilder<StructWithNestedBlobType.Builder, StructWithNestedBlobType> {
    private static final SdkField<SdkBytes> NESTED_BLOB_FIELD = SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
        .memberName("NestedBlob").getter(getter(StructWithNestedBlobType::nestedBlob)).setter(setter(Builder::nestedBlob))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NestedBlob").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NESTED_BLOB_FIELD));

    private static final long serialVersionUID = 1L;

    private final SdkBytes nestedBlob;

    private StructWithNestedBlobType(BuilderImpl builder) {
        this.nestedBlob = builder.nestedBlob;
    }

    /**
     * Returns the value of the NestedBlob property for this object.
     *
     * @return The value of the NestedBlob property for this object.
     */
    public SdkBytes nestedBlob() {
        return nestedBlob;
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
        hashCode = 31 * hashCode + Objects.hashCode(nestedBlob());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof StructWithNestedBlobType)) {
            return false;
        }
        StructWithNestedBlobType other = (StructWithNestedBlobType) obj;
        return Objects.equals(nestedBlob(), other.nestedBlob());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("StructWithNestedBlobType").add("NestedBlob", nestedBlob()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "NestedBlob":
                return Optional.ofNullable(clazz.cast(nestedBlob()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<StructWithNestedBlobType, T> g) {
        return obj -> g.apply((StructWithNestedBlobType) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, StructWithNestedBlobType> {
        /**
         * Sets the value of the NestedBlob property for this object.
         *
         * @param nestedBlob
         *        The new value for the NestedBlob property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedBlob(SdkBytes nestedBlob);
    }

    static final class BuilderImpl implements Builder {
        private SdkBytes nestedBlob;

        private BuilderImpl() {
        }

        private BuilderImpl(StructWithNestedBlobType model) {
            nestedBlob(model.nestedBlob);
        }

        public final ByteBuffer getNestedBlob() {
            return nestedBlob == null ? null : nestedBlob.asByteBuffer();
        }

        @Override
        public final Builder nestedBlob(SdkBytes nestedBlob) {
            this.nestedBlob = StandardMemberCopier.copy(nestedBlob);
            return this;
        }

        public final void setNestedBlob(ByteBuffer nestedBlob) {
            nestedBlob(nestedBlob == null ? null : SdkBytes.fromByteBuffer(nestedBlob));
        }

        @Override
        public StructWithNestedBlobType build() {
            return new StructWithNestedBlobType(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
