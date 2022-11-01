Dotted Version Vector Sets
==========================

[![SWUbanner](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/banner2-direct.svg)](https://github.com/vshymanskyy/StandWithUkraine/blob/main/docs/README.md)

This is an implementation of the Erlang's [DVV](https://github.com/ricardobcl/Dotted-Version-Vectors) on Python.

It is used in distributed systems, where UTC timestamp is unreliable value for object's version control.


Usage examples
==============

* Creating a new version
```java
DVVSet dvvSet = new DVVSet();
Clock dot = dvvSet.create(dvvSet.newDvv("1611600920427"), "user_id_1");
```

* Incrementing version
```java
List context = dvvSet.join(dot);
Clock newDot = dvvSet.update(dvvSet.newWithHistory(context, "1616682865530"), dot, "user_id_2");
List mergedHistory = dvvSet.sync(new Clock(dot.asList(), newDot.asList()));
```

* Detecting conflicts

Conflict is situation when two branches of the history exist.
It could happen when someone updates old version ( casual history ).
```java
List values = dvvSet.values(mergedHistory);
if (values.size() > 1) {
    System.out.println("CONFLICT");
} else {
    System.out.println("OK");
}
```

Example
=======
1. User 1 uploads file to the server, specifying version vector:
```java
DVVSet dvvSet = new DVVSet();
Clock dot = dvvSet.create(dvvSet.newDvv("1611600920427"), "user_id_1");
```

2. Server checks version on a subject of conflict. Then it
stores file with version information and provides it to User 2.
```java
List mergedHistory = dvvSet.sync(new Clock(existingVersion, uploadedVersion));
List values = dvvSet.values(mergedHistory);
if (values.size() > 1) {
    // return 409 Conflict
} else {
    // return 200 OK
}
```

3. User 2 downloads file, edits it and increments its version, before uploading back to server.
```java
List context = dvvSet.join(dot);
Clock newDot = dvvSet.update(dvvSet.newWithHistory(context, "1616682865530"), dot, "user_id_2");
dvvSet.sync(new Clock(dot.asList(), newDot.asList()));
```
