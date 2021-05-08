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

package com.mongodb.client.model

import org.bson.BsonDocument
import spock.lang.Specification

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static java.util.concurrent.TimeUnit.SECONDS

class FindOneAndReplaceOptionsSpecification extends Specification {

    def 'should have the expected defaults'() {
        when:
        def options = new FindOneAndReplaceOptions()

        then:
        options.getCollation() == null
        options.getMaxTime(MILLISECONDS) == 0
        options.getProjection() == null
        options.getSort() == null
        options.getBypassDocumentValidation() == null
        options.getReturnDocument() ==  ReturnDocument.BEFORE
        options.isUpsert() == false
    }

    def 'should set collation'() {
        expect:
        new FindOneAndReplaceOptions().collation(collation).getCollation() == collation

        where:
        collation << [null, Collation.builder().locale('en').build()]
    }

    def 'should set projection'() {
        expect:
        new FindOneAndReplaceOptions().projection(projection).getProjection() == projection

        where:
        projection << [null, BsonDocument.parse('{ a: 1}')]
    }

    def 'should set sort'() {
        expect:
        new FindOneAndReplaceOptions().sort(sort).getSort() == sort

        where:
        sort << [null, BsonDocument.parse('{ a: 1}')]
    }

    def 'should convert maxTime'() {
        when:
        def options = new FindOneAndReplaceOptions()

        then:
        options.getMaxTime(SECONDS) == 0

        when:
        options.maxTime(100, MILLISECONDS)

        then:
        options.getMaxTime(MILLISECONDS) == 100

        when:
        options.maxTime(1004, MILLISECONDS)

        then:
        options.getMaxTime(SECONDS) == 1
    }

    def 'should set upsert'() {
        expect:
        new FindOneAndReplaceOptions().upsert(upsert).isUpsert() == upsert

        where:
        upsert << [true, false]
    }

    def 'should set bypassDocumentValidation'() {
        expect:
        new FindOneAndReplaceOptions().bypassDocumentValidation(bypassValidation).getBypassDocumentValidation() == bypassValidation

        where:
        bypassValidation << [null, true, false]
    }

    def 'should set returnDocument'() {
        expect:
        new FindOneAndReplaceOptions().returnDocument(returnDocument).getReturnDocument() == returnDocument

        where:
        returnDocument << [ReturnDocument.BEFORE, ReturnDocument.AFTER]
    }
}
