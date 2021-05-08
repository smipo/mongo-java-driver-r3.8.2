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

package com.mongodb.operation;

import com.mongodb.MongoCommandException;
import com.mongodb.WriteConcern;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.binding.AsyncWriteBinding;
import com.mongodb.binding.WriteBinding;
import com.mongodb.connection.AsyncConnection;
import com.mongodb.connection.Connection;
import com.mongodb.connection.ConnectionDescription;
import org.bson.BsonDocument;
import org.bson.BsonString;

import static com.mongodb.assertions.Assertions.notNull;
import static com.mongodb.internal.async.ErrorHandlingResultCallback.errorHandlingCallback;
import static com.mongodb.operation.CommandOperationHelper.executeWrappedCommandProtocol;
import static com.mongodb.operation.CommandOperationHelper.executeWrappedCommandProtocolAsync;
import static com.mongodb.operation.OperationHelper.AsyncCallableWithConnection;
import static com.mongodb.operation.OperationHelper.CallableWithConnection;
import static com.mongodb.operation.OperationHelper.LOGGER;
import static com.mongodb.operation.OperationHelper.releasingCallback;
import static com.mongodb.operation.OperationHelper.withConnection;
import static com.mongodb.operation.UserOperationHelper.translateUserCommandException;
import static com.mongodb.operation.UserOperationHelper.userCommandCallback;
import static com.mongodb.internal.operation.WriteConcernHelper.appendWriteConcernToCommand;
import static com.mongodb.operation.CommandOperationHelper.writeConcernErrorTransformer;

/**
 * An operation to remove a user.
 *
 * @since 3.0
 * @deprecated use {@link CommandWriteOperation} directly or the mongod shell helpers.
 */
@Deprecated
public class DropUserOperation implements AsyncWriteOperation<Void>, WriteOperation<Void> {
    private final String databaseName;
    private final String userName;
    private final WriteConcern writeConcern;

    /**
     * Construct a new instance.
     *
     * @param databaseName the name of the database for the operation.
     * @param userName     the name of the user to be dropped.
     */
    public DropUserOperation(final String databaseName, final String userName) {
        this(databaseName, userName, null);
    }

    /**
     * Construct a new instance.
     *
     * @param databaseName the name of the database for the operation.
     * @param userName     the name of the user to be dropped.
     * @param writeConcern the write concern
     *
     * @since 3.4
     */
    public DropUserOperation(final String databaseName, final String userName, final WriteConcern writeConcern) {
        this.databaseName = notNull("databaseName", databaseName);
        this.userName = notNull("userName", userName);
        this.writeConcern = writeConcern;
    }

    @Override
    public Void execute(final WriteBinding binding) {
        return withConnection(binding, new CallableWithConnection<Void>() {
            @Override
            public Void call(final Connection connection) {
                try {
                    executeWrappedCommandProtocol(binding, databaseName, getCommand(connection.getDescription()), connection,
                            writeConcernErrorTransformer());
                } catch (MongoCommandException e) {
                    translateUserCommandException(e);
                }
                return null;
            }
        });
    }

    @Override
    public void executeAsync(final AsyncWriteBinding binding, final SingleResultCallback<Void> callback) {
        withConnection(binding, new AsyncCallableWithConnection() {
            @Override
            public void call(final AsyncConnection connection, final Throwable t) {
                SingleResultCallback<Void> errHandlingCallback = errorHandlingCallback(callback, LOGGER);
                if (t != null) {
                    errHandlingCallback.onResult(null, t);
                } else {
                    final SingleResultCallback<Void> wrappedCallback = releasingCallback(errHandlingCallback, connection);
                    executeWrappedCommandProtocolAsync(binding, databaseName, getCommand(connection.getDescription()), connection,
                            writeConcernErrorTransformer(), userCommandCallback(wrappedCallback));

                }
            }
        });
    }

    private BsonDocument getCommand(final ConnectionDescription description) {
        BsonDocument commandDocument = new BsonDocument("dropUser", new BsonString(userName));
        appendWriteConcernToCommand(writeConcern, commandDocument, description);
        return commandDocument;
    }
}
