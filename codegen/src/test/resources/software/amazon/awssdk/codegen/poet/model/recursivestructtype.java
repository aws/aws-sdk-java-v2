package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.protocol.ProtocolMarshaller;
import software.amazon.awssdk.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.RecursiveStructTypeMarshaller;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class RecursiveStructType implements StructuredPojo, ToCopyableBuilder<RecursiveStructType.Builder, RecursiveStructType> {
    private final String noRecurse;

    private final RecursiveStructType recursiveStruct;

    private final List<RecursiveStructType> recursiveList;

    private final Map<String, RecursiveStructType> recursiveMap;

    private RecursiveStructType(BuilderImpl builder) {
        this.noRecurse = builder.noRecurse;
        this.recursiveStruct = builder.recursiveStruct;
        this.recursiveList = builder.recursiveList;
        this.recursiveMap = builder.recursiveMap;
    }

    /**
     * Returns the value of the NoRecurse property for this object.
     *
     * @return The value of the NoRecurse property for this object.
     */
    public String noRecurse() {
        return noRecurse;
    }

    /**
     * Returns the value of the RecursiveStruct property for this object.
     *
     * @return The value of the RecursiveStruct property for this object.
     */
    public RecursiveStructType recursiveStruct() {
        return recursiveStruct;
    }

    /**
     * Returns the value of the RecursiveList property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the RecursiveList property for this object.
     */
    public List<RecursiveStructType> recursiveList() {
        return recursiveList;
    }

    /**
     * Returns the value of the RecursiveMap property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the RecursiveMap property for this object.
     */
    public Map<String, RecursiveStructType> recursiveMap() {
        return recursiveMap;
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
        hashCode = 31 * hashCode + ((noRecurse() == null) ? 0 : noRecurse().hashCode());
        hashCode = 31 * hashCode + ((recursiveStruct() == null) ? 0 : recursiveStruct().hashCode());
        hashCode = 31 * hashCode + ((recursiveList() == null) ? 0 : recursiveList().hashCode());
        hashCode = 31 * hashCode + ((recursiveMap() == null) ? 0 : recursiveMap().hashCode());
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
        if (!(obj instanceof RecursiveStructType)) {
            return false;
        }
        RecursiveStructType other = (RecursiveStructType) obj;
        if (other.noRecurse() == null ^ this.noRecurse() == null) {
            return false;
        }
        if (other.noRecurse() != null && !other.noRecurse().equals(this.noRecurse())) {
            return false;
        }
        if (other.recursiveStruct() == null ^ this.recursiveStruct() == null) {
            return false;
        }
        if (other.recursiveStruct() != null && !other.recursiveStruct().equals(this.recursiveStruct())) {
            return false;
        }
        if (other.recursiveList() == null ^ this.recursiveList() == null) {
            return false;
        }
        if (other.recursiveList() != null && !other.recursiveList().equals(this.recursiveList())) {
            return false;
        }
        if (other.recursiveMap() == null ^ this.recursiveMap() == null) {
            return false;
        }
        if (other.recursiveMap() != null && !other.recursiveMap().equals(this.recursiveMap())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (noRecurse() != null) {
            sb.append("NoRecurse: ").append(noRecurse()).append(",");
        }
        if (recursiveStruct() != null) {
            sb.append("RecursiveStruct: ").append(recursiveStruct()).append(",");
        }
        if (recursiveList() != null) {
            sb.append("RecursiveList: ").append(recursiveList()).append(",");
        }
        if (recursiveMap() != null) {
            sb.append("RecursiveMap: ").append(recursiveMap()).append(",");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        RecursiveStructTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, RecursiveStructType> {
        /**
         * Sets the value of the NoRecurse property for this object.
         *
         * @param noRecurse
         *        The new value for the NoRecurse property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder noRecurse(String noRecurse);

        /**
         * Sets the value of the RecursiveStruct property for this object.
         *
         * @param recursiveStruct
         *        The new value for the RecursiveStruct property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveStruct(RecursiveStructType recursiveStruct);

        /**
         * Sets the value of the RecursiveList property for this object.
         *
         * @param recursiveList
         *        The new value for the RecursiveList property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveList(Collection<RecursiveStructType> recursiveList);

        /**
         * Sets the value of the RecursiveList property for this object.
         *
         * @param recursiveList
         *        The new value for the RecursiveList property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveList(RecursiveStructType... recursiveList);

        /**
         * Sets the value of the RecursiveMap property for this object.
         *
         * @param recursiveMap
         *        The new value for the RecursiveMap property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveMap(Map<String, RecursiveStructType> recursiveMap);
    }

    private static final class BuilderImpl implements Builder {
        private String noRecurse;

        private RecursiveStructType recursiveStruct;

        private List<RecursiveStructType> recursiveList;

        private Map<String, RecursiveStructType> recursiveMap;

        private BuilderImpl() {
        }

        private BuilderImpl(RecursiveStructType model) {
            setNoRecurse(model.noRecurse);
            setRecursiveStruct(model.recursiveStruct);
            setRecursiveList(model.recursiveList);
            setRecursiveMap(model.recursiveMap);
        }

        public final String getNoRecurse() {
            return noRecurse;
        }

        @Override
        public final Builder noRecurse(String noRecurse) {
            this.noRecurse = noRecurse;
            return this;
        }

        public final void setNoRecurse(String noRecurse) {
            this.noRecurse = noRecurse;
        }

        public final RecursiveStructType getRecursiveStruct() {
            return recursiveStruct;
        }

        @Override
        public final Builder recursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = recursiveStruct;
            return this;
        }

        public final void setRecursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = recursiveStruct;
        }

        public final Collection<RecursiveStructType> getRecursiveList() {
            return recursiveList;
        }

        @Override
        public final Builder recursiveList(Collection<RecursiveStructType> recursiveList) {
            this.recursiveList = RecursiveListTypeCopier.copy(recursiveList);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder recursiveList(RecursiveStructType... recursiveList) {
            recursiveList(Arrays.asList(recursiveList));
            return this;
        }

        public final void setRecursiveList(Collection<RecursiveStructType> recursiveList) {
            this.recursiveList = RecursiveListTypeCopier.copy(recursiveList);
        }

        public final Map<String, RecursiveStructType> getRecursiveMap() {
            return recursiveMap;
        }

        @Override
        public final Builder recursiveMap(Map<String, RecursiveStructType> recursiveMap) {
            this.recursiveMap = RecursiveMapTypeCopier.copy(recursiveMap);
            return this;
        }

        public final void setRecursiveMap(Map<String, RecursiveStructType> recursiveMap) {
            this.recursiveMap = RecursiveMapTypeCopier.copy(recursiveMap);
        }

        @Override
        public RecursiveStructType build() {
            return new RecursiveStructType(this);
        }
    }
}
