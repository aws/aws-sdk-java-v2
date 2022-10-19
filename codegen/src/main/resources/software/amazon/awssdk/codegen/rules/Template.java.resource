import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Template represents a "Template Literal". This is a literal string within the rules language. A template can contain 0 or more
 * dynamic sections. The dynamic sections use getAttr short-form:
 * <p>
 * `https://{Region}.{partition#dnsSuffix}` --------          ------------ |                   | Dynamic            getAttr short
 * form
 */
@SdkInternalApi
public class Template {
    private final List<Part> parts;

    Template(String template) {
        this.parts = RuleError.ctx("when parsing template", () -> parseTemplate(template));
    }

    public <T> Stream<T> accept(TemplateVisitor<T> visitor) {
        if (isStatic()) {
            return Stream.of(visitor.visitStaticTemplate(expectLiteral()));
        }
        if (parts.size() == 1) {
            // must be dynamic because previous branch handled single-element static template
            return Stream.of(visitor.visitSingleDynamicTemplate(((Dynamic) parts.get(0)).expr));
        }
        Stream<T> start = Stream.of(visitor.startMultipartTemplate());
        Stream<T> components = parts.stream().map(part -> part.accept(visitor));
        Stream<T> end = Stream.of(visitor.finishMultipartTemplate());
        return Stream.concat(start, Stream.concat(components, end));
    }

    public List<Part> getParts() {
        return parts;
    }

    public boolean isStatic() {
        return this.parts.stream().allMatch(it -> it instanceof Literal);
    }

    public String expectLiteral() {
        assert isStatic();
        return this.parts.stream().map(Part::toString).collect(Collectors.joining());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Template template = (Template) o;

        return parts != null ? parts.equals(template.parts) : template.parts == null;
    }

    @Override
    public int hashCode() {
        return parts != null ? parts.hashCode() : 0;
    }

    public static Template fromString(String s) {
        return new Template(s);
    }

    @Override
    public String toString() {
        return String.format("\"%s\"", this.parts.stream().map(Part::toString).collect(Collectors.joining()));
    }

    public Value eval(Scope<Value> scope) {
        return Value.fromStr(parts.stream().map(part -> part.eval(scope)).collect(Collectors.joining()));
    }

    private List<Part> parseTemplate(String template) {
        List<Part> out = new ArrayList<>();
        Optional<Integer> templateStart = Optional.empty();
        int depth = 0;
        int templateEnd = 0;
        for (int i = 0; i < template.length(); i++) {
            if (template.substring(i).startsWith("{{")) {
                i++;
                continue;
            }
            if (template.substring(i).startsWith("}}")) {
                i++;
                continue;
            }
            if (template.charAt(i) == '{') {
                if (depth == 0) {
                    if (templateEnd != i) {
                        out.add(Literal.unescape(template.substring(templateEnd, i)));
                    }
                    templateStart = Optional.of(i + 1);
                }
                depth++;
            }
            if (template.charAt(i) == '}') {
                depth--;
                if (depth < 0) {
                    throw new InnerParseError("unmatched `}` in template");
                }
                if (depth == 0) {
                    out.add(Dynamic.parse(template.substring(templateStart.get(), i)));
                    templateStart = Optional.empty();
                }
                templateEnd = i + 1;
            }
        }
        if (depth != 0) {
            throw new InnerParseError("unmatched `{` in template");
        }
        if (templateEnd < template.length()) {
            out.add(Literal.unescape(template.substring(templateEnd)));
        }
        return out;
    }

    public abstract static class Part {
        abstract String eval(Scope<Value> scope);

        abstract <T> T accept(TemplateVisitor<T> visitor);
    }

    public static class Literal extends Part {
        private final String value;

        public Literal(String value) {
            if (value.isEmpty()) {
                throw new RuntimeException("value cannot blank");
            }
            this.value = value;
        }

        public static Literal unescape(String value) {
            return new Literal(value.replace("{{", "{").replace("}}", "}"));
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @Override
        String eval(Scope<Value> scope) {
            return this.value;
        }

        @Override
        <T> T accept(TemplateVisitor<T> visitor) {
            return visitor.visitStaticElement(this.value);
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

            return value != null ? value.equals(literal.value) : literal.value == null;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }

    public static class Dynamic extends Part {
        private final String raw;
        private final Expr expr;

        private Dynamic(String raw, Expr expr) {
            this.raw = raw;
            this.expr = expr;
        }

        @Override
        public String toString() {
            return String.format("{dyn %s}", this.raw);
        }

        @Override
        String eval(Scope<Value> scope) {
            return RuleError.ctx("while evaluating " + this, () -> expr.eval(scope).expectString());
        }

        @Override
        <T> T accept(TemplateVisitor<T> visitor) {
            return visitor.visitDynamicElement(this.expr);
        }

        public Expr getExpr() {
            return expr;
        }

        public static Dynamic parse(String value) {
            return new Dynamic(value, Expr.parseShortform(value));
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

            Dynamic dynamic = (Dynamic) o;

            if (raw != null ? !raw.equals(dynamic.raw) : dynamic.raw != null) {
                return false;
            }
            return expr != null ? expr.equals(dynamic.expr) : dynamic.expr == null;
        }

        @Override
        public int hashCode() {
            int result = raw != null ? raw.hashCode() : 0;
            result = 31 * result + (expr != null ? expr.hashCode() : 0);
            return result;
        }
    }
}
