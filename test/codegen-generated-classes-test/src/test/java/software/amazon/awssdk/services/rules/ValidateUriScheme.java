package software.amazon.awssdk.services.rules;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.EndpointResult;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Identifier;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Literal;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.Template;

/**
 * Validate that URIs start with a scheme
 */
public class ValidateUriScheme extends TraversingVisitor<ValidationError> {
    boolean checkingEndpoint = false;

    @Override
    public Stream<ValidationError> visitEndpoint(EndpointResult endpoint) {
        checkingEndpoint = true;
        Stream<ValidationError> errors = endpoint.getUrl().accept(this);
        checkingEndpoint = false;
        return errors;
    }

    @Override
    public Stream<ValidationError> visitLiteral(Literal literal) {
        return literal.accept(new Literal.Visitor<Stream<ValidationError>>() {
            @Override
            public Stream<ValidationError> visitBool(boolean b) {
                return Stream.empty();
            }

            @Override
            public Stream<ValidationError> visitStr(Template value) {
                return validateTemplate(value);
            }

            @Override
            public Stream<ValidationError> visitObject(Map<Identifier, Literal> members) {
                return Stream.empty();
            }

            @Override
            public Stream<ValidationError> visitTuple(List<Literal> members) {
                return Stream.empty();
            }

            @Override
            public Stream<ValidationError> visitInt(int value) {
                return Stream.empty();
            }
        });
    }

    private Stream<ValidationError> validateTemplate(Template template) {
        if (checkingEndpoint) {
            Template.Part head = template.getParts().get(0);
            if (head instanceof Template.Literal) {
                String templateStart = ((Template.Literal) head).getValue();
                if (!(templateStart.startsWith("http://") || templateStart.startsWith("https://"))) {
                    return Stream.of(new ValidationError(
                            ValidationErrorType.INVALID_URI,
                            "URI should start with `http://` or `https://` but the URI started with " + templateStart)
                    );
                }
            }
            /* Allow dynamic URIs for now—we should lint that at looks like a scheme at some point */
        }
        return Stream.empty();
    }
}
