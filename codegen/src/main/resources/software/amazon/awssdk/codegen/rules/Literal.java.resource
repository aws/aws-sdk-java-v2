import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;

@SdkInternalApi
public class Literal extends Expr {
    public interface Visitor<T> {

        T visitBool(boolean b);

        T visitStr(Template value);

        T visitObject(Map<Identifier, Literal> members);

        T visitTuple(List<Literal> members);

        T visitInt(int value);
    }

    private final Lit source;

    private Literal(Lit source) {
        this.source = source;
    }

    public <T> T accept(Visitor<T> visitor) {
        return this.source.accept(visitor);
    }

    public String expectLiteralString() {
        if (source instanceof Str) {
            Str s = (Str) source;

            return s.value.expectLiteral();
        } else {
            throw RuleError.builder()
                           .cause(SourceException.builder()
                                                 .message("Expected a literal string, got " + source)
                                                 .build())
                           .build();
        }
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitLiteral(this);
    }

    @Override
    public Value eval(Scope<Value> scope) {
        return source.accept(new Visitor<Value>() {
            @Override
            public Value visitInt(int value) {
                return Value.fromInteger(value);
            }

            @Override
            public Value visitBool(boolean b) {
                return Value.fromBool(b);
            }

            @Override
            public Value visitStr(Template value) {
                return value.eval(scope);
            }

            @Override
            public Value visitObject(Map<Identifier, Literal> members) {
                Map<Identifier, Value> tpe = new HashMap<>();
                members.forEach((k, v) -> {
                    tpe.put(k, v.eval(scope));
                });
                return Value.fromRecord(tpe);
            }

            @Override
            public Value visitTuple(List<Literal> members) {
                List<Value> tuples = new ArrayList<>();
                for (Literal el : ((Tuple) source).members) {
                    tuples.add(el.eval(scope));
                }
                return Value.fromArray(tuples);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Literal literal = (Literal) o;

        return source != null ? source.equals(literal.source) : literal.source == null;
    }

    @Override
    public int hashCode() {
        return source != null ? source.hashCode() : 0;
    }

    public String toString() {
        return source.toString();
    }

    public static Literal fromNode(JsonNode node) {
        Lit lit;
        if (node.isArray()) {
            List<Literal> array = node.asArray().stream().map(Literal::fromNode).collect(Collectors.toList());
            lit = new Tuple(array);
        } else if (node.isBoolean()) {
            lit = new Bool(node.asBoolean());
        } else if (node.isNull()) {
            throw SdkClientException.create("null node not supported");
        } else if (node.isNumber()) {
            lit = new Int(Integer.parseInt(node.asNumber()));
        } else if (node.isObject()) {
            Map<Identifier, Literal> obj = new HashMap<>();
            node.asObject().forEach((k, v) -> obj.put(Identifier.of(k), fromNode(v)));
            lit = new Obj(obj);
        } else if (node.isString()) {
            lit = new Str(new Template(node.asString()));
        } else {
            throw SdkClientException.create("Unable to create literal from " + node);
        }
        return new Literal(lit);
    }

    public static Literal fromTuple(List<Literal> authSchemes) {
        return new Literal(new Tuple(authSchemes));
    }

    public static Literal fromRecord(Map<Identifier, Literal> record) {
        return new Literal(new Obj(record));
    }

    public static Literal fromStr(Template value) {
        return new Literal(new Str(value));
    }

    public static Literal fromStr(String s) {
        return fromStr(new Template(s));
    }

    public static Literal fromInteger(int value) {
        return new Literal(new Int(value));
    }

    public static Literal fromBool(boolean value) {
        return new Literal(new Bool(value));
    }

    private interface Lit {
        <T> T accept(Visitor<T> visitor);
    }

    static final class Int implements Lit {
        private final Integer value;

        Int(Integer value) {
            this.value = value;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitInt(value);
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Int anInt = (Int) o;

            return value != null ? value.equals(anInt.value) : anInt.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    static final class Tuple implements Lit {
        private final List<Literal> members;

        Tuple(List<Literal> members) {
            this.members = members;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitTuple(members);
        }

        @Override
        public String toString() {
            return members.stream().map(Literal::toString).collect(Collectors.joining(", ", "[", "]"));
        }

        public List<Literal> members() {
            return members;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Tuple tuple = (Tuple) o;

            return members != null ? members.equals(tuple.members) : tuple.members == null;
        }

        @Override
        public int hashCode() {
            return members != null ? members.hashCode() : 0;
        }
    }

    static final class Obj implements Lit {
        private final Map<Identifier, Literal> members;

        Obj(Map<Identifier, Literal> members) {
            this.members = members;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitObject(members);
        }

        @Override
        public String toString() {
            return members.toString();
        }

        public Map<Identifier, Literal> members() {
            return members;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Obj obj = (Obj) o;

            return members != null ? members.equals(obj.members) : obj.members == null;
        }

        @Override
        public int hashCode() {
            return members != null ? members.hashCode() : 0;
        }
    }

    static final class Bool implements Lit {
        private final boolean value;

        Bool(boolean value) {
            this.value = value;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitBool(value);
        }

        @Override
        public String toString() {
            return Boolean.toString(value);
        }

        public Boolean value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Bool bool = (Bool) o;

            return value == bool.value;
        }

        @Override
        public int hashCode() {
            return value ? 1 : 0;
        }
    }


    static final class Str implements Lit {
        private final Template value;

        Str(Template value) {
            this.value = value;
        }

        @Override
        public <T> T accept(Visitor<T> visitor) {
            return visitor.visitStr(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }

        public Template value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Str str = (Str) o;

            return value != null ? value.equals(str.value) : str.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }
}
