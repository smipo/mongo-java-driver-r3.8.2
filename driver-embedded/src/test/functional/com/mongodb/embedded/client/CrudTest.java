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

package com.mongodb.embedded.client;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.junit.After;
import org.junit.Before;
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

import static com.mongodb.embedded.client.Fixture.serverVersionGreaterThan;
import static com.mongodb.embedded.client.Fixture.serverVersionLessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

// See https://github.com/mongodb/specifications/tree/master/source/crud/tests
@RunWith(Parameterized.class)
public class CrudTest extends DatabaseTestCase {
    private final String filename;
    private final String description;
    private final BsonArray data;
    private final BsonDocument definition;
    private MongoDatabase database;
    private MongoCollection<BsonDocument> collection;
    private JsonPoweredCrudTestHelper helper;

    public CrudTest(final String filename, final String description, final BsonArray data, final BsonDocument definition) {
        this.filename = filename;
        this.description = description;
        this.data = data;
        this.definition = definition;
    }

    @Before
    public void setUp() {
        database = Fixture.getDefaultDatabase();

        collection = database.getCollection(getClass().getName(), BsonDocument.class);
        List<BsonDocument> documents = new ArrayList<BsonDocument>();
        for (BsonValue document: data) {
            documents.add(document.asDocument());
        }
        collection.insertMany(documents);
        helper = new JsonPoweredCrudTestHelper(description, collection);
    }


    @After
    public void tearDown() {
        if (collection != null) {
            collection.drop();
        }
    }

    @Test
    public void shouldPassAllOutcomes() {
        if (definition.containsKey("minServerVersion") && serverVersionLessThan(definition.getString("minServerVersion").getValue())) {
            assumeTrue("Server less than min version", false);
        }
        if (definition.containsKey("maxServerVersion") && serverVersionGreaterThan(definition.getString("maxServerVersion").getValue())) {
            assumeTrue("Server greater than max version", false);
        }

        assumeTrue(database != null);
        BsonDocument outcome = helper.getOperationResults(definition.getDocument("operation"));
        BsonDocument expectedOutcome = definition.getDocument("outcome");

        // Hack to workaround the lack of upsertedCount
        BsonValue expectedResult = expectedOutcome.get("result");
        BsonValue actualResult = outcome.get("result");
        if (actualResult.isDocument()
                && actualResult.asDocument().containsKey("upsertedCount")
                && actualResult.asDocument().getNumber("upsertedCount").intValue() == 0
                && !expectedResult.asDocument().containsKey("upsertedCount")) {
            expectedResult.asDocument().append("upsertedCount", actualResult.asDocument().get("upsertedCount"));
        }

        // Remove insertCount
        if (actualResult.isDocument() && actualResult.asDocument().containsKey("insertedCount")) {
            actualResult.asDocument().remove("insertedCount");
        }

        assertEquals(description, expectedResult, actualResult);

        if (expectedOutcome.containsKey("collection")) {
            assertCollectionEquals(expectedOutcome.getDocument("collection"));
        }
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() throws URISyntaxException, IOException {
        List<Object[]> data = new ArrayList<Object[]>();
        for (File file : JsonPoweredTestHelper.getTestFiles("/crud")) {
            BsonDocument testDocument = JsonPoweredTestHelper.getTestDocument(file);
            for (BsonValue test: testDocument.getArray("tests")) {
                data.add(new Object[]{file.getName(), test.asDocument().getString("description").getValue(),
                        testDocument.getArray("data"), test.asDocument()});
            }
        }
        return data;
    }

    private void assertCollectionEquals(final BsonDocument expectedCollection) {
        MongoCollection<BsonDocument> collectionToCompare = collection;
        if (expectedCollection.containsKey("name")) {
            collectionToCompare = database.getCollection(expectedCollection.getString("name").getValue(), BsonDocument.class);
        }
        assertEquals(description, expectedCollection.getArray("data"), collectionToCompare.find().into(new BsonArray()));
    }
}
