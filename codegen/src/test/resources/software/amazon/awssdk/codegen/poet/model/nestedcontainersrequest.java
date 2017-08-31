package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class NestedContainersRequest extends AmazonWebServiceRequest implements
                                                                     ToCopyableBuilder<NestedContainersRequest.Builder, NestedContainersRequest> {
    private final List<List<String>> listOfListOfStrings;

    private final List<List<List<String>>> listOfListOfListOfStrings;

    private final Map<String, List<List<String>>> mapOfStringToListOfListOfStrings;

    private NestedContainersRequest(BuilderImpl builder) {
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
        hashCode = 31 * hashCode + ((listOfListOfStrings() == null) ? 0 : listOfListOfStrings().hashCode());
        hashCode = 31 * hashCode + ((listOfListOfListOfStrings() == null) ? 0 : listOfListOfListOfStrings().hashCode());
        hashCode = 31 * hashCode
                   + ((mapOfStringToListOfListOfStrings() == null) ? 0 : mapOfStringToListOfListOfStrings().hashCode());
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
        if (other.listOfListOfStrings() == null ^ this.listOfListOfStrings() == null) {
            return false;
        }
        if (other.listOfListOfStrings() != null && !other.listOfListOfStrings().equals(this.listOfListOfStrings())) {
            return false;
        }
        if (other.listOfListOfListOfStrings() == null ^ this.listOfListOfListOfStrings() == null) {
            return false;
        }
        if (other.listOfListOfListOfStrings() != null
            && !other.listOfListOfListOfStrings().equals(this.listOfListOfListOfStrings())) {
            return false;
        }
        if (other.mapOfStringToListOfListOfStrings() == null ^ this.mapOfStringToListOfListOfStrings() == null) {
            return false;
        }
        if (other.mapOfStringToListOfListOfStrings() != null
            && !other.mapOfStringToListOfListOfStrings().equals(this.mapOfStringToListOfListOfStrings())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (listOfListOfStrings() != null) {
            sb.append("ListOfListOfStrings: ").append(listOfListOfStrings()).append(",");
        }
        if (listOfListOfListOfStrings() != null) {
            sb.append("ListOfListOfListOfStrings: ").append(listOfListOfListOfStrings()).append(",");
        }
        if (mapOfStringToListOfListOfStrings() != null) {
            sb.append("MapOfStringToListOfListOfStrings: ").append(mapOfStringToListOfListOfStrings()).append(",");
        }
        String str = sb.toString();
        if (str.length() == 0) {
            return "{}";
        }
        return "{" + str.substring(0, str.length() - 1) + "}";
    }

    public interface Builder extends CopyableBuilder<Builder, NestedContainersRequest> {
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
    }

    private static final class BuilderImpl implements Builder {
        private List<List<String>> listOfListOfStrings;

        private List<List<List<String>>> listOfListOfListOfStrings;

        private Map<String, List<List<String>>> mapOfStringToListOfListOfStrings;

        private BuilderImpl() {
        }

        private BuilderImpl(NestedContainersRequest model) {
            setListOfListOfStrings(model.listOfListOfStrings);
            setListOfListOfListOfStrings(model.listOfListOfListOfStrings);
            setMapOfStringToListOfListOfStrings(model.mapOfStringToListOfListOfStrings);
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
        public NestedContainersRequest build() {
            return new NestedContainersRequest(this);
        }
    }
}
