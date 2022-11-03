package software.amazon.awssdk.services.rules;

import java.util.List;
import java.util.stream.Stream;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Condition;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.EndpointResult;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.EndpointRuleset;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Expr;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Fn;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Rule;

public abstract class TraversingVisitor<R> extends DefaultVisitor<Stream<R>> {
    public Stream<R> visitRuleset(EndpointRuleset ruleset) {
        return ruleset.getRules()
                .stream()
                .flatMap(this::handleRule);
    }

    private Stream<R> handleRule(Rule rule) {
        Stream<R> fromConditions = visitConditions(rule.getConditions());
        return Stream.concat(fromConditions, rule.accept(this));
    }

    @Override
    public Stream<R> visitFn(Fn fn) {
        return fn.acceptFnVisitor(this);
    }

    @Override
    public Stream<R> getDefault() {
        return Stream.empty();
    }

    @Override
    public Stream<R> visitEndpointRule(EndpointResult endpoint) {
        return visitEndpoint(endpoint);
    }

    @Override
    public Stream<R> visitErrorRule(Expr error) {
        return error.accept(this);
    }

    @Override
    public Stream<R> visitTreeRule(List<Rule> rules) {
        return rules.stream().flatMap(subrule -> subrule.accept(this));
    }

    public Stream<R> visitEndpoint(EndpointResult endpoint) {
        return Stream.concat(
                endpoint.getUrl()
                        .accept(this),
                endpoint.getProperties()
                        .entrySet()
                        .stream()
                        .flatMap(map -> map.getValue().accept(this))
        );
    }

    public Stream<R> visitConditions(List<Condition> conditions) {
        return conditions.stream().flatMap(c -> c.getFn().accept(this));
    }
}
