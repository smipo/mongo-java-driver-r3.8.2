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

package com.mongodb.async.client;

import com.mongodb.MongoClientException;
import com.mongodb.MongoException;
import com.mongodb.MongoInternalException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.binding.AsyncClusterBinding;
import com.mongodb.binding.AsyncReadBinding;
import com.mongodb.binding.AsyncReadWriteBinding;
import com.mongodb.binding.AsyncWriteBinding;
import com.mongodb.diagnostics.logging.Logger;
import com.mongodb.diagnostics.logging.Loggers;
import com.mongodb.lang.Nullable;
import com.mongodb.operation.AsyncReadOperation;
import com.mongodb.operation.AsyncWriteOperation;

import static com.mongodb.MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL;
import static com.mongodb.MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL;
import static com.mongodb.ReadPreference.primary;
import static com.mongodb.assertions.Assertions.notNull;
import static com.mongodb.internal.async.ErrorHandlingResultCallback.errorHandlingCallback;

class OperationExecutorImpl implements OperationExecutor {
    private static final Logger LOGGER = Loggers.getLogger("client");
    private final MongoClientImpl mongoClient;
    private final ClientSessionHelper clientSessionHelper;

    OperationExecutorImpl(final MongoClientImpl mongoClient, final ClientSessionHelper clientSessionHelper) {
        this.mongoClient = mongoClient;
        this.clientSessionHelper = clientSessionHelper;
    }

    @Override
    public <T> void execute(final AsyncReadOperation<T> operation, final ReadPreference readPreference, final ReadConcern readConcern,
                            final SingleResultCallback<T> callback) {
        execute(operation, readPreference, readConcern, null, callback);
    }

    @Override
    public <T> void execute(final AsyncReadOperation<T> operation, final ReadPreference readPreference, final ReadConcern readConcern,
                            @Nullable final ClientSession session, final SingleResultCallback<T> callback) {
        notNull("operation", operation);
        notNull("readPreference", readPreference);
        notNull("callback", callback);
        final SingleResultCallback<T> errHandlingCallback = errorHandlingCallback(callback, LOGGER);
        clientSessionHelper.withClientSession(session, this, new SingleResultCallback<ClientSession>(){
            @Override
            public void onResult(final ClientSession clientSession, final Throwable t) {
                if (t != null) {
                    errHandlingCallback.onResult(null, t);
                } else {
                    final AsyncReadBinding binding = getReadWriteBinding(readPreference, readConcern, clientSession,
                            session == null && clientSession != null);
                    if (session != null && session.hasActiveTransaction() && !binding.getReadPreference().equals(primary())) {
                        throw new MongoClientException("Read preference in a transaction must be primary");
                    }
                    operation.executeAsync(binding, new SingleResultCallback<T>() {
                        @Override
                        public void onResult(final T result, final Throwable t) {
                            try {
                                labelException(t, session);
                                errHandlingCallback.onResult(result, t);
                            } finally {
                                binding.release();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public <T> void execute(final AsyncWriteOperation<T> operation, final ReadConcern readConcern, final SingleResultCallback<T> callback) {
        execute(operation, readConcern, null, callback);
    }

    @Override
    public <T> void execute(final AsyncWriteOperation<T> operation, final ReadConcern readConcern, @Nullable final ClientSession session,
                            final SingleResultCallback<T> callback) {
        notNull("operation", operation);
        notNull("callback", callback);
        final SingleResultCallback<T> errHandlingCallback = errorHandlingCallback(callback, LOGGER);
        clientSessionHelper.withClientSession(session, this, new SingleResultCallback<ClientSession>() {
            @Override
            public void onResult(final ClientSession clientSession, final Throwable t) {
                if (t != null) {
                    errHandlingCallback.onResult(null, t);
                } else {
                    final AsyncWriteBinding binding = getReadWriteBinding(ReadPreference.primary(), readConcern, clientSession,
                            session == null && clientSession != null);
                    operation.executeAsync(binding, new SingleResultCallback<T>() {
                        @Override
                        public void onResult(final T result, final Throwable t) {
                            try {
                                labelException(t, session);
                                errHandlingCallback.onResult(result, t);
                            } finally {
                                binding.release();
                            }
                        }
                    });
                }
            }
        });
    }


    private void labelException(final Throwable t, final ClientSession session) {
        if ((t instanceof MongoSocketException || t instanceof MongoTimeoutException)
                && session != null && session.hasActiveTransaction()
                && !((MongoException) t).hasErrorLabel(UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL)) {
            ((MongoException) t).addLabel(TRANSIENT_TRANSACTION_ERROR_LABEL);
        }
    }

    private AsyncReadWriteBinding getReadWriteBinding(final ReadPreference readPreference, final ReadConcern readConcern,
                                                      @Nullable final ClientSession session, final boolean ownsSession) {
        notNull("readPreference", readPreference);
        AsyncReadWriteBinding readWriteBinding = new AsyncClusterBinding(mongoClient.getCluster(),
                getReadPreferenceForBinding(readPreference, session), readConcern);
        if (session != null) {
            readWriteBinding = new ClientSessionBinding(session, ownsSession, readWriteBinding);
        }
        return readWriteBinding;
    }

    private ReadPreference getReadPreferenceForBinding(final ReadPreference readPreference, @Nullable final ClientSession session) {
        if (session == null) {
            return readPreference;
        }
        if (session.hasActiveTransaction()) {
            ReadPreference readPreferenceForBinding = session.getTransactionOptions().getReadPreference();
            if (readPreferenceForBinding == null) {
                throw new MongoInternalException("Invariant violated.  Transaction options read preference can not be null");
            }
            return readPreferenceForBinding;
        }
        return readPreference;
    }
}
