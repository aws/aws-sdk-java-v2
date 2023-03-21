# Decision Log for v2 Transfer Manager Progress Listeners

## Log Entry Template

**Source:** (Meeting/aside/pair programming discussion/daily standup) to (discuss/implement) X

**Attendees:** (names)

**Closed Decisions:**

1. Question? Decision. Justification.

**Open Decisions:**

1. (Old/Reopened/New) Question?

## 2/22/23

**Source:** Meeting to discuss Surface API review [PR# 3786](https://github.com/aws/aws-sdk-java-v2/pull/3786)

**Attendees:** Anna-karin, DavidH, Dongie, John, Matt, Olivier, Zoe

**Closed Decisions:**

1. **Should we provide API to Get/Set the attribute as `Object` similar to 1,x?**
    1. No , we donot need them.After playing around with APIs provided in Surface API review , Matt suggested that AttributeValue is the right thing for us to use 
for the getList(String), getMap(String) method where we don’t use attribute converters.

1. **How can users access the unstructured elements of a DynamoDB item that may contain nested lists, strings, maps, and other types?**
    1. Working with List<Object>, Map<String, Object> can be cumbersome due to verbose instanceof checks, especially for nested sets or lists. 
A better alternative is to use List<AttributeValue>, which is a union type that provides a type() method, a visit() method, 
and other helpful functionalities. This approach can simplify the process of accessing unstructured data in DynamoDB items.
We will add new methods to EnhancedDocument:
```java
List<AttributeValue> getUnknownTypeList(String attributeName);
Map<String, AttributeValue> getUnknownTypeMap(String attributeName); 
Map<String, AttributeValue> toUnknownTypeMap(); 
// (Replaces Map<String, Object> toMap(), since AttributeValue is much easier to work with) toAttributeValueMap
```

1. **Use EnhancedAttributeValue  instead of AttributeValue for UnknownTypes ?**
    1. EnhancedAttributeValue is currently an InternalAPI and is not publicly available.

1. **How can user access an attribute value of UnknownType ?**
    1. User should first access document.toUnknownTypeMap() and then access that attribute as AttributeValue
```java
AttributeValue unknownTypeAttributeValue = document.toUnknownTypeMap().get("keyOfUnknownAttribute");
```