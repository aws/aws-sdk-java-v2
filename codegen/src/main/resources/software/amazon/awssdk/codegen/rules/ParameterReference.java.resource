import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class ParameterReference implements ToParameterReference {
    private final String name;
    private final String context;

    private ParameterReference(Builder builder) {
        this.name = builder.name;
        this.context = builder.context;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getContext() {
        return Optional.ofNullable(context);
    }

    public static ParameterReference from(String reference) {
        String[] split = reference.split("\\.", 2);
        return from(split[0], split.length == 2 ? split[1] : null);
    }

    public static ParameterReference from(String name, String context) {
        Builder builder = builder().name(name);
        if (context != null) {
            builder.context(context);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ParameterReference toParameterReference() {
        return this;
    }

    @Override
    public String toString() {
        if (context == null) {
            return name;
        }
        return name + "." + context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParameterReference that = (ParameterReference) o;
        return getName().equals(that.getName()) && Objects.equals(getContext(), that.getContext());
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (context != null ? context.hashCode() : 0);
        return result;
    }

    public static class Builder {
        private String name;
        private String context;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        public ParameterReference build() {
            return new ParameterReference(this);
        }
    }
}
