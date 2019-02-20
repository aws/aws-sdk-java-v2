## Use of Optional

This page describes general guidelines of how we use `Optional`.

- Do not declare any instance variable of type Optional in public API.
- Do not use Optional in parameters in public API
- Do not use Optional in Builder classes
- Do not use Optional in POJOs.
- Use Optional for getters that access the field that's we know always going to be optional.
- Use Optional as a return type for any methods that have a result that's we know always going to be optional.

References:
- http://blog.joda.org/2015/08/java-se-8-optional-pragmatic-approach.html
