/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.client;

import com.mongodb.ClusterFixture;
import com.mongodb.MongoException;
import com.mongodb.MongoNamespace;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.client.test.CollectionHelper;
import com.mongodb.connection.ServerVersion;
import com.mongodb.event.CommandEvent;
import com.mongodb.internal.connection.TestCommandListener;
import com.mongodb.lang.Nullable;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.codecs.BsonDocumentCodec;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import util.JsonPoweredTestHelper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.mongodb.ClusterFixture.isDiscoverableReplicaSet;
import static com.mongodb.ClusterFixture.isSharded;
import static com.mongodb.ClusterFixture.isStandalone;
import static com.mongodb.client.CommandMonitoringTestHelper.getExpectedEvents;
import static com.mongodb.client.Fixture.getMongoClientSettingsBuilder;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

// See https://github.com/mongodb/specifications/tree/master/source/change-streams/tests
@RunWith(Parameterized.class)
public class ChangeStreamsTest {
    private final String filename;
    private final String description;
    private final MongoNamespace namespace;
    private final MongoNamespace namespace2;
    private final BsonDocument definition;

    private MongoClient mongoClient;
    private TestCommandListener commandListener;

    public ChangeStreamsTest(final String filename, final String description, final MongoNamespace namespace,
                             final MongoNamespace namespace2, final BsonDocument definition) {
        this.filename = filename;
        this.description = description;
        this.namespace = namespace;
        this.namespace2 = namespace2;
        this.definition = definition;
    }

    @BeforeClass
    public static void beforeClass() {
    }

    @AfterClass
    public static void afterClass() {
    }

    @Before
    public void setUp() {
        ServerVersion serverVersion = ClusterFixture.getServerVersion();
        if (definition.containsKey("minServerVersion")) {
            assumeTrue(serverVersion.compareTo(getServerVersion("minServerVersion")) > 0);
        }
        if (definition.containsKey("maxServerVersion")) {
            assumeTrue(serverVersion.compareTo(getServerVersion("maxServerVersion")) < 0);
        }
        if (definition.containsKey("topology")) {
            BsonArray topologyTypes = definition.getArray("topology");
            for (BsonValue type : topologyTypes) {
                String typeString = type.asString().getValue();
                if (typeString.equals("sharded")) {
                    assumeTrue(isSharded());
                } else if (typeString.equals("replicaset")) {
                    assumeTrue(isDiscoverableReplicaSet());
                } else if (typeString.equals("single")) {
                    assumeTrue(isStandalone());
                }
            }
        }

        CollectionHelper<BsonDocument> collectionHelper = new CollectionHelper<BsonDocument>(new BsonDocumentCodec(), namespace);
        collectionHelper.drop();
        collectionHelper.create();

        CollectionHelper<BsonDocument> collectionHelper2 = new CollectionHelper<BsonDocument>(new BsonDocumentCodec(), namespace2);
        collectionHelper2.drop();
        collectionHelper2.create();

        commandListener = new TestCommandListener();
        mongoClient = MongoClients.create(getMongoClientSettingsBuilder().addCommandListener(commandListener).build());
    }

    @After
    public void cleanUp() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Test
    public void shouldPassAllOutcomes() {
        BsonDocument result = definition.getDocument("result");
        MongoCursor<ChangeStreamDocument<BsonDocument>> cursor = createCursor(result);
        if (cursor == null) {
            return;
        }

        try {
            handleOperations();

            checkStreamValues(result, cursor);

        } finally {
            cursor.close();
        }

        checkExpectations();

    }

    private void checkStreamValues(final BsonDocument result, final MongoCursor<ChangeStreamDocument<BsonDocument>> cursor) {
        for (BsonValue success : result.getArray("success", new BsonArray())) {
            BsonDocument expected = success.asDocument();
            ChangeStreamDocument<BsonDocument> actual = cursor.next();

            BsonDocument ns = expected.getDocument("ns", new BsonDocument());
            MongoNamespace expectedNamespace = new MongoNamespace(
                    ns.getString("db", new BsonString("db")).getValue(),
                    ns.getString("coll", new BsonString("coll")).getValue());
            assertEquals(expectedNamespace, actual.getNamespace());
            assertEquals(OperationType.fromString(expected.getString("operationType").getValue()), actual.getOperationType());
            actual.getFullDocument().remove("_id");


            assertEquals(expected.get("fullDocument"), actual.getFullDocument());
        }
    }

    @Nullable
    private MongoCursor<ChangeStreamDocument<BsonDocument>> createCursor(final BsonDocument result) {
        MongoCursor<ChangeStreamDocument<BsonDocument>> cursor;
        try {
            cursor = createChangeStreamCursor();
        } catch (MongoException e) {
            assertEquals(result.getDocument("error", new BsonDocument()).getInt32("code", new BsonInt32(-1)).getValue(), e.getCode());
            return null;
        }
        return cursor;
    }

    private void checkExpectations() {
        if (definition.getArray("expectations").size() > 0) {

            String database = definition.getString("target").getValue().equals("client") ? "admin" : namespace.getDatabaseName();
            List<CommandEvent> expectedEvents = getExpectedEvents(definition.getArray("expectations"), database, new BsonDocument());
            List<CommandEvent> events = commandListener.getEvents();

            for (int i = 0; i < expectedEvents.size(); i++) {
                CommandEvent expectedEvent = expectedEvents.get(i);
                CommandEvent event = events.get(i);
                CommandMonitoringTestHelper.assertEventsEquality(singletonList(expectedEvent), singletonList(event));
            }
        }
    }


    private MongoCursor<ChangeStreamDocument<BsonDocument>> createChangeStreamCursor() {
        String target = definition.getString("target").getValue();
        List<BsonDocument> pipeline = new ArrayList<BsonDocument>();
        for (BsonValue bsonValue : definition.getArray("changeStreamPipeline", new BsonArray())) {
            pipeline.add(bsonValue.asDocument());
        }

        if (target.equals("client")) {
            return mongoClient.watch(pipeline, BsonDocument.class).iterator();
        } else if (target.equals("database")) {
            return mongoClient.getDatabase(namespace.getDatabaseName()).watch(pipeline, BsonDocument.class).iterator();
        } else if (target.equals("collection")) {
            return mongoClient.getDatabase(namespace.getDatabaseName()).getCollection(namespace.getCollectionName())
                    .watch(pipeline, BsonDocument.class).iterator();
        } else {
            throw new IllegalArgumentException(format("Unknown target: %s", target));
        }
    }

    private void handleOperations() {
        for (BsonValue operations : definition.getArray("operations")) {
            BsonDocument op = operations.asDocument();
            MongoNamespace opNamespace = new MongoNamespace(op.getString("database").getValue(), op.getString("collection").getValue());
            createJsonPoweredCrudTestHelper(Fixture.getMongoClient(), opNamespace).getOperationResults(op);
        }
    }

    private JsonPoweredCrudTestHelper createJsonPoweredCrudTestHelper(final MongoClient localMongoClient, final MongoNamespace namespace) {
        return new JsonPoweredCrudTestHelper(description, localMongoClient.getDatabase(namespace.getDatabaseName()),
                localMongoClient.getDatabase(namespace.getDatabaseName()).getCollection(namespace.getCollectionName(), BsonDocument.class));
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() throws URISyntaxException, IOException {
        List<Object[]> data = new ArrayList<Object[]>();
        for (File file : JsonPoweredTestHelper.getTestFiles("/change-streams")) {
            BsonDocument testDocument = JsonPoweredTestHelper.getTestDocument(file);
            MongoNamespace namespace = new MongoNamespace(testDocument.getString("database_name").getValue(),
                    testDocument.getString("collection_name").getValue());
            MongoNamespace namespace2 = new MongoNamespace(testDocument.getString("database_name").getValue(),
                    testDocument.getString("collection_name").getValue());
            for (BsonValue test : testDocument.getArray("tests")) {
                data.add(new Object[]{file.getName(), test.asDocument().getString("description").getValue(),
                        namespace, namespace2, test.asDocument()});
            }
        }
        return data;
    }

    private ServerVersion getServerVersion(final String fieldName) {
        String[] versionStringArray = definition.getString(fieldName).getValue().split("\\.");
        return new ServerVersion(Integer.parseInt(versionStringArray[0]), Integer.parseInt(versionStringArray[1]));
    }
}
