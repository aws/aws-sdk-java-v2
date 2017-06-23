package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class NestedContainersResponse extends AmazonWebServiceResult<ResponseMetadata> implements
        ToCopyableBuilder<NestedContainersResponse.Builder, NestedContainersResponse> {
    private final List<List<String>> listOfListOfStrings;

    private final List<List<List<String>>> listOfListOfListOfStrings;

    private final Map<String, List<List<String>>> mapOfStringToListOfListOfStrings;

    private NestedContainersResponse(BuilderImpl builder) {
        this.listOfListOfStrings = builder.listOfListOfStrings;
        this.listOfListOfListOfStrings = builder.listOfListOfListOfStrings;
        this.mapOfStringToListOfListOfStrings = builder.mapOfStringToListOfListOfStrings;
    }

    /**
     *
     * @return
     */
    public List<List<String>> listOfListOfStrings() {
        return listOfListOfStrings;
    }

    /**
     *
     * @return
     */
    public List<List<List<String>>> listOfListOfListOfStrings() {
        return listOfListOfListOfStrings;
    }

    /**
     *
     * @return
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
        if (!(obj instanceof NestedContainersResponse)) {
            return false;
        }
        NestedContainersResponse other = (NestedContainersResponse) obj;
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
        sb.append("{");
        if (listOfListOfStrings() != null) {
            sb.append("ListOfListOfStrings: ").append(listOfListOfStrings()).append(",");
        }
        if (listOfListOfListOfStrings() != null) {
            sb.append("ListOfListOfListOfStrings: ").append(listOfListOfListOfStrings()).append(",");
        }
        if (mapOfStringToListOfListOfStrings() != null) {
            sb.append("MapOfStringToListOfListOfStrings: ").append(mapOfStringToListOfListOfStrings()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public interface Builder extends CopyableBuilder<Builder, NestedContainersResponse> {
        /**
         *
         * @param listOfListOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfStrings(Collection<? extends Collection<String>> listOfListOfStrings);

        /**
         *
         * <p>
         * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
         * {@link #setListOfListOfStrings(java.util.Collection)} or
         * {@link #withListOfListOfStrings(java.util.Collection)} if you want to override the existing values.
         * </p>
         * 
         * @param listOfListOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfStrings(Collection<String>... listOfListOfStrings);

        /**
         *
         * @param listOfListOfListOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListOfStrings(Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStrings);

        /**
         *
         * <p>
         * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
         * {@link #setListOfListOfListOfStrings(java.util.Collection)} or
         * {@link #withListOfListOfListOfStrings(java.util.Collection)} if you want to override the existing values.
         * </p>
         * 
         * @param listOfListOfListOfStrings
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListOfStrings(Collection<? extends Collection<String>>... listOfListOfListOfStrings);

        /**
         *
         * @param mapOfStringToListOfListOfStrings
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

        private BuilderImpl(NestedContainersResponse model) {
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
            if (this.listOfListOfStrings == null) {
                this.listOfListOfStrings = new ArrayList<>(listOfListOfStrings.length);
            }
            for (Collection<String> e : listOfListOfStrings) {
                this.listOfListOfStrings.add(ListOfStringsCopier.copy(e));
            }
            return this;
        }

        public final void setListOfListOfStrings(Collection<? extends Collection<String>> listOfListOfStrings) {
            this.listOfListOfStrings = ListOfListOfStringsCopier.copy(listOfListOfStrings);
        }

        @SafeVarargs
        public final void setListOfListOfStrings(Collection<String>... listOfListOfStrings) {
            if (this.listOfListOfStrings == null) {
                this.listOfListOfStrings = new ArrayList<>(listOfListOfStrings.length);
            }
            for (Collection<String> e : listOfListOfStrings) {
                this.listOfListOfStrings.add(ListOfStringsCopier.copy(e));
            }
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
            if (this.listOfListOfListOfStrings == null) {
                this.listOfListOfListOfStrings = new ArrayList<>(listOfListOfListOfStrings.length);
            }
            for (Collection<? extends Collection<String>> e : listOfListOfListOfStrings) {
                this.listOfListOfListOfStrings.add(ListOfListOfStringsCopier.copy(e));
            }
            return this;
        }

        public final void setListOfListOfListOfStrings(
                Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStrings) {
            this.listOfListOfListOfStrings = ListOfListOfListOfStringsCopier.copy(listOfListOfListOfStrings);
        }

        @SafeVarargs
        public final void setListOfListOfListOfStrings(Collection<? extends Collection<String>>... listOfListOfListOfStrings) {
            if (this.listOfListOfListOfStrings == null) {
                this.listOfListOfListOfStrings = new ArrayList<>(listOfListOfListOfStrings.length);
            }
            for (Collection<? extends Collection<String>> e : listOfListOfListOfStrings) {
                this.listOfListOfListOfStrings.add(ListOfListOfStringsCopier.copy(e));
            }
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
        public NestedContainersResponse build() {
            return new NestedContainersResponse(this);
        }
    }
}
