import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class GetAttr extends Fn {
    public static final String ID = "getAttr";

    public GetAttr(FnNode node) {
        super(node);
    }

    @Override
    public Value eval(Scope<Value> scope) {
        Value root = target().eval(scope);
        List<Part> path;
        try {
            path = path();
        } catch (InnerParseError e) {
            throw new RuntimeException(e);
        }
        for (Part part : path) {
            root = part.eval(root);
        }
        return root;
    }

    public interface Part {

        Value eval(Value container);

        final class Key implements Part {
            private final Identifier key;

            public Key(Identifier key) {
                this.key = key;
            }

            @Override
            public String toString() {
                return key.asString();
            }

            public static Key of(String key) {
                return new Key(Identifier.of(key));
            }

            @Override
            public Value eval(Value container) {
                return container.expectRecord().get(key);
            }

            public Identifier key() {
                return key;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                Key that = (Key) obj;
                return Objects.equals(this.key, that.key);
            }

            @Override
            public int hashCode() {
                return key != null ? key.hashCode() : 0;
            }
        }

        final class Index implements Part {
            private final int index;

            public Index(int index) {
                this.index = index;
            }

            @Override
            public Value eval(Value container) {
                return container.expectArray().get(index);
            }

            @Override
            public String toString() {
                return String.format("[%s]", index);
            }

            public int index() {
                return index;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                Index that = (Index) obj;
                return this.index == that.index;
            }

            @Override
            public int hashCode() {
                return index;
            }
        }
    }


    private static GetAttr fromBuilder(Builder builder) {
        return new GetAttr(FnNode
                .builder()
                .fn("getAttr")
                .argv(
                        Arrays.asList(
                                builder.target,
                                Literal.fromStr(String.join(".", builder.path))))
                .build());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Expr target() {
        return expectTwoArgs().left();
    }

    public List<Part> path() throws InnerParseError {
        Expr right = expectTwoArgs().right();
        if (right instanceof Literal) {
            Literal path = (Literal) right;
            return parse(path.expectLiteralString());
        } else {
            throw SourceException.builder().message("second argument must be a string literal").build();
        }
    }

    private static List<Part> parse(String path) throws InnerParseError {
        String[] components = path.split("\\.");
        List<Part> result = new ArrayList<>();
        for (String component : components) {
            if (component.contains("[")) {
                int slicePartIndex = component.indexOf("[");
                String slicePart = component.substring(slicePartIndex);
                if (!slicePart.endsWith("]")) {
                    throw new InnerParseError("Invalid path component: %s. Must end with `]`");
                }
                try {
                    String number = slicePart.substring(1, slicePart.length() - 1);
                    int slice = Integer.parseInt(number);
                    if (slice < 0) {
                        throw new InnerParseError("Invalid path component: slice index must be >= 0");
                    }
                    result.add(Part.Key.of(component.substring(0, slicePartIndex)));
                    result.add(new Part.Index(slice));
                } catch (NumberFormatException ex) {
                    throw new InnerParseError(String.format("%s could not be parsed as a number", slicePart));
                }
            } else {
                result.add(Part.Key.of(component));
            }
        }
        if (result.isEmpty()) {
            throw new InnerParseError("Invalid argument to GetAttr: path may not be empty");
        }
        return result;
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitGetAttr(this);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(target());
        try {
            for (Part part : path()) {
                out.append(".");
                out.append(part);
            }
        } catch (InnerParseError e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    @Override
    public String template() {
        String target = ((Ref) this.target()).getName().asString();
        StringBuilder pathPart = new StringBuilder();

        List<Part> partList;
        try {
            partList = path();
        } catch (InnerParseError e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < partList.size(); i ++) {
            if (i != 0) {
                if (partList.get(i) instanceof Part.Key) {
                    pathPart.append(".");
                }
            }
            pathPart.append(partList.get(i).toString());
        }
        return "{" + target + "#" + pathPart + "}";
    }

    public static class Builder {
        Expr target;
        String path;

        public Builder target(Expr target) {
            this.target = target;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public GetAttr build() {
            return GetAttr.fromBuilder(this);
        }
    }
}
