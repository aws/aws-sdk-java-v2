# S3 Event Notifications

## Overview

This module contains the classes used to represent Amazon S3 Event Notifications.


## Deserialization

To convert a json notification to java classes, use the static methods 
available on `S3EventNotification`:

```java
String json = "..."; // the notification as json
S3EventNotification event = S3EventNotification.fromJson(json);
event.getRecords().forEach(rec -> println(rec.toString()));
```

Any missing fields of the json will be null in the resulting object. 
Any extra fields will be ignored.


## Serialization

To convert an instance of `S3EventNotification` to json, use the `.toJson()`
or `toJsonPretty()` method:

```java
S3EventNotification event = new S3EventNotification(...);
String json = event.toJson();
String jsonPretty = event.toJsonPretty();
```

`GlacierEventData`, `ReplicationEventData`, `IntelligentTieringEventData` and `LifecycleEventData`
will be excluded from the json if null. Any other null fields of the object will be 
serialized in the json as `null`.
