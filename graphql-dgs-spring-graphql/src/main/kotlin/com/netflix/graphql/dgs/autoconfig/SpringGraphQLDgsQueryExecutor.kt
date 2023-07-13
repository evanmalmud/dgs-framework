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

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.TypeRef
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.internal.DefaultDgsGraphQLContextBuilder
import com.netflix.graphql.dgs.internal.DgsDataLoaderProvider
import com.netflix.graphql.dgs.internal.DgsWebMvcRequestData
import graphql.ExecutionResult
import graphql.GraphQLContext
import graphql.GraphQLError
import org.springframework.graphql.execution.DefaultExecutionGraphQlService
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import java.util.concurrent.CompletableFuture

class SpringGraphQLDgsQueryExecutor(val executionService: DefaultExecutionGraphQlService, private val dgsContextBuilder: DefaultDgsGraphQLContextBuilder, private val dgsDataLoaderProvider: DgsDataLoaderProvider) : DgsQueryExecutor {

    override fun execute(
        query: String?,
        variables: Map<String, Any>,
        extensions: Map<String, Any>?,
        headers: HttpHeaders?,
        operationName: String?,
        webRequest: WebRequest?
    ): ExecutionResult {
        val request = DefaultExecutionGraphQlRequest(
            query,
            operationName,
            variables,
            extensions,
            "",
            null
        )

        val dgsContext = dgsContextBuilder.build(DgsWebMvcRequestData(request.extensions, headers, webRequest))
        val graphQLContextFuture = CompletableFuture<GraphQLContext>()
        val dataLoaderRegistry = dgsDataLoaderProvider.buildRegistryWithContextSupplier { graphQLContextFuture.get() }

        request.configureExecutionInput { _, builder ->
            builder
                .context(dgsContext)
                .graphQLContext(dgsContext)
                .dataLoaderRegistry(dataLoaderRegistry).build()
        }

        val execute = executionService.execute(
            request
        ).block() ?: throw IllegalStateException("Unexpected null response from Spring GraphQL client")

        return ExecutionResult.newExecutionResult()
            .data(execute.getData())
            .errors(execute.errors.map { GraphQLError.newError().message(it.message).build() })
            .build()
    }

    override fun <T : Any?> executeAndExtractJsonPath(
        query: String,
        jsonPath: String,
        variables: MutableMap<String, Any>
    ): T {
        TODO("Not yet implemented")
//        return graphqlClient.document(query).variables(variables).retrieve(jsonPath).toEntity(object: ParameterizedTypeReference<T>(){}).block()!!
    }

    override fun <T : Any?> executeAndExtractJsonPath(query: String?, jsonPath: String?, headers: HttpHeaders?): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> executeAndExtractJsonPath(
        query: String?,
        jsonPath: String?,
        servletWebRequest: ServletWebRequest?
    ): T {
        TODO("Not yet implemented")
    }

    override fun executeAndGetDocumentContext(query: String?, variables: MutableMap<String, Any>?): DocumentContext {
        TODO("Not yet implemented")
    }

    override fun executeAndGetDocumentContext(
        query: String?,
        variables: MutableMap<String, Any>?,
        headers: HttpHeaders?
    ): DocumentContext {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> executeAndExtractJsonPathAsObject(
        query: String?,
        jsonPath: String?,
        variables: MutableMap<String, Any>?,
        clazz: Class<T>?,
        headers: HttpHeaders?
    ): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> executeAndExtractJsonPathAsObject(
        query: String?,
        jsonPath: String?,
        variables: MutableMap<String, Any>?,
        typeRef: TypeRef<T>?,
        headers: HttpHeaders?
    ): T {
        TODO("Not yet implemented")
    }
}