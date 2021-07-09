package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class DocumentInputOperationRequest extends JsonProtocolTestsRequest implements
        ToCopyableBuilder<DocumentInputOperationRequest.Builder, DocumentInputOperationRequest> {
    private static final SdkField<Document> DOCUMENT_MEMBER_FIELD = SdkField
            .<Document> builder(MarshallingType.DOCUMENT)
            .memberName("DocumentMember")
            .getter(getter(DocumentInputOperationRequest::documentMember))
            .setter(setter(Builder::documentMember))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DocumentMember").build(),
                    PayloadTrait.create()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(DOCUMENT_MEMBER_FIELD));

    private final Document documentMember;

    private DocumentInputOperationRequest(BuilderImpl builder) {
        super(builder);
        this.documentMember = builder.documentMember;
    }

    /**
     * Returns the value of the DocumentMember property for this object.
     *
     * @return The value of the DocumentMember property for this object.
     */
    public final Document documentMember() {
        return documentMember;
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
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(documentMember());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj) && equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DocumentInputOperationRequest)) {
            return false;
        }
        DocumentInputOperationRequest other = (DocumentInputOperationRequest) obj;
        return Objects.equals(documentMember(), other.documentMember());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("DocumentInputOperationRequest").add("DocumentMember", documentMember()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "DocumentMember":
                return Optional.ofNullable(clazz.cast(documentMember()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DocumentInputOperationRequest, T> g) {
        return obj -> g.apply((DocumentInputOperationRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends JsonProtocolTestsRequest.Builder, SdkPojo,
            CopyableBuilder<Builder, DocumentInputOperationRequest> {
        /**
         * Sets the value of the DocumentMember property for this object.
         *
         * @param documentMember
         *        The new value for the DocumentMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder documentMember(Document documentMember);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends JsonProtocolTestsRequest.BuilderImpl implements Builder {
        private Document documentMember;

        private BuilderImpl() {
        }

        private BuilderImpl(DocumentInputOperationRequest model) {
            super(model);
            documentMember(model.documentMember);
        }

        public final Document getDocumentMember() {
            return documentMember;
        }

        @Override
        public final Builder documentMember(Document documentMember) {
            this.documentMember = documentMember;
            return this;
        }

        public final void setDocumentMember(Document documentMember) {
            this.documentMember = documentMember;
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            super.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
            super.overrideConfiguration(builderConsumer);
            return this;
        }

        @Override
        public DocumentInputOperationRequest build() {
            return new DocumentInputOperationRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}