package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import software.amazon.awssdk.AmazonWebServiceResult;
import software.amazon.awssdk.ResponseMetadata;
import software.amazon.awssdk.runtime.StandardMemberCopier;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class AllTypesResponse extends AmazonWebServiceResult<ResponseMetadata> implements
        ToCopyableBuilder<AllTypesResponse.Builder, AllTypesResponse> {
    private final String stringMember;

    private final Integer integerMember;

    private final Boolean booleanMember;

    private final Float floatMember;

    private final Double doubleMember;

    private final Long longMember;

    private final List<String> simpleList;

    private final List<Map<String, String>> listOfMaps;

    private final List<SimpleStruct> listOfStructs;

    private final Map<String, List<Integer>> mapOfStringToIntegerList;

    private final Map<String, String> mapOfStringToString;

    private final Map<String, SimpleStruct> mapOfStringToStruct;

    private final Date timestampMember;

    private final StructWithTimestamp structWithNestedTimestampMember;

    private final ByteBuffer blobArg;

    private final StructWithNestedBlobType structWithNestedBlob;

    private final Map<String, ByteBuffer> blobMap;

    private final List<ByteBuffer> listOfBlobs;

    private final RecursiveStructType recursiveStruct;

    private final BaseType polymorphicTypeWithSubTypes;

    private final SubTypeOne polymorphicTypeWithoutSubTypes;

    private AllTypesResponse(BuilderImpl builder) {
        this.stringMember = builder.stringMember;
        this.integerMember = builder.integerMember;
        this.booleanMember = builder.booleanMember;
        this.floatMember = builder.floatMember;
        this.doubleMember = builder.doubleMember;
        this.longMember = builder.longMember;
        this.simpleList = builder.simpleList;
        this.listOfMaps = builder.listOfMaps;
        this.listOfStructs = builder.listOfStructs;
        this.mapOfStringToIntegerList = builder.mapOfStringToIntegerList;
        this.mapOfStringToString = builder.mapOfStringToString;
        this.mapOfStringToStruct = builder.mapOfStringToStruct;
        this.timestampMember = builder.timestampMember;
        this.structWithNestedTimestampMember = builder.structWithNestedTimestampMember;
        this.blobArg = builder.blobArg;
        this.structWithNestedBlob = builder.structWithNestedBlob;
        this.blobMap = builder.blobMap;
        this.listOfBlobs = builder.listOfBlobs;
        this.recursiveStruct = builder.recursiveStruct;
        this.polymorphicTypeWithSubTypes = builder.polymorphicTypeWithSubTypes;
        this.polymorphicTypeWithoutSubTypes = builder.polymorphicTypeWithoutSubTypes;
    }

    /**
     *
     * @return
     */
    public String stringMember() {
        return stringMember;
    }

    /**
     *
     * @return
     */
    public Integer integerMember() {
        return integerMember;
    }

    /**
     *
     * @return
     */
    public Boolean booleanMember() {
        return booleanMember;
    }

    /**
     *
     * @return
     */
    public Float floatMember() {
        return floatMember;
    }

    /**
     *
     * @return
     */
    public Double doubleMember() {
        return doubleMember;
    }

    /**
     *
     * @return
     */
    public Long longMember() {
        return longMember;
    }

    /**
     *
     * @return
     */
    public List<String> simpleList() {
        return simpleList;
    }

    /**
     *
     * @return
     */
    public List<Map<String, String>> listOfMaps() {
        return listOfMaps;
    }

    /**
     *
     * @return
     */
    public List<SimpleStruct> listOfStructs() {
        return listOfStructs;
    }

    /**
     *
     * @return
     */
    public Map<String, List<Integer>> mapOfStringToIntegerList() {
        return mapOfStringToIntegerList;
    }

    /**
     *
     * @return
     */
    public Map<String, String> mapOfStringToString() {
        return mapOfStringToString;
    }

    /**
     *
     * @return
     */
    public Map<String, SimpleStruct> mapOfStringToStruct() {
        return mapOfStringToStruct;
    }

    /**
     *
     * @return
     */
    public Date timestampMember() {
        return timestampMember;
    }

    /**
     *
     * @return
     */
    public StructWithTimestamp structWithNestedTimestampMember() {
        return structWithNestedTimestampMember;
    }

    /**
     *
     * <p>
     * {@code ByteBuffer}s are stateful. Calling their {@code get} methods changes their {@code position}. We recommend
     * using {@link java.nio.ByteBuffer#asReadOnlyBuffer()} to create a read-only view of the buffer with an independent
     * {@code position}, and calling {@code get} methods on this rather than directly on the returned {@code ByteBuffer}
     * . Doing so will ensure that anyone else using the {@code ByteBuffer} will not be affected by changes to the
     * {@code position}.
     * </p>
     * 
     * @return
     */
    public ByteBuffer blobArg() {
        return blobArg;
    }

    /**
     *
     * @return
     */
    public StructWithNestedBlobType structWithNestedBlob() {
        return structWithNestedBlob;
    }

    /**
     *
     * @return
     */
    public Map<String, ByteBuffer> blobMap() {
        return blobMap;
    }

    /**
     *
     * @return
     */
    public List<ByteBuffer> listOfBlobs() {
        return listOfBlobs;
    }

    /**
     *
     * @return
     */
    public RecursiveStructType recursiveStruct() {
        return recursiveStruct;
    }

    /**
     *
     * @return
     */
    public BaseType polymorphicTypeWithSubTypes() {
        return polymorphicTypeWithSubTypes;
    }

    /**
     *
     * @return
     */
    public SubTypeOne polymorphicTypeWithoutSubTypes() {
        return polymorphicTypeWithoutSubTypes;
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
        hashCode = 31 * hashCode + ((stringMember() == null) ? 0 : stringMember().hashCode());
        hashCode = 31 * hashCode + ((integerMember() == null) ? 0 : integerMember().hashCode());
        hashCode = 31 * hashCode + ((booleanMember() == null) ? 0 : booleanMember().hashCode());
        hashCode = 31 * hashCode + ((floatMember() == null) ? 0 : floatMember().hashCode());
        hashCode = 31 * hashCode + ((doubleMember() == null) ? 0 : doubleMember().hashCode());
        hashCode = 31 * hashCode + ((longMember() == null) ? 0 : longMember().hashCode());
        hashCode = 31 * hashCode + ((simpleList() == null) ? 0 : simpleList().hashCode());
        hashCode = 31 * hashCode + ((listOfMaps() == null) ? 0 : listOfMaps().hashCode());
        hashCode = 31 * hashCode + ((listOfStructs() == null) ? 0 : listOfStructs().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToIntegerList() == null) ? 0 : mapOfStringToIntegerList().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToString() == null) ? 0 : mapOfStringToString().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToStruct() == null) ? 0 : mapOfStringToStruct().hashCode());
        hashCode = 31 * hashCode + ((timestampMember() == null) ? 0 : timestampMember().hashCode());
        hashCode = 31 * hashCode
                + ((structWithNestedTimestampMember() == null) ? 0 : structWithNestedTimestampMember().hashCode());
        hashCode = 31 * hashCode + ((blobArg() == null) ? 0 : blobArg().hashCode());
        hashCode = 31 * hashCode + ((structWithNestedBlob() == null) ? 0 : structWithNestedBlob().hashCode());
        hashCode = 31 * hashCode + ((blobMap() == null) ? 0 : blobMap().hashCode());
        hashCode = 31 * hashCode + ((listOfBlobs() == null) ? 0 : listOfBlobs().hashCode());
        hashCode = 31 * hashCode + ((recursiveStruct() == null) ? 0 : recursiveStruct().hashCode());
        hashCode = 31 * hashCode + ((polymorphicTypeWithSubTypes() == null) ? 0 : polymorphicTypeWithSubTypes().hashCode());
        hashCode = 31 * hashCode + ((polymorphicTypeWithoutSubTypes() == null) ? 0 : polymorphicTypeWithoutSubTypes().hashCode());
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
        if (!(obj instanceof AllTypesResponse)) {
            return false;
        }
        AllTypesResponse other = (AllTypesResponse) obj;
        if (other.stringMember() == null ^ this.stringMember() == null) {
            return false;
        }
        if (other.stringMember() != null && !other.stringMember().equals(this.stringMember())) {
            return false;
        }
        if (other.integerMember() == null ^ this.integerMember() == null) {
            return false;
        }
        if (other.integerMember() != null && !other.integerMember().equals(this.integerMember())) {
            return false;
        }
        if (other.booleanMember() == null ^ this.booleanMember() == null) {
            return false;
        }
        if (other.booleanMember() != null && !other.booleanMember().equals(this.booleanMember())) {
            return false;
        }
        if (other.floatMember() == null ^ this.floatMember() == null) {
            return false;
        }
        if (other.floatMember() != null && !other.floatMember().equals(this.floatMember())) {
            return false;
        }
        if (other.doubleMember() == null ^ this.doubleMember() == null) {
            return false;
        }
        if (other.doubleMember() != null && !other.doubleMember().equals(this.doubleMember())) {
            return false;
        }
        if (other.longMember() == null ^ this.longMember() == null) {
            return false;
        }
        if (other.longMember() != null && !other.longMember().equals(this.longMember())) {
            return false;
        }
        if (other.simpleList() == null ^ this.simpleList() == null) {
            return false;
        }
        if (other.simpleList() != null && !other.simpleList().equals(this.simpleList())) {
            return false;
        }
        if (other.listOfMaps() == null ^ this.listOfMaps() == null) {
            return false;
        }
        if (other.listOfMaps() != null && !other.listOfMaps().equals(this.listOfMaps())) {
            return false;
        }
        if (other.listOfStructs() == null ^ this.listOfStructs() == null) {
            return false;
        }
        if (other.listOfStructs() != null && !other.listOfStructs().equals(this.listOfStructs())) {
            return false;
        }
        if (other.mapOfStringToIntegerList() == null ^ this.mapOfStringToIntegerList() == null) {
            return false;
        }
        if (other.mapOfStringToIntegerList() != null && !other.mapOfStringToIntegerList().equals(this.mapOfStringToIntegerList())) {
            return false;
        }
        if (other.mapOfStringToString() == null ^ this.mapOfStringToString() == null) {
            return false;
        }
        if (other.mapOfStringToString() != null && !other.mapOfStringToString().equals(this.mapOfStringToString())) {
            return false;
        }
        if (other.mapOfStringToStruct() == null ^ this.mapOfStringToStruct() == null) {
            return false;
        }
        if (other.mapOfStringToStruct() != null && !other.mapOfStringToStruct().equals(this.mapOfStringToStruct())) {
            return false;
        }
        if (other.timestampMember() == null ^ this.timestampMember() == null) {
            return false;
        }
        if (other.timestampMember() != null && !other.timestampMember().equals(this.timestampMember())) {
            return false;
        }
        if (other.structWithNestedTimestampMember() == null ^ this.structWithNestedTimestampMember() == null) {
            return false;
        }
        if (other.structWithNestedTimestampMember() != null
                && !other.structWithNestedTimestampMember().equals(this.structWithNestedTimestampMember())) {
            return false;
        }
        if (other.blobArg() == null ^ this.blobArg() == null) {
            return false;
        }
        if (other.blobArg() != null && !other.blobArg().equals(this.blobArg())) {
            return false;
        }
        if (other.structWithNestedBlob() == null ^ this.structWithNestedBlob() == null) {
            return false;
        }
        if (other.structWithNestedBlob() != null && !other.structWithNestedBlob().equals(this.structWithNestedBlob())) {
            return false;
        }
        if (other.blobMap() == null ^ this.blobMap() == null) {
            return false;
        }
        if (other.blobMap() != null && !other.blobMap().equals(this.blobMap())) {
            return false;
        }
        if (other.listOfBlobs() == null ^ this.listOfBlobs() == null) {
            return false;
        }
        if (other.listOfBlobs() != null && !other.listOfBlobs().equals(this.listOfBlobs())) {
            return false;
        }
        if (other.recursiveStruct() == null ^ this.recursiveStruct() == null) {
            return false;
        }
        if (other.recursiveStruct() != null && !other.recursiveStruct().equals(this.recursiveStruct())) {
            return false;
        }
        if (other.polymorphicTypeWithSubTypes() == null ^ this.polymorphicTypeWithSubTypes() == null) {
            return false;
        }
        if (other.polymorphicTypeWithSubTypes() != null
                && !other.polymorphicTypeWithSubTypes().equals(this.polymorphicTypeWithSubTypes())) {
            return false;
        }
        if (other.polymorphicTypeWithoutSubTypes() == null ^ this.polymorphicTypeWithoutSubTypes() == null) {
            return false;
        }
        if (other.polymorphicTypeWithoutSubTypes() != null
                && !other.polymorphicTypeWithoutSubTypes().equals(this.polymorphicTypeWithoutSubTypes())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (stringMember() != null) {
            sb.append("StringMember: ").append(stringMember()).append(",");
        }
        if (integerMember() != null) {
            sb.append("IntegerMember: ").append(integerMember()).append(",");
        }
        if (booleanMember() != null) {
            sb.append("BooleanMember: ").append(booleanMember()).append(",");
        }
        if (floatMember() != null) {
            sb.append("FloatMember: ").append(floatMember()).append(",");
        }
        if (doubleMember() != null) {
            sb.append("DoubleMember: ").append(doubleMember()).append(",");
        }
        if (longMember() != null) {
            sb.append("LongMember: ").append(longMember()).append(",");
        }
        if (simpleList() != null) {
            sb.append("SimpleList: ").append(simpleList()).append(",");
        }
        if (listOfMaps() != null) {
            sb.append("ListOfMaps: ").append(listOfMaps()).append(",");
        }
        if (listOfStructs() != null) {
            sb.append("ListOfStructs: ").append(listOfStructs()).append(",");
        }
        if (mapOfStringToIntegerList() != null) {
            sb.append("MapOfStringToIntegerList: ").append(mapOfStringToIntegerList()).append(",");
        }
        if (mapOfStringToString() != null) {
            sb.append("MapOfStringToString: ").append(mapOfStringToString()).append(",");
        }
        if (mapOfStringToStruct() != null) {
            sb.append("MapOfStringToStruct: ").append(mapOfStringToStruct()).append(",");
        }
        if (timestampMember() != null) {
            sb.append("TimestampMember: ").append(timestampMember()).append(",");
        }
        if (structWithNestedTimestampMember() != null) {
            sb.append("StructWithNestedTimestampMember: ").append(structWithNestedTimestampMember()).append(",");
        }
        if (blobArg() != null) {
            sb.append("BlobArg: ").append(blobArg()).append(",");
        }
        if (structWithNestedBlob() != null) {
            sb.append("StructWithNestedBlob: ").append(structWithNestedBlob()).append(",");
        }
        if (blobMap() != null) {
            sb.append("BlobMap: ").append(blobMap()).append(",");
        }
        if (listOfBlobs() != null) {
            sb.append("ListOfBlobs: ").append(listOfBlobs()).append(",");
        }
        if (recursiveStruct() != null) {
            sb.append("RecursiveStruct: ").append(recursiveStruct()).append(",");
        }
        if (polymorphicTypeWithSubTypes() != null) {
            sb.append("PolymorphicTypeWithSubTypes: ").append(polymorphicTypeWithSubTypes()).append(",");
        }
        if (polymorphicTypeWithoutSubTypes() != null) {
            sb.append("PolymorphicTypeWithoutSubTypes: ").append(polymorphicTypeWithoutSubTypes()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public interface Builder extends CopyableBuilder<Builder, AllTypesResponse> {
        /**
         *
         * @param stringMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringMember(String stringMember);

        /**
         *
         * @param integerMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder integerMember(Integer integerMember);

        /**
         *
         * @param booleanMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder booleanMember(Boolean booleanMember);

        /**
         *
         * @param floatMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder floatMember(Float floatMember);

        /**
         *
         * @param doubleMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder doubleMember(Double doubleMember);

        /**
         *
         * @param longMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder longMember(Long longMember);

        /**
         *
         * @param simpleList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder simpleList(Collection<String> simpleList);

        /**
         *
         * <p>
         * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
         * {@link #setSimpleList(java.util.Collection)} or {@link #withSimpleList(java.util.Collection)} if you want to
         * override the existing values.
         * </p>
         * 
         * @param simpleList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder simpleList(String... simpleList);

        /**
         *
         * @param listOfMaps
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMaps(Collection<Map<String, String>> listOfMaps);

        /**
         *
         * <p>
         * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
         * {@link #setListOfMaps(java.util.Collection)} or {@link #withListOfMaps(java.util.Collection)} if you want to
         * override the existing values.
         * </p>
         * 
         * @param listOfMaps
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMaps(Map<String, String>... listOfMaps);

        /**
         *
         * @param listOfStructs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfStructs(Collection<SimpleStruct> listOfStructs);

        /**
         *
         * <p>
         * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
         * {@link #setListOfStructs(java.util.Collection)} or {@link #withListOfStructs(java.util.Collection)} if you
         * want to override the existing values.
         * </p>
         * 
         * @param listOfStructs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfStructs(SimpleStruct... listOfStructs);

        /**
         *
         * @param mapOfStringToIntegerList
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToIntegerList(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerList);

        /**
         *
         * @param mapOfStringToString
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToString(Map<String, String> mapOfStringToString);

        /**
         *
         * @param mapOfStringToStruct
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToStruct(Map<String, SimpleStruct> mapOfStringToStruct);

        /**
         *
         * @param timestampMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder timestampMember(Date timestampMember);

        /**
         *
         * @param structWithNestedTimestampMember
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder structWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember);

        /**
         *
         * @param blobArg
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder blobArg(ByteBuffer blobArg);

        /**
         *
         * @param structWithNestedBlob
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder structWithNestedBlob(StructWithNestedBlobType structWithNestedBlob);

        /**
         *
         * @param blobMap
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder blobMap(Map<String, ByteBuffer> blobMap);

        /**
         *
         * @param listOfBlobs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfBlobs(Collection<ByteBuffer> listOfBlobs);

        /**
         *
         * <p>
         * <b>NOTE:</b> This method appends the values to the existing list (if any). Use
         * {@link #setListOfBlobs(java.util.Collection)} or {@link #withListOfBlobs(java.util.Collection)} if you want
         * to override the existing values.
         * </p>
         * 
         * @param listOfBlobs
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfBlobs(ByteBuffer... listOfBlobs);

        /**
         *
         * @param recursiveStruct
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveStruct(RecursiveStructType recursiveStruct);

        /**
         *
         * @param polymorphicTypeWithSubTypes
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder polymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes);

        /**
         *
         * @param polymorphicTypeWithoutSubTypes
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder polymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes);
    }

    private static final class BuilderImpl implements Builder {
        private String stringMember;

        private Integer integerMember;

        private Boolean booleanMember;

        private Float floatMember;

        private Double doubleMember;

        private Long longMember;

        private List<String> simpleList;

        private List<Map<String, String>> listOfMaps;

        private List<SimpleStruct> listOfStructs;

        private Map<String, List<Integer>> mapOfStringToIntegerList;

        private Map<String, String> mapOfStringToString;

        private Map<String, SimpleStruct> mapOfStringToStruct;

        private Date timestampMember;

        private StructWithTimestamp structWithNestedTimestampMember;

        private ByteBuffer blobArg;

        private StructWithNestedBlobType structWithNestedBlob;

        private Map<String, ByteBuffer> blobMap;

        private List<ByteBuffer> listOfBlobs;

        private RecursiveStructType recursiveStruct;

        private BaseType polymorphicTypeWithSubTypes;

        private SubTypeOne polymorphicTypeWithoutSubTypes;

        private BuilderImpl() {
        }

        private BuilderImpl(AllTypesResponse model) {
            setStringMember(model.stringMember);
            setIntegerMember(model.integerMember);
            setBooleanMember(model.booleanMember);
            setFloatMember(model.floatMember);
            setDoubleMember(model.doubleMember);
            setLongMember(model.longMember);
            setSimpleList(model.simpleList);
            setListOfMaps(model.listOfMaps);
            setListOfStructs(model.listOfStructs);
            setMapOfStringToIntegerList(model.mapOfStringToIntegerList);
            setMapOfStringToString(model.mapOfStringToString);
            setMapOfStringToStruct(model.mapOfStringToStruct);
            setTimestampMember(model.timestampMember);
            setStructWithNestedTimestampMember(model.structWithNestedTimestampMember);
            setBlobArg(model.blobArg);
            setStructWithNestedBlob(model.structWithNestedBlob);
            setBlobMap(model.blobMap);
            setListOfBlobs(model.listOfBlobs);
            setRecursiveStruct(model.recursiveStruct);
            setPolymorphicTypeWithSubTypes(model.polymorphicTypeWithSubTypes);
            setPolymorphicTypeWithoutSubTypes(model.polymorphicTypeWithoutSubTypes);
        }

        public final String getStringMember() {
            return stringMember;
        }

        @Override
        public final Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        public final void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        public final Integer getIntegerMember() {
            return integerMember;
        }

        @Override
        public final Builder integerMember(Integer integerMember) {
            this.integerMember = integerMember;
            return this;
        }

        public final void setIntegerMember(Integer integerMember) {
            this.integerMember = integerMember;
        }

        public final Boolean getBooleanMember() {
            return booleanMember;
        }

        @Override
        public final Builder booleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
            return this;
        }

        public final void setBooleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
        }

        public final Float getFloatMember() {
            return floatMember;
        }

        @Override
        public final Builder floatMember(Float floatMember) {
            this.floatMember = floatMember;
            return this;
        }

        public final void setFloatMember(Float floatMember) {
            this.floatMember = floatMember;
        }

        public final Double getDoubleMember() {
            return doubleMember;
        }

        @Override
        public final Builder doubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
            return this;
        }

        public final void setDoubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
        }

        public final Long getLongMember() {
            return longMember;
        }

        @Override
        public final Builder longMember(Long longMember) {
            this.longMember = longMember;
            return this;
        }

        public final void setLongMember(Long longMember) {
            this.longMember = longMember;
        }

        public final Collection<String> getSimpleList() {
            return simpleList;
        }

        @Override
        public final Builder simpleList(Collection<String> simpleList) {
            this.simpleList = ListOfStringsCopier.copy(simpleList);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder simpleList(String... simpleList) {
            if (this.simpleList == null) {
                this.simpleList = new ArrayList<>(simpleList.length);
            }
            for (String e : simpleList) {
                this.simpleList.add(e);
            }
            return this;
        }

        public final void setSimpleList(Collection<String> simpleList) {
            this.simpleList = ListOfStringsCopier.copy(simpleList);
        }

        @SafeVarargs
        public final void setSimpleList(String... simpleList) {
            if (this.simpleList == null) {
                this.simpleList = new ArrayList<>(simpleList.length);
            }
            for (String e : simpleList) {
                this.simpleList.add(e);
            }
        }

        public final Collection<Map<String, String>> getListOfMaps() {
            return listOfMaps;
        }

        @Override
        public final Builder listOfMaps(Collection<Map<String, String>> listOfMaps) {
            this.listOfMaps = ListOfMapStringToStringCopier.copy(listOfMaps);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfMaps(Map<String, String>... listOfMaps) {
            if (this.listOfMaps == null) {
                this.listOfMaps = new ArrayList<>(listOfMaps.length);
            }
            for (Map<String, String> e : listOfMaps) {
                this.listOfMaps.add(MapOfStringToStringCopier.copy(e));
            }
            return this;
        }

        public final void setListOfMaps(Collection<Map<String, String>> listOfMaps) {
            this.listOfMaps = ListOfMapStringToStringCopier.copy(listOfMaps);
        }

        @SafeVarargs
        public final void setListOfMaps(Map<String, String>... listOfMaps) {
            if (this.listOfMaps == null) {
                this.listOfMaps = new ArrayList<>(listOfMaps.length);
            }
            for (Map<String, String> e : listOfMaps) {
                this.listOfMaps.add(MapOfStringToStringCopier.copy(e));
            }
        }

        public final Collection<SimpleStruct> getListOfStructs() {
            return listOfStructs;
        }

        @Override
        public final Builder listOfStructs(Collection<SimpleStruct> listOfStructs) {
            this.listOfStructs = ListOfSimpleStructsCopier.copy(listOfStructs);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfStructs(SimpleStruct... listOfStructs) {
            if (this.listOfStructs == null) {
                this.listOfStructs = new ArrayList<>(listOfStructs.length);
            }
            for (SimpleStruct e : listOfStructs) {
                this.listOfStructs.add(e);
            }
            return this;
        }

        public final void setListOfStructs(Collection<SimpleStruct> listOfStructs) {
            this.listOfStructs = ListOfSimpleStructsCopier.copy(listOfStructs);
        }

        @SafeVarargs
        public final void setListOfStructs(SimpleStruct... listOfStructs) {
            if (this.listOfStructs == null) {
                this.listOfStructs = new ArrayList<>(listOfStructs.length);
            }
            for (SimpleStruct e : listOfStructs) {
                this.listOfStructs.add(e);
            }
        }

        public final Map<String, ? extends Collection<Integer>> getMapOfStringToIntegerList() {
            return mapOfStringToIntegerList;
        }

        @Override
        public final Builder mapOfStringToIntegerList(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerList) {
            this.mapOfStringToIntegerList = MapOfStringToIntegerListCopier.copy(mapOfStringToIntegerList);
            return this;
        }

        public final void setMapOfStringToIntegerList(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerList) {
            this.mapOfStringToIntegerList = MapOfStringToIntegerListCopier.copy(mapOfStringToIntegerList);
        }

        public final Map<String, String> getMapOfStringToString() {
            return mapOfStringToString;
        }

        @Override
        public final Builder mapOfStringToString(Map<String, String> mapOfStringToString) {
            this.mapOfStringToString = MapOfStringToStringCopier.copy(mapOfStringToString);
            return this;
        }

        public final void setMapOfStringToString(Map<String, String> mapOfStringToString) {
            this.mapOfStringToString = MapOfStringToStringCopier.copy(mapOfStringToString);
        }

        public final Map<String, SimpleStruct> getMapOfStringToStruct() {
            return mapOfStringToStruct;
        }

        @Override
        public final Builder mapOfStringToStruct(Map<String, SimpleStruct> mapOfStringToStruct) {
            this.mapOfStringToStruct = MapOfStringToSimpleStructCopier.copy(mapOfStringToStruct);
            return this;
        }

        public final void setMapOfStringToStruct(Map<String, SimpleStruct> mapOfStringToStruct) {
            this.mapOfStringToStruct = MapOfStringToSimpleStructCopier.copy(mapOfStringToStruct);
        }

        public final Date getTimestampMember() {
            return timestampMember;
        }

        @Override
        public final Builder timestampMember(Date timestampMember) {
            this.timestampMember = StandardMemberCopier.copy(timestampMember);
            return this;
        }

        public final void setTimestampMember(Date timestampMember) {
            this.timestampMember = StandardMemberCopier.copy(timestampMember);
        }

        public final StructWithTimestamp getStructWithNestedTimestampMember() {
            return structWithNestedTimestampMember;
        }

        @Override
        public final Builder structWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
            this.structWithNestedTimestampMember = structWithNestedTimestampMember;
            return this;
        }

        public final void setStructWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
            this.structWithNestedTimestampMember = structWithNestedTimestampMember;
        }

        public final ByteBuffer getBlobArg() {
            return blobArg;
        }

        @Override
        public final Builder blobArg(ByteBuffer blobArg) {
            this.blobArg = StandardMemberCopier.copy(blobArg);
            return this;
        }

        public final void setBlobArg(ByteBuffer blobArg) {
            this.blobArg = StandardMemberCopier.copy(blobArg);
        }

        public final StructWithNestedBlobType getStructWithNestedBlob() {
            return structWithNestedBlob;
        }

        @Override
        public final Builder structWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
            this.structWithNestedBlob = structWithNestedBlob;
            return this;
        }

        public final void setStructWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
            this.structWithNestedBlob = structWithNestedBlob;
        }

        public final Map<String, ByteBuffer> getBlobMap() {
            return blobMap;
        }

        @Override
        public final Builder blobMap(Map<String, ByteBuffer> blobMap) {
            this.blobMap = BlobMapTypeCopier.copy(blobMap);
            return this;
        }

        public final void setBlobMap(Map<String, ByteBuffer> blobMap) {
            this.blobMap = BlobMapTypeCopier.copy(blobMap);
        }

        public final Collection<ByteBuffer> getListOfBlobs() {
            return listOfBlobs;
        }

        @Override
        public final Builder listOfBlobs(Collection<ByteBuffer> listOfBlobs) {
            this.listOfBlobs = ListOfBlobsTypeCopier.copy(listOfBlobs);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfBlobs(ByteBuffer... listOfBlobs) {
            if (this.listOfBlobs == null) {
                this.listOfBlobs = new ArrayList<>(listOfBlobs.length);
            }
            for (ByteBuffer e : listOfBlobs) {
                this.listOfBlobs.add(StandardMemberCopier.copy(e));
            }
            return this;
        }

        public final void setListOfBlobs(Collection<ByteBuffer> listOfBlobs) {
            this.listOfBlobs = ListOfBlobsTypeCopier.copy(listOfBlobs);
        }

        @SafeVarargs
        public final void setListOfBlobs(ByteBuffer... listOfBlobs) {
            if (this.listOfBlobs == null) {
                this.listOfBlobs = new ArrayList<>(listOfBlobs.length);
            }
            for (ByteBuffer e : listOfBlobs) {
                this.listOfBlobs.add(StandardMemberCopier.copy(e));
            }
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

        public final BaseType getPolymorphicTypeWithSubTypes() {
            return polymorphicTypeWithSubTypes;
        }

        @Override
        public final Builder polymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes;
            return this;
        }

        public final void setPolymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes;
        }

        public final SubTypeOne getPolymorphicTypeWithoutSubTypes() {
            return polymorphicTypeWithoutSubTypes;
        }

        @Override
        public final Builder polymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes;
            return this;
        }

        public final void setPolymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes;
        }

        @Override
        public AllTypesResponse build() {
            return new AllTypesResponse(this);
        }
    }
}
