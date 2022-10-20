import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Identifiers for variables declared within the rule engine, e.g. from an {@code assign} statement.
 */
@SdkInternalApi
public final class Identifier {
    private String name;

    public Identifier(String name) {
        this.name = name;
    }

    public static Identifier fromString(String name) {
        return new Identifier(name);
    }

    public static Identifier of(String name) {
        return new Identifier(name);
    }

    public String asString() {
        return name;
    }

    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Identifier that = (Identifier) o;

        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
