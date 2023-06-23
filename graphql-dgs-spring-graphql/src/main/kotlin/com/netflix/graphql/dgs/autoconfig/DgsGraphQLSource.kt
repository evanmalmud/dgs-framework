/*
 * Copyright 2023 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.graphql.dgs.autoconfig

import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor
import com.netflix.graphql.dgs.internal.DgsSchemaProvider
import graphql.GraphQL
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.ExecutionIdProvider
import graphql.execution.ExecutionStrategy
import graphql.execution.SubscriptionExecutionStrategy
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.preparsed.PreparsedDocumentProvider
import graphql.schema.GraphQLSchema
import org.springframework.graphql.execution.GraphQlSource
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class DgsGraphQLSource(
    private val schemaProvider: DgsSchemaProvider,
    private val instrumentation: Instrumentation?,
    private val dataFetcherExceptionHandler: DataFetcherExceptionHandler,
    private val queryExecutionStrategy: ExecutionStrategy,
    private val mutationExecutionStrategy: ExecutionStrategy,
    private val idProvider: Optional<ExecutionIdProvider>,
    private val reloadIndicator: DefaultDgsQueryExecutor.ReloadSchemaIndicator = DefaultDgsQueryExecutor.ReloadSchemaIndicator { false },
    private val preparsedDocumentProvider: PreparsedDocumentProvider? = null
) : GraphQlSource {

    private var schema = AtomicReference(schemaProvider.schema())
    override fun graphQl(): GraphQL {
        val graphQLSchema =
            if (reloadIndicator.reloadSchema()) {
                schema.updateAndGet { schemaProvider.schema() }
            } else {
                schema.get()
            }

        val graphQLBuilder = GraphQL.newGraphQL(graphQLSchema)
            .preparsedDocumentProvider(preparsedDocumentProvider)
            .instrumentation(instrumentation)
            .queryExecutionStrategy(queryExecutionStrategy)
            .mutationExecutionStrategy(mutationExecutionStrategy)
            .subscriptionExecutionStrategy(SubscriptionExecutionStrategy())
            .defaultDataFetcherExceptionHandler(dataFetcherExceptionHandler)
        if (idProvider.isPresent) {
            graphQLBuilder.executionIdProvider(idProvider.get())
        }
        return graphQLBuilder.build()
    }

    override fun schema(): GraphQLSchema {
        if (reloadIndicator.reloadSchema()) {
            return schema.updateAndGet { schemaProvider.schema() }
        } else {
            return schema.get()
        }
    }
}
