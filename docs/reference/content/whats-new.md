+++
date = "2016-06-09T12:47:43-04:00"
title = "What's New"
[menu.main]
  identifier = "Release notes"
  weight = 15
  pre = "<i class='fa fa-level-up'></i>"
+++

## What's New in 3.8

Key new features of the 3.8 Java driver release:

### Transactions

The Java driver now provides support for executing CRUD operations within a transaction (requires MongoDB 4.0).  See the 
[Transactions and MongoDB Drivers](https://docs.mongodb.com/master/core/transactions/#transactions-and-mongodb-drivers) section
of the documentation and select the `Java (Sync)` tab.

### Change Stream enhancements

The Java driver now provides support for opening a change stream against an entire database, via new 
[`MongoDatabase.watch`]({{<apiref "com/mongodb/client/MongoDatabase.html">}}) methods, or an 
entire deployment, via new [`MongoClient.watch`]({{<apiref "com/mongodb/client/MongoClient.html">}}) methods. See 
[Change Streams]({{<ref "driver/tutorials/change-streams.md">}}) for further details.

### SCRAM-256 Authentication Mechanism

The Java driver now provides support for the SCRAM-256 authentication mechanism (requires MongoDB 4.0).


## What's New in 3.7

Key new features of the 3.7 Java driver release:

### Java 9 support

#### Modules

The Java driver now provides a set of JAR files that are compliant with the Java 9 
[module specification](http://cr.openjdk.java.net/~mr/jigsaw/spec/), and `Automatic-Module-Name` declarations have been added 
to the manifests of those JAR files. See the [Installation Guide]({{<ref "driver/getting-started/installation.md">}}) 
for information on which JAR files are now Java 9-compliant modules as well as what each of their module names is.  

Note that it was not possible to modularize all the existing JAR files due to the fact that, for some of them, packages are split amongst 
multiple JAR files, and this violates a core rule of the Java 9 module system which states that at most one module contains classes for any 
given package. For instance, the `mongodb-driver` and `mongodb-driver-core` JAR files both contain classes in the `com.mongodb` package, 
and thus it's not possible to make both `mongodb-driver` and `mongodb-driver-core` Java 9 modules. Also so-called 
"uber jars" like `mongo-java-driver` are not appropriate for Java 9 modularization, as they can conflict with their non-uber brethren, and 
thus have not been given module names. 

Note that none of the modular JAR files contain `module-info` class files yet.  Addition of these classes will be considered in a future 
release.

#### New Entry Point

So that the driver can offer a modular option, a new entry point has been added to the `com.mongodb.client` package. 
Static methods in this entry point, `com.mongodb.client.MongoClients`, returns instances of a new `com.mongodb.client.MongoClient` 
interface.  This interface, while similar to the existing `com.mongodb.MongoClient` class in that it is a factory for 
`com.mongodb.client.MongoDatabase` instances, does not support the legacy `com.mongodb.DBCollection`-based API, and thus does not suffer 
from the aforementioned package-splitting issue that prevents Java 9 modularization. This new entry point is encapsulated in the new 
`mongodb-driver-sync` JAR file, which is also a Java 9-compliant module.

The new entry point also moves the driver further in the direction of effective deprecation of the legacy API, which is now only available
only via the `mongo-java-driver` and `mongodb-driver` uber-jars, which are not Java 9 modules. At this point there are no plans to offer 
the legacy API as a Java 9 module.

See [Connect To MongoDB]({{<ref "driver/tutorials/connect-to-mongodb.md">}}) for details on the new `com.mongodb.client.MongoClients`
and how it compares to the existing `com.mongodb.MongoClient` class.   

### Unix domain socket support

The 3.7 driver adds support for Unix domain sockets via the [`jnr.unixsocket`](http://https://github.com/jnr/jnr-unixsocket) library.
Connecting to Unix domain sockets is done via the [`ConnectionString`]({{< apiref "com/mongodb/ConnectionString" >}}) or via
[`UnixServerAddress`]({{<apiref "com/mongodb/UnixServerAddress.html">}}).

### PojoCodec improvements

The 3.7 release brings support for `Map<String, Object>` to the `PojoCodec`.

### JSR-310 Instant, LocalDate & LocalDateTime support

Support for `Instant`, `LocalDate` and `LocalDateTime` has been added to the driver. The MongoDB Java drivers team would like to thank
[Cezary Bartosiak](https://github.com/cbartosiak) for their excellent contribution to the driver. Users needing alternative data structures
and / or more flexibility regarding JSR-310 dates should check out the alternative JSR-310 codecs provider by Cezary:
[bson-codecs-jsr310](https://github.com/cbartosiak/bson-codecs-jsr310).

### JSR-305 NonNull annotations

The public API is now annotated with JSR-305 compatible `@NonNull` and `@Nullable` annotations.  This will allow programmers
to rely on tools like FindBugs/SpotBugs, IDEs like IntelliJ IDEA, and compilers like the Kotlin compiler to find errors in the use of the 
driver via static analysis rather than via runtime failures.

### Improved logging of commands

When the log level is set to DEBUG for the `org.mongodb.driver.protocol.command` logger, the driver now logs additional information to aid
in debugging:

* Before sending the command, it logs the full command (up to 1000 characters), and the request id.
* After receive a response to the command, it logs the request id and elapsed time in milliseconds.

Here's an example

```
10:37:29.099 [cluster-ClusterId {value='5a466138741fc252712a6d71', description='null'}-127.0.0.1:27017] DEBUG org.mongodb.driver.protocol.command - 
Sending command '{ "ismaster" : 1, "$db" : "admin" } ...' with request id 4 to database admin on connection [connectionId{localValue:1, serverValue:1958}] to server 127.0.0.1:27017
10:37:29.104 [cluster-ClusterId{value='5a466138741fc252712a6d71', description='null'}-127.0.0.1:27017] DEBUG org.mongodb.driver.protocol.command - 
Execution of command with request id 4 completed successfully in 22.44 ms on connection [connectionId {localValue:1, serverValue:1958}] to server 127.0.0.1:27017
```
 
### Improved support for "raw" documents

When working with "raw" BSON for improved performance via the [`RawBsonDocument`]({{<apiref "org/bson/RawBsonDocument">}}), the efficiency
of accessing embedded documents and arrays has been drastically improved by returning raw slices of the containing document or array.  For
instance

```java
RawBsonDocument doc = new RawBsonDocument(bytes);

// returns a RawBsonDocument that is a slice of the bytes from the containing doc
BsonDocument embeddedDoc = doc.getDocument("embeddedDoc");

// returns a RawBsonArray that is a slice of the bytes from the containing doc
BsonArray embeddedArray = doc.getArray("embeddedArray");

// returns a RawBsonDocument that is a slice of the bytes from the containing array 
BsonDocument embeddedDoc2 = (BsonDocument) embeddedArray.get(0); 
``` 

## What's New in 3.6

Key new features of the 3.6 Java driver release:

### Change Stream support

The 3.6 release adds support for [change streams](http://dochub.mongodb.org/core/changestreams).

* [Change Stream Quick Start]({{<ref "driver/tutorials/change-streams.md">}}) 
* [Change Stream Quick Start (Async)]({{<ref "driver-async/tutorials/change-streams.md">}})

### Retryable writes

The 3.6 release adds support for retryable writes using the `retryWrites` option in 
[`MongoClientOptions`]({{<apiref "com/mongodb/MongoClientOptions">}}).

### Compression

The 3.6 release adds support for compression of messages to and from appropriately configured MongoDB servers:

* [Compression Tutorial]({{<ref "driver/tutorials/compression.md">}})
* [Compression Tutorial (Async)]({{<ref "driver-async/tutorials/compression.md">}})

### Causal consistency
              
The 3.6 release adds support for [causally consistency](http://dochub.mongodb.org/core/causal-consistency) via the new
[`ClientSession`]({{<apiref "com/mongodb/session/ClientSession">}}) API. 

### Application-configured server selection

The 3.6 release adds support for application-configured control over server selection, using the `serverSelector` option in
[`MongoClientOptions`]({{<apiref "com/mongodb/MongoClientOptions">}}).

### PojoCodec improvements

The 3.6 release brings new improvements to the `PojoCodec`:

  * Improved sub-class and discriminator support.
  * Support for custom Collection and Map implementations.
  * Improvements to the `BsonCreator` annotation, which now supports `@BsonId` and `@BsonProperty` with values that represent the read name of the property.
  * A new [`PropertyCodecProvider`]({{<apiref "org/bson/codecs/pojo/PropertyCodecProvider">}}) API, allowing for easy and type-safe handling of container types.
  * Added the [`SET_PRIVATE_FIELDS_CONVENTION`]({{<apiref "org/bson/codecs/pojo/Conventions.html#SET_PRIVATE_FIELDS_CONVENTION">}}) convention.
  * Added the [`USE_GETTERS_FOR_SETTERS`]({{<apiref "org/bson/codecs/pojo/Conventions.html#USE_GETTERS_FOR_SETTERS">}}) convention.

The MongoDB Java drivers team would like to thank both [Joseph Florencio](https://github.com/jflorencio) and [Qi Liu](https://github.com/visualage)
for their excellent contributions to the PojoCodec.

## What's New in 3.5

Key new features of the 3.5 Java driver release:

### Native POJO support

The 3.5 release adds support for [POJO](https://en.wikipedia.org/wiki/Plain_old_Java_object) serialization at the BSON layer, and can be
used by the synchronous and asynchronous drivers.  See the POJO Quick start pages for details.

* [POJO Quick Start]({{<ref "driver/getting-started/quick-start-pojo.md">}}) 
* [POJO Quick Start (Async)]({{<ref "driver-async/getting-started/quick-start-pojo.md">}})
* [POJO Reference]({{<ref "bson/pojos.md">}}) 

### Improved JSON support

The 3.5 release improves support for JSON parsing and generation.

* Implements the new [Extended JSON specification](https://github.com/mongodb/specifications/blob/master/source/extended-json.rst)
* Implements custom JSON converters to give applications full control over JSON generation for each BSON type

See the [JSON reference]({{<ref "bson/extended-json.md">}}) for details. 

### Connection pool monitoring

The 3.5 release adds support for monitoring connection pool-related events.

* [Connection pool monitoring in the driver]({{<ref "driver/reference/monitoring.md">}})
* [Connection pool monitoring in the async driver]({{<ref "driver-async/reference/monitoring.md">}})

### SSLContext configuration

The 3.5 release supports overriding the default `javax.net.ssl.SSLContext` used for SSL connections to MongoDB.

* [SSL configuration in the driver]({{<ref "driver/tutorials/ssl.md">}})
* [SSL configuration in the async driver]({{<ref "driver-async/tutorials/ssl.md">}})

### KeepAlive configuration deprecated

The 3.5 release deprecated socket keep-alive settings, also socket keep-alive checks are now on by default.
It is *strongly recommended* that system keep-alive settings should be configured with shorter timeouts. 

See the 
['does TCP keep-alive time affect MongoDB deployments?']({{<docsref "/faq/diagnostics/#does-tcp-keepalive-time-affect-mongodb-deployments">}}) 
documentation for more information.


## What's New in 3.4

The 3.4 release includes full support for the MongoDB 3.4 server release.  Key new features include:

### Support for Decimal128 Format

``` java
import org.bson.types.Decimal128;
```

The [Decimal128]({{<docsref "release-notes/3.4/#decimal-type">}}) format supports numbers with up to 34 decimal digits
(i.e. significant digits) and an exponent range of ???6143 to +6144.

To create a `Decimal128` number, you can use

- [`Decimal128.parse()`] ({{<apiref "org/bson/types/Decimal128.html">}}) with a string:

      ```java
      Decimal128.parse("9.9900");
      ```

- [`new Decimal128()`] ({{<apiref "org/bson/types/Decimal128.html">}}) with a long:


      ```java
      new Decimal128(10L);
      ```

- [`new Decimal128()`] ({{<apiref "org/bson/types/Decimal128.html">}}) with a `java.math.BigDecimal`:

      ```java
      new Decimal128(new BigDecimal("4.350000"));
      ```

### Support for Collation

```java
import com.mongodb.client.model.Collation;
```

[Collation]({{<docsref "reference/collation/">}}) allows users to specify language-specific rules for string
comparison. 
Use the [`Collation.builder()`] ({{<apiref "com/mongodb/client/model/Collation.html">}}) 
to create the `Collation` object. For example, the following example creates a `Collation` object with Primary level of comparison and [locale]({{<docsref "reference/collation-locales-defaults/#supported-languages-and-locales">}}) ``fr``.

```java
Collation.builder().collationStrength(CollationStrength.PRIMARY).locale("fr").build()));
```

You can specify collation at the collection level, at an index level, or at a collation-supported operation level:

#### Collection Level

To specify collation at the collection level, pass a `Collation` object as an option to the `createCollection()` method. To specify options to the `createCollection` method, use the [`CreateCollectionOptions`]({{<apiref "com/mongodb/client/model/CreateCollectionOptions.html">}}) class. 

```java
database.createCollection("myColl", new CreateCollectionOptions().collation(
                              Collation.builder()
                                    .collationStrength(CollationStrength.PRIMARY)
                                    .locale("fr").build()));
```

#### Index Level

To specify collation for an index, pass a `Collation` object as an option to the `createIndex()` method. To specify index options, use the [IndexOptions]({{<apiref "com/mongodb/client/model/IndexOptions.html">}}) class. 

```java
IndexOptions collationIndexOptions = new IndexOptions().name("collation-fr")
                                       .collation(Collation.builder()
                                       .collationStrength(CollationStrength.SECONDARY)
                                       .locale("fr").build());

collection.createIndex(
        Indexes.ascending("name"), collationIndexOptions);
```

#### Operation Level

The following operations support collation by specifying the
`Collation` object to their respective Iterable object.

- `MongoCollection.aggregate()`
   
- `MongoCollection.distinct()`
   
- `MongoCollection.find()`

- `MongoCollection.mapReduce()`

For example:

```java
Collation collation = Collation.builder()
                                 .collationStrength(CollationStrength.SECONDARY)
                                 .locale("fr").build();

collection.find(Filters.eq("category", "cafe")).collation(collation);

collection.aggregate(Arrays.asList(
                         Aggregates.group("$category", Accumulators.sum("count", 1))))
          .collation(collation);

```

The following operations support collation by specifying the
`Collation` object as an option using the corresponding option class
for the method:

- `MongoCollection.count()`
- `MongoCollection.deleteOne()`
- `MongoCollection.deleteMany()`
- `MongoCollection.findOneAndDelete()` 
- `MongoCollection.findOneAndReplace()`
- `MongoCollection.findOneAndUpdate()`
- `MongoCollection.updateOne()`
- `MongoCollection.updateMany()`
- `MongoCollection.bulkWrite()` for:

   - `DeleteManyModel` and `DeleteOneModel` using `DeleteOptions` to specify the collation
   - `ReplaceOneModel`, `UpdateManyModel`, and `UpdateOneModel` using `UpdateOptions` to specify the collation.

For example:

```java
Collation collation = Collation.builder()
                                 .collationStrength(CollationStrength.SECONDARY)
                                 .locale("fr").build();

collection.count(Filters.eq("category", "cafe"), new CountOptions().collation(collation));

collection.updateOne(Filters.eq("category", "cafe"), set("stars", 1), 
                     new UpdateOptions().collation(collation));

collection.bulkWrite(Arrays.asList(
                new UpdateOneModel<>(new Document("category", "cafe"),
                                     new Document("$set", new Document("x", 0)), 
                                     new UpdateOptions().collation(collation)),
                new DeleteOneModel<>(new Document("category", "cafe"), 
                                     new DeleteOptions().collation(collation))));
```
  

For more information on collation, including the supported locales, refer to the
[manual]({{<docsref "reference/collation/">}}).

### Other MongoDB 3.4 features

* Support for specification of
[maximum staleness for secondary reads](https://github.com/mongodb/specifications/blob/master/source/max-staleness/max-staleness.rst)
* Support for the
[MongoDB handshake protocol](https://github.com/mongodb/specifications/blob/master/source/mongodb-handshake/handshake.rst).
* Builders for [eight new aggregation pipeline stages]({{<docsref "release-notes/3.4/#aggregation">}})
* Helpers for creating [read-only views]({{<docsref "release-notes/3.4/#views">}})
* Support for the [linearizable read concern](https://docs.mongodb.com/master/release-notes/3.4/#linearizable-read-concern)

### Support for JNDI

The synchronous driver now includes a [JNDI]({{<ref "driver/tutorials/jndi.md">}}) ObjectFactory implementation.


## Upgrading

See the [upgrading guide]({{<ref "upgrading.md">}}) on how to upgrade to 3.5.
