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

import com.mongodb.BasicDBObject
import com.mongodb.DefaultDBEncoder
import com.mongodb.WriteConcern
import spock.lang.Specification

class DBCollectionUpdateOptionsSpecification extends Specification {

    def 'should have the expected default values'() {
        when:
        def options = new DBCollectionUpdateOptions()

        then:
        !options.isMulti()
        !options.isUpsert()
        options.getArrayFilters() == null
        options.getBypassDocumentValidation() == null
        options.getEncoder() == null
        options.getWriteConcern() == null
    }

    def 'should set and return the expected values'() {
        given:
        def writeConcern = WriteConcern.MAJORITY
        def encoder = new DefaultDBEncoder()
        def arrayFilters = [new BasicDBObject('i.b', 1)]

        when:
        def options = new DBCollectionUpdateOptions()
                .bypassDocumentValidation(true)
                .encoder(encoder)
                .multi(true)
                .upsert(true)
                .arrayFilters(arrayFilters)
                .writeConcern(writeConcern)

        then:
        options.getBypassDocumentValidation()
        options.getEncoder() == encoder
        options.getWriteConcern() == writeConcern
        options.isMulti()
        options.isUpsert()
        options.getArrayFilters() == arrayFilters
    }

}
