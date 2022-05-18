**Design:** New Feature, **Status:** [Released](../../../../../services-custom/dynamodb-enhanced/README.md)

## Problem
The DynamoDB Enhanced `updateItem()` table operation supports creating or updating an existing item by overwriting some or all attributes when supplying a POJO type instance with key attributes. In contrast, the low level DynamoDB [updateItem](https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_UpdateItem.html) operation that is underlying the DynamoDB Enhanced op supports a wider range of functionality through its [UpdateExpression syntax](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html).

This document proposes a mechanism for users to provide update expressions that will allow them to take advantage of the features of the low level expressions and implement functionality such as atomic counters. 

### Requested features
Customer-requested features related to DynamoDB Enhanced UpdateItem:

1. Increment/decrement numerical attribute values in order to support atomic counters and similar use cases
2. Adding items to lists
3. Unsetting / nullifying specific attributes while not modifying the whole item. 
4. Modifying return value behavior.

Example Github issue: https://github.com/aws/aws-sdk-java-v2/issues/2292

## Current functionality
When calling updateItem, the enhanced client converts the supplied POJO into the low-level UpdateExpression syntax. It supports only a few specific actions: 
- REMOVE - to delete attributes
- SET - setting whole attributes

## Proposed Solution
The enhanced client lets users provide custom update expressions in addition to the normal POJO records provided
to the updateItem operation. 
### Enhanced Client UpdateExpression API
The UpdateExpressions you can write in the enhanced client models the DynamoDB syntax at a higher abstraction level in order to support merging and analyzing expressions.  To create an UpdateExpression, create one or more UpdateAction (AddAction, SetAction, RemoveAction and DeleteAction) and add to the UpdateExpression builder.

~~~
SetAction setAction = SetAction.builder()
                               .path("#attr1_ref")
                               .value(":new_value")
                               .putExpressionName("#attr1_ref", "attr1")
                               .putExpressionValue(":new_value", newValue)
                               .build();
                               
UpdateExpression updateExpression = UpdateExpression.builder()
                                                    .addAction(setAction)
                                                    .build();
~~~
*path = either the attribute name or another expression supported by the low level API.*<br>
*value = the value of the path. Can also contain low level expressions.*<br>
*expressionNames = (optional) maps name tokens to attribute names.*<br>
*expressionValues = maps value tokens to attribute values.*<br>

### Applying an UpdateExpression
#### Schema level 
Schema level UpdateExpression enable use cases where the same action should be applied every time the database is called, such as atomic counters.
The extension framework supports UpdateExpression as an output in a WriteModification.

In an extension class implementing [DynamoDbEnhancedClientExtension](https://github.com/aws/aws-sdk-java-v2/blob/feature/master/DynamoDBenhanced-updateexpression/services-custom/dynamodb-enhanced/src/main/java/software/amazon/awssdk/enhanced/dynamodb/DynamoDbEnhancedClientExtension.java):
~~~
WriteModification.builder()
                 .updateExpression(updateExpression)
                 .build();
~~~
Adding the extension to the extension chain:
~~~
DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                              .dynamoDbClient(getDynamoDbClient())
                                                              .extensions(MyExtension.create())
                                                              .build();

DynamoDbTable<Record> table = enhancedClient.table("some-table-name"), TableSchema.fromClass(SomeRecord.class));
~~~


#### Request level
Request level UpdateExpression would allow you to modify the record at a single instant in time, such as adding an item to a list or deleting an attribute, by adding an update expression to the request itself.

**NOTE:** This feature is not supported in the initial release. 

### Transforming enhanced UpdateExpression to low-level
Performed by the enhanced client in the UpdateItemOperation when creating the low level request. It uses the `UpdateExpressionConverter` to transform the UpdateExpression into an Expression and then the string format DynamoDB expects. 

### Precedence and merging rules
Several extensions can return an UpdateExpression, and they need to be merged in the extension chain so that a final extension UpdateExpression reaches the UpdateItemOperation. Within the operation, the extension expression must be merged with the one generated for the request POJO.

[PR #2926](https://github.com/aws/aws-sdk-java-v2/pull/2926) outlines the challenges when resolving the final UpdateExpression in further detail. One of the biggest issues is reconciling the user experience in both using extensions and request and getting a result that makes sense with default configuration.

#### Merging UpdateExpression in the extension chain

Extension merge adds later expressions in the chain to any existing UpdateExpression (remember, the UpdateExpression is just a set of collections before parsing).

Q: What happens if one extension later in the chain modifies the same attribute?<br>
A: Their update actions will both be in the UpdateExpression, and it will fail at parse time.
Should we have more support here?

Q: Can an extension see previous UpdateExpression in the extension chain?<br>
A: The extension context, input to the extension, does not currently contain UpdateExpression, which means the extension cannot view previous expressions (The extension CAN view any updates to the transact item however).

#### Merging extension and request POJO UpdateExpressions

Q: What takes precedence, extension expressions or reqest-level POJO extensions?<br>
A: We currently just add them together, with one exception: A filter is in place that removes automatically generated delete of attributes for the POJO expression, that are explicitly modified by the extension UpdateExpression. This is because `ignoreNulls` is false by default and remove statements would be created to collide with extension attributes.

Q: Should we consider doing something similar for POJO SET operations? <br>
A: It depends on the view one takes of request level POJO updates compared to the extension ones. On one hand, request level often takes precedence. On the other, it’s important to protect the extension from being overwritten. This is not implemented. 

Q: What happens if the POJO sets an attribute that is also modified by an extension?
A: An exception is thrown at parse time, preventing users from overwriting an extension attribute.

## Appendix B: Alternative solutions

### Design alternative: More fluent UpdateExpression API 
In this design alternative, the UpdateExpression API and update actions are more fluent, allowing users to write less code to achieve their goals:
~~~
UpdateExpression updateExpression1 = UpdateExpression.builder()
                                                     .remove("attr1")                              
                                                     .set("attr1", value1, updateBehavior)
                                                     .build();
~~~
If directly adding actions, these had methods exposed 
~~~
UpdateExpression updateExpression2 = UpdateExpression.builder()
                                                     .removeAction(UpdateAction.remove("attr1"))
                                                     .setAction(SetUpdateAction.addWithStartValue(attr2, delta, start))
                                                     .addAction(UpdateAction.appendToList("attr1", 3, myListAttributeValue))
                                                     .addAction(UpdateAction.appendToList("attr1", 3, myListAttributeValue))
                                                     .build();
~~~

**Decision**

This alternative was discarded due to the lack of flexibility in the highly modeled API risking difficulties in keeping up with changes to the low level syntax by DynamoDB.

### Design alternative: Single UpdateAction class
Using a type field to differentiate between different actions:
~~~
UpdateAction updateAction = 
          UpdateAction.builder()
                      .type(UpdateActionType.REMOVE)
                      .attributeName(attributeName)
                      .expression(keyRef(attributeName))
                      .build();
~~~

**Decision**

Discarded in favor of the current design.