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

package com.mongodb.client.internal;

import com.mongodb.Block;
import com.mongodb.Function;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.lang.Nullable;
import com.mongodb.operation.BatchCursor;
import com.mongodb.operation.ReadOperation;

import java.util.Collection;

import static com.mongodb.assertions.Assertions.notNull;

/**
 * This class is not part of the public API and may be removed or changed at any time.
 *
 * @param <TResult> the result type
 */
public abstract class MongoIterableImpl<TResult> implements MongoIterable<TResult> {
    private final ClientSession clientSession;
    private final ReadConcern readConcern;
    private final OperationExecutor executor;
    private final ReadPreference readPreference;
    private Integer batchSize;

    public MongoIterableImpl(@Nullable final ClientSession clientSession, final OperationExecutor executor, final ReadConcern readConcern,
                             final ReadPreference readPreference) {
        this.clientSession = clientSession;
        this.executor = notNull("executor", executor);
        this.readConcern = notNull("readConcern", readConcern);
        this.readPreference = notNull("readPreference", readPreference);
    }

    public abstract ReadOperation<BatchCursor<TResult>> asReadOperation();

    @Nullable
    ClientSession getClientSession() {
        return clientSession;
    }

    OperationExecutor getExecutor() {
        return executor;
    }

    ReadPreference getReadPreference() {
        return readPreference;
    }

    protected ReadConcern getReadConcern() {
        return readConcern;
    }

    @Nullable
    public Integer getBatchSize() {
        return batchSize;
    }

    @Override
    public MongoIterable<TResult> batchSize(final int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    @Override
    public MongoCursor<TResult> iterator() {
        return new MongoBatchCursorAdapter<TResult>(execute());
    }

   // @Override
    @Override
    public MongoCursor<TResult> iterator(ReadPreference readPreference) {
        return new MongoBatchCursorAdapter<TResult>(execute(readPreference));
    }

    @Nullable
    @Override
    public TResult first() {
        return first(null);
    }

    @Override
    @Nullable
   // @Override
    public TResult first(ReadPreference readPreference) {
        MongoCursor<TResult> cursor = iterator(readPreference);
        try {
            if (!cursor.hasNext()) {
                return null;
            }
            return cursor.next();
        } finally {
            cursor.close();
        }
    }

    @Override
    public <U> MongoIterable<U> map(final Function<TResult, U> mapper) {
        return new MappingIterable<TResult, U>(this, mapper);
    }

    @Override
    public void forEach(final Block<? super TResult> block) {
        MongoCursor<TResult> cursor = iterator();
        try {
            while (cursor.hasNext()) {
                block.apply(cursor.next());
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    public <A extends Collection<? super TResult>> A into(final A target) {
        forEach(new Block<TResult>() {
            @Override
            public void apply(final TResult t) {
                target.add(t);
            }
        });
        return target;
    }

    private BatchCursor<TResult> execute() {
        return execute(null);
    }

    private BatchCursor<TResult> execute(ReadPreference originalReadPreference) {
        return executor.execute(asReadOperation(), originalReadPreference == null ? readPreference : originalReadPreference, readConcern, clientSession);
    }
}
