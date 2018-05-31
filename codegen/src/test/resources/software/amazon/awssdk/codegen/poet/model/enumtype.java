package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Optional;
import javax.annotation.Generated;
import software.amazon.awssdk.utils.ToString;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class EnumType {
    private EnumType(EnumType.BuilderImpl builder) {
    }

    @Override
    public EnumType.Builder toBuilder() {
        return new EnumType.BuilderImpl(this);
    }

    public static EnumType.Builder builder() {
        return new EnumType.BuilderImpl();
    }

    public static Class<? extends EnumType.Builder> serializableBuilderClass() {
        return EnumType.BuilderImpl.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
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
        if (!(obj instanceof EnumType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return ToString.builder("EnumType").build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        return Optional.empty();
    }
}
