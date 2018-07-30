package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class NestedContainersRequest extends JsonProtocolTestsRequest implements
        ToCopyableBuilder<NestedContainersRequest.Builder, NestedContainersRequest> {
    private final List<List<String>> listOfListOfStrings;

    private final List<List<List<String>>> listOfListOfListOfStrings;

    private final Map<String, List<List<String>>> mapOfStringToListOfListOfStrings;

    private NestedContainersRequest(BuilderImpl builder) {
        super(builder);
        this.listOfListOfStrings = builder.listOfListOfStrings;
        this.listOfListOfListOfStrings = builder.listOfListOfListOfStrings;
        this.mapOfStringToListOfListOfStrings = builder.mapOfStringToListOfListOfStrings;
    }

    /**
     * Returns the value of the ListOfListOfStrings property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the ListOfListOfStrings property for this object.
     */
    public List<List<String>> listOfListOfStrings() {
        return listOfListOfStrings;
    }

    /**
     * Returns the value of the ListOfListOfListOfStrings property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the ListOfListOfListOfStrings property for this object.
     */
    public List<List<List<String>>> listOfListOfListOfStrings() {
        return listOfListOfListOfStrings;
    }

    /**
     * Returns the value of the MapOfStringToListOfListOfStrings property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfStringToListOfListOfStrings property for this object.
     */
    public Map<String, List<List<String>>> mapOfStringToListOfListOfStrings() {
        return mapOfStringToListOfListOfStrings;
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
        hashCode = 31 * hashCode + Objects.hashCode(listOfListOfStrings());
        hashCode = 31 * hashCode + Objects.hashCode(listOfListOfListOfStrings());
        hashCode = 31 * hashCode + Objects.hashCode(mapOfStringToListOfListOfStrings());
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
        if (!(obj instanceof NestedContainersRequest)) {
            return false;
        }
        NestedContainersRequest other = (NestedContainersRequest) obj;
        return Objects.equals(listOfListOfStrings(), other.listOfListOfStrings())
                && Objects.equals(listOfListOfListOfStrings(), other.listOfListOfListOfStrings())
                && Objects.equals(mapOfStringToListOfListOfStrings(), other.mapOfStringToListOfListOfStrings());
    }

    @Override
    public String toString() {
        return ToString.builder("NestedContainersRequest").add("ListOfListOfStrings", listOfListOfStrings())
                .add("ListOfListOfListOfStrings", listOfListOfListOfStrings())
                .add("MapOfStringToListOfListOfStrings", mapOfStringToListOfListOfStrings()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "ListOfListOfStrings":
            return Optional.ofNullable(clazz.cast(listOfListOfStrings()));
        case "ListOfListOfListOfStrings":
            return Optional.ofNullable(clazz.cast(listOfListOfListOfStrings()));
        case "MapOfStringToListOfListOfStrings":
            return Optional.ofNullable(clazz.cast(mapOfStringToListOfListOfStrings()));
        default:
            return Optional.empty();
        }
    }

    public interface Builder extends JsonProtocolTestsRequest.Builder, CopyableBuilder<Builder, NestedContainersRequest> {
        /**
         * Sets the value of the ListOfListOfStrings property for this object.
         *
         * @param listOfListOfStrings
         *        The new value for the ListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfStrings(Collection<? extends Collection<String>> listOfListOfStrings);

        /**
         * Sets the value of the ListOfListOfStrings property for this object.
         *
         * @param listOfListOfStrings
         *        The new value for the ListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfStrings(Collection<String>... listOfListOfStrings);

        /**
         * Sets the value of the ListOfListOfListOfStrings property for this object.
         *
         * @param listOfListOfListOfStrings
         *        The new value for the ListOfListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListOfStrings(Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStrings);

        /**
         * Sets the value of the ListOfListOfListOfStrings property for this object.
         *
         * @param listOfListOfListOfStrings
         *        The new value for the ListOfListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListOfStrings(Collection<? extends Collection<String>>... listOfListOfListOfStrings);

        /**
         * Sets the value of the MapOfStringToListOfListOfStrings property for this object.
         *
         * @param mapOfStringToListOfListOfStrings
         *        The new value for the MapOfStringToListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToListOfListOfStrings(
                Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStrings);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends JsonProtocolTestsRequest.BuilderImpl implements Builder {
        private List<List<String>> listOfListOfStrings;

        private List<List<List<String>>> listOfListOfListOfStrings;

        private Map<String, List<List<String>>> mapOfStringToListOfListOfStrings;

        private BuilderImpl() {
        }

        private BuilderImpl(NestedContainersRequest model) {
            super(model);
            listOfListOfStrings(model.listOfListOfStrings);
            listOfListOfListOfStrings(model.listOfListOfListOfStrings);
            mapOfStringToListOfListOfStrings(model.mapOfStringToListOfListOfStrings);
        }

        public final Collection<? extends Collection<String>> getListOfListOfStrings() {
            return listOfListOfStrings;
        }

        @Override
        public final Builder listOfListOfStrings(Collection<? extends Collection<String>> listOfListOfStrings) {
            this.listOfListOfStrings = ListOfListOfStringsCopier.copy(listOfListOfStrings);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfListOfStrings(Collection<String>... listOfListOfStrings) {
            listOfListOfStrings(Arrays.asList(listOfListOfStrings));
            return this;
        }

        public final void setListOfListOfStrings(Collection<? extends Collection<String>> listOfListOfStrings) {
            this.listOfListOfStrings = ListOfListOfStringsCopier.copy(listOfListOfStrings);
        }

        public final Collection<? extends Collection<? extends Collection<String>>> getListOfListOfListOfStrings() {
            return listOfListOfListOfStrings;
        }

        @Override
        public final Builder listOfListOfListOfStrings(
                Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStrings) {
            this.listOfListOfListOfStrings = ListOfListOfListOfStringsCopier.copy(listOfListOfListOfStrings);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfListOfListOfStrings(Collection<? extends Collection<String>>... listOfListOfListOfStrings) {
            listOfListOfListOfStrings(Arrays.asList(listOfListOfListOfStrings));
            return this;
        }

        public final void setListOfListOfListOfStrings(
                Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStrings) {
            this.listOfListOfListOfStrings = ListOfListOfListOfStringsCopier.copy(listOfListOfListOfStrings);
        }

        public final Map<String, ? extends Collection<? extends Collection<String>>> getMapOfStringToListOfListOfStrings() {
            return mapOfStringToListOfListOfStrings;
        }

        @Override
        public final Builder mapOfStringToListOfListOfStrings(
                Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStrings) {
            this.mapOfStringToListOfListOfStrings = MapOfStringToListOfListOfStringsCopier.copy(mapOfStringToListOfListOfStrings);
            return this;
        }

        public final void setMapOfStringToListOfListOfStrings(
                Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStrings) {
            this.mapOfStringToListOfListOfStrings = MapOfStringToListOfListOfStringsCopier.copy(mapOfStringToListOfListOfStrings);
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
        public NestedContainersRequest build() {
            return new NestedContainersRequest(this);
        }
    }
}
