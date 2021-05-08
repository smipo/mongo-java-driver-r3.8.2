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

package com.mongodb

import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonString
import spock.lang.Specification

import static java.util.Arrays.asList
import static java.util.Collections.emptyList
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static java.util.concurrent.TimeUnit.SECONDS
import static org.bson.BsonDocument.parse

@SuppressWarnings(['DuplicateMapLiteral', 'LineLength'])
class ReadPreferenceSpecification extends Specification {

    def 'should have correct names'() {
        expect:
        readPreference.getName() == name

        where:
        readPreference                      | name
        ReadPreference.primary()            | 'primary'
        ReadPreference.primaryPreferred()   | 'primaryPreferred'
        ReadPreference.secondary()          | 'secondary'
        ReadPreference.secondaryPreferred() | 'secondaryPreferred'
        ReadPreference.nearest()            | 'nearest'
    }

    static final TAG_SET = new TagSet(new Tag('rack', '1'))
    static final TAG_SET_LIST = [TAG_SET]

    def 'should have correct max staleness and tag set list'() {
        given:

        expect:
        ((TaggableReadPreference) readPreference).getMaxStaleness(MILLISECONDS) == (Long) maxStalenessMS
        ((TaggableReadPreference) readPreference).getTagSetList() == tagSetList

        where:
        readPreference                                                 | maxStalenessMS | tagSetList
        ReadPreference.primaryPreferred()                              | null           | emptyList()
        ReadPreference.secondary()                                     | null           | emptyList()
        ReadPreference.secondaryPreferred()                            | null           | emptyList()
        ReadPreference.nearest()                                       | null           | emptyList()
        ReadPreference.secondary(10, SECONDS)                          | 10000          | emptyList()
        ReadPreference.secondaryPreferred(10, SECONDS)                 | 10000          | emptyList()
        ReadPreference.primaryPreferred(10, SECONDS)                   | 10000          | emptyList()
        ReadPreference.nearest(10, SECONDS)                            | 10000          | emptyList()
        ReadPreference.secondary(TAG_SET, 10, SECONDS)                 | 10000          | TAG_SET_LIST
        ReadPreference.secondaryPreferred(TAG_SET, 10, SECONDS)        | 10000          | TAG_SET_LIST
        ReadPreference.primaryPreferred(TAG_SET, 10, SECONDS)          | 10000          | TAG_SET_LIST
        ReadPreference.nearest(TAG_SET, 10, SECONDS)                   | 10000          | TAG_SET_LIST
        ReadPreference.secondary(TAG_SET_LIST, 10, SECONDS)            | 10000          | TAG_SET_LIST
        ReadPreference.secondaryPreferred(TAG_SET_LIST, 10, SECONDS)   | 10000          | TAG_SET_LIST
        ReadPreference.primaryPreferred(TAG_SET_LIST, 10, SECONDS)     | 10000          | TAG_SET_LIST
        ReadPreference.nearest(TAG_SET_LIST, 10, SECONDS)              | 10000          | TAG_SET_LIST
    }

    def 'should throw if max staleness is negative'() {
        when:
        ReadPreference.secondary(-1, SECONDS)

        then:
        thrown(IllegalArgumentException)
    }

    def 'should have correct valueOf'() {
        expect:
        ReadPreference.primary() == ReadPreference.valueOf('primary')
        ReadPreference.secondary() == ReadPreference.valueOf('secondary')
        ReadPreference.primaryPreferred() == ReadPreference.valueOf('primaryPreferred')
        ReadPreference.secondaryPreferred() == ReadPreference.valueOf('secondaryPreferred')
        ReadPreference.nearest() == ReadPreference.valueOf('nearest')
    }

    def 'valueOf should throw with null name'() {
        when:
        ReadPreference.valueOf(null)

        then:
        thrown(IllegalArgumentException)

        when:
        ReadPreference.valueOf(null, asList(new TagSet(new Tag('dc', 'ny'))))

        then:
        thrown(IllegalArgumentException)
    }

    def 'valueOf should throw with unexpected name'() {
        when:
        ReadPreference.valueOf('unknown')

        then:
        thrown(IllegalArgumentException)

        when:
        ReadPreference.valueOf(null, asList(new TagSet(new Tag('dc', 'ny'))))

        then:
        thrown(IllegalArgumentException)

        when:
        ReadPreference.valueOf('primary', asList(new TagSet(new Tag('dc', 'ny'))))

        then:
        thrown(IllegalArgumentException)
    }

    def 'should have correct valueOf with tag set list'() {
        def tags = [new TagSet([new Tag('dy', 'ny'), new Tag('rack', '1')]), new TagSet([new Tag('dy', 'ca'), new Tag('rack', '2')])]

        expect:
        ReadPreference.secondary(tags) == ReadPreference.valueOf('secondary', tags)
        ReadPreference.primaryPreferred(tags) == ReadPreference.valueOf('primaryPreferred', tags)
        ReadPreference.secondaryPreferred(tags) == ReadPreference.valueOf('secondaryPreferred', tags)
        ReadPreference.nearest(tags) == ReadPreference.valueOf('nearest', tags)
    }

    def 'should have correct valueOf with max staleness'() {
        expect:
        ReadPreference.secondary(10, SECONDS) == ReadPreference.valueOf('secondary', [], 10, SECONDS)
    }

    def 'should convert read preference with max staleness to correct documents'() {
        expect:
        readPreference.toDocument() == document

        where:
        readPreference                                 | document
        ReadPreference.primaryPreferred(10, SECONDS)   | parse('{mode : "primaryPreferred", maxStalenessSeconds : {$numberLong : "10" }}}')
        ReadPreference.secondary(10, SECONDS)          | parse('{mode : "secondary", maxStalenessSeconds : {$numberLong : "10" }}}')
        ReadPreference.secondaryPreferred(10, SECONDS) | parse('{mode : "secondaryPreferred", maxStalenessSeconds : {$numberLong : "10" }}}')
        ReadPreference.nearest(10, SECONDS)            | parse('{mode : "nearest", maxStalenessSeconds : {$numberLong : "10" }}}')
        ReadPreference.nearest(10005, MILLISECONDS)    | parse('{mode : "nearest", maxStalenessSeconds : {$numberLong : "10" }}}')
    }

    def 'should convert read preferences with a single tag set to correct documents'() {
        expect:
        readPreference.toDocument() == document

        where:
        readPreference                                                   | document
        ReadPreference.primary()                                         | new BsonDocument('mode', new BsonString('primary'))
        ReadPreference.primaryPreferred()                                | new BsonDocument('mode', new BsonString('primaryPreferred'))
        ReadPreference.secondary()                                       | new BsonDocument('mode', new BsonString('secondary'))
        ReadPreference.secondaryPreferred()                              | new BsonDocument('mode', new BsonString('secondaryPreferred'))
        ReadPreference.nearest()                                         | new BsonDocument('mode', new BsonString('nearest'))
        ReadPreference.primaryPreferred(
                new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')])) | new BsonDocument('mode', new BsonString('primaryPreferred'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1'))]))

        ReadPreference.secondary(
                new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')])) | new BsonDocument('mode', new BsonString('secondary'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1'))]))

        ReadPreference.secondaryPreferred(
                new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')])) | new BsonDocument('mode', new BsonString('secondaryPreferred'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1'))]))

        ReadPreference.nearest(
                new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')])) | new BsonDocument('mode', new BsonString('nearest'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1'))]))

        ReadPreference.nearest(
                new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')])) | new BsonDocument('mode', new BsonString('nearest'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1'))]))
    }


    def 'should convert read preferences with a tag set list to correct documents'() {
        expect:
        readPreference.toDocument() == document

        where:
        readPreference                                                     | document
        ReadPreference.primary()                                           | new BsonDocument('mode', new BsonString('primary'))
        ReadPreference.primaryPreferred()                                  | new BsonDocument('mode', new BsonString('primaryPreferred'))
        ReadPreference.secondary()                                         | new BsonDocument('mode', new BsonString('secondary'))
        ReadPreference.secondaryPreferred()                                | new BsonDocument('mode', new BsonString('secondaryPreferred'))
        ReadPreference.nearest()                                           | new BsonDocument('mode', new BsonString('nearest'))
        ReadPreference.primaryPreferred(
                [new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')])]) | new BsonDocument('mode', new BsonString('primaryPreferred'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1'))]))

        ReadPreference.secondary(
                [new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')])]) | new BsonDocument('mode', new BsonString('secondary'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1'))]))

        ReadPreference.secondaryPreferred(
                [new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')])]) | new BsonDocument('mode', new BsonString('secondaryPreferred'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1'))]))

        ReadPreference.nearest(
                [new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')])]) | new BsonDocument('mode', new BsonString('nearest'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1'))]))

        ReadPreference.nearest(
                [new TagSet([new Tag('dc', 'ny'), new Tag('rack', '1')]),
                 new TagSet([new Tag('dc', 'ca'), new Tag('rack', '2')])]) | new BsonDocument('mode', new BsonString('nearest'))
                .append('tags', new BsonArray([new BsonDocument('dc', new BsonString('ny')).append('rack', new BsonString('1')),
                                               new BsonDocument('dc', new BsonString('ca')).append('rack', new BsonString('2'))]))
    }

    def 'different read preferences should have different hash codes'() {
        expect:
        first.hashCode() != second.hashCode()

        where:
        first                                                         | second
        ReadPreference.primary()                                      | ReadPreference.secondary()
        ReadPreference.secondary()                                    | ReadPreference.nearest()
        ReadPreference.secondary()                                    | ReadPreference.secondary([new TagSet([new Tag('dc', 'ny')])])
        ReadPreference.secondary([new TagSet([new Tag('dc', 'ny')])]) | ReadPreference.secondary([new TagSet([new Tag('dc', 'la')])])
        ReadPreference.secondary()                                    | ReadPreference.secondary(1000, MILLISECONDS)
    }
}
