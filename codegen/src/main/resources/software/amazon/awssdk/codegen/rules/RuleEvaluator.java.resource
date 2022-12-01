import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class RuleEvaluator implements FnVisitor<Value>, ExprVisitor<Value> {
    private final Scope<Value> scope = new Scope<>();

    public Value evaluateRuleset(EndpointRuleset ruleset, Map<Identifier, Value> input) {
        return scope.inScope(
                () -> {
                    ruleset
                            .getParameters()
                            .toList()
                            .forEach(
                                    param -> {
                                        param.getDefault().ifPresent(value -> scope.insert(param.getName(), value));
                                    });
                    input.forEach(scope::insert);
                    for (Rule rule : ruleset.getRules()) {
                        Value result = handleRule(rule);
                        if (!result.isNone()) {
                            return result;
                        }
                    }
                    throw new RuntimeException("No rules in ruleset matched");
                });
    }

    @Override
    public Value visitLiteral(Literal literal) {
        return literal.eval(scope);
    }

    @Override
    public Value visitRef(Ref ref) {
        return scope
                .getValue(ref.getName())
                .orElseThrow(
                        () -> new RuntimeException(String.format("Invalid ruleset: %s was not in scope", ref)));
    }

    @Override
    public Value visitFn(Fn fn) {
        return fn.acceptFnVisitor(this);
    }

    @Override
    public Value visitPartition(PartitionFn fn) {
        return fn.eval(scope);
    }

    @Override
    public Value visitParseArn(ParseArn fn) {
        return fn.eval(scope);
    }

    @Override
    public Value visitIsValidHostLabel(IsValidHostLabel fn) {
        return fn.eval(scope);
    }

    @Override
    public Value visitBoolEquals(BooleanEqualsFn fn) {
        return fn.eval(scope);
    }

    @Override
    public Value visitStringEquals(StringEqualsFn fn) {
        return fn.eval(scope);
    }

    @Override
    public Value visitIsSet(IsSet fn) {
        return fn.eval(scope);
    }

    @Override
    public Value visitNot(Not not) {
        return Value.fromBool(!not.target().accept(this).expectBool());
    }

    @Override
    public Value visitGetAttr(GetAttr getAttr) {
        return getAttr.eval(scope);
    }

    @Override
    public Value visitParseUrl(ParseUrl parseUrl) {
        return parseUrl.eval(scope);
    }

    @Override
    public Value visitSubstring(Substring fn) {
        return fn.eval(scope);
    }

    @Override
    public Value visitUriEncode(UriEncodeFn fn) {
        return fn.eval(scope);
    }

    @Override
    public Value visitIsVirtualHostLabelsS3Bucket(IsVirtualHostableS3Bucket fn) {
        return fn.eval(scope);
    }

    private Value handleRule(Rule rule) {
        RuleEvaluator self = this;
        return scope.inScope(
                () -> {
                    for (Condition condition : rule.getConditions()) {
                        Value value = evaluateCondition(condition);
                        if (value.isNone() || value.equals(Value.fromBool(false))) {
                            return Value.none();
                        }
                    }
                    return rule.accept(new RuleValueVisitor<Value>() {
                        @Override
                        public Value visitTreeRule(List<Rule> rules) {
                            for (Rule subrule : rules) {
                                Value result = handleRule(subrule);
                                if (!result.isNone()) {
                                    return result;
                                }
                            }
                            throw new RuntimeException(
                                    String.format("no rules inside of tree rule matched—invalid rules (%s)", this));
                        }

                        @Override
                        public Value visitErrorRule(Expr error) {
                            return error.accept(self);
                        }

                        @Override
                        public Value visitEndpointRule(EndpointResult endpoint) {
                            return generateEndpoint(endpoint);
                        }
                    });
                });
    }

    public Value evaluateCondition(Condition condition) {
        Value value = condition.getFn().accept(this);
        if (!value.isNone()) {
            condition.getResult().ifPresent(res -> scope.insert(res, value));
        }
        return value;
    }

    public Value generateEndpoint(EndpointResult endpoint) {
        Value.Endpoint.Builder builder = Value.Endpoint.builder()
                                                       .url(endpoint.getUrl()
                                                                    .accept(this)
                                                                    .expectString());
        endpoint.getProperties()
                .forEach(
                        (key, value) -> {
                            builder.property(key.toString(), value.accept(this));
                        });
        endpoint
                .getHeaders()
                .forEach(
                        (name, exprs) -> {
                            exprs.forEach(expr -> builder.addHeader(name, expr.accept(this).expectString()));
                        });

        return builder.build();
    }
}
