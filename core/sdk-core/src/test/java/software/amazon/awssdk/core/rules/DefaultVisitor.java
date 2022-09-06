package software.amazon.awssdk.core.rules;

import java.util.List;

public abstract class DefaultVisitor<R> implements RuleValueVisitor<R>, ExprVisitor<R>, FnVisitor<R> {
    public abstract R getDefault();

    @Override
    public R visitLiteral(Literal literal) {
        return getDefault();
    }

    @Override
    public R visitRef(Ref ref) {
        return getDefault();
    }

    @Override
    public R visitFn(Fn fn) {
        return getDefault();
    }

    @Override
    public R visitPartition(PartitionFn fn) {
        return getDefault();
    }

    @Override
    public R visitParseArn(ParseArn fn) {
        return getDefault();
    }


    @Override
    public R visitIsValidHostLabel(IsValidHostLabel fn) {
        return getDefault();
    }

    @Override
    public R visitBoolEquals(BooleanEqualsFn fn) {
        return getDefault();
    }

    @Override
    public R visitStringEquals(StringEqualsFn fn) {
        return getDefault();
    }

    @Override
    public R visitIsSet(IsSet fn) {
        return getDefault();
    }

    @Override
    public R visitNot(Not not) {
        return getDefault();
    }

    @Override
    public R visitGetAttr(GetAttr getAttr) {
        return getDefault();
    }

    @Override
    public R visitParseUrl(ParseUrl parseUrl) {
        return getDefault();
    }

    @Override
    public R visitSubstring(Substring substring) { return getDefault(); }

    @Override
    public R visitTreeRule(List<Rule> rules) {
        return getDefault();
    }

    @Override
    public R visitErrorRule(Expr error) {
        return getDefault();
    }

    @Override
    public R visitEndpointRule(EndpointResult endpoint) {
        return getDefault();
    }

}
