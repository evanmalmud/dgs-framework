/*
 * Copyright 2022 Netflix, Inc.
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
import com.netflix.graphql.dgs.internal.DefaultDgsGraphQLContextBuilder
import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor
import com.netflix.graphql.dgs.internal.DgsDataLoaderProvider
import com.netflix.graphql.dgs.internal.DgsSchemaProvider
import com.netflix.graphql.dgs.internal.method.ArgumentResolver
import com.netflix.graphql.dgs.mvc.internal.method.HandlerMethodArgumentResolverAdapter
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.AsyncSerialExecutionStrategy
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.ExecutionIdProvider
import graphql.execution.ExecutionStrategy
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.preparsed.PreparsedDocumentProvider
import graphql.schema.GraphQLSchema
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.method.annotation.RequestHeaderMapMethodArgumentResolver
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver
import org.springframework.web.method.annotation.RequestParamMapMethodArgumentResolver
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import org.springframework.web.servlet.mvc.method.annotation.ServletCookieValueMethodArgumentResolver
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory
import java.util.*

/**
 * Framework auto configuration based on open source Spring only, without Netflix integrations.
 * This does NOT have logging, tracing, metrics and security integration.
 */
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfiguration
open class DgsSpringGraphQLAutoConfiguration {

    @Bean
    open fun dgsGraphQLSource(
        schema: GraphQLSchema,
        schemaProvider: DgsSchemaProvider,
        dataFetcherExceptionHandler: DataFetcherExceptionHandler,
        instrumentations: ObjectProvider<Instrumentation>,
        environment: Environment,
        @Qualifier("query") providedQueryExecutionStrategy: Optional<ExecutionStrategy>,
        @Qualifier("mutation") providedMutationExecutionStrategy: Optional<ExecutionStrategy>,
        idProvider: Optional<ExecutionIdProvider>,
        reloadSchemaIndicator: DefaultDgsQueryExecutor.ReloadSchemaIndicator,
        preparsedDocumentProvider: ObjectProvider<PreparsedDocumentProvider>
    ): DgsGraphQLSource {
        val queryExecutionStrategy =
            providedQueryExecutionStrategy.orElse(AsyncExecutionStrategy(dataFetcherExceptionHandler))
        val mutationExecutionStrategy =
            providedMutationExecutionStrategy.orElse(AsyncSerialExecutionStrategy(dataFetcherExceptionHandler))

        val instrumentationImpls = instrumentations.orderedStream().toList()
        val instrumentation: Instrumentation? = when {
            instrumentationImpls.size == 1 -> instrumentationImpls.single()
            instrumentationImpls.isNotEmpty() -> ChainedInstrumentation(instrumentationImpls)
            else -> null
        }
        return DgsGraphQLSource(schemaProvider, instrumentation, dataFetcherExceptionHandler, queryExecutionStrategy, mutationExecutionStrategy, idProvider, reloadSchemaIndicator, preparsedDocumentProvider.getIfAvailable())
    }

    @Bean
    open fun dgsGraphQlInterceptor(
        dgsDataLoaderProvider: DgsDataLoaderProvider,
        dgsDefaultContextBuilder: DefaultDgsGraphQLContextBuilder
    ): DgsGraphQLInterceptor {
        return DgsGraphQLInterceptor(
            dgsDataLoaderProvider,
            dgsDefaultContextBuilder
        )
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    open class WebMvcArgumentHandlerConfiguration {

        @Qualifier
        private annotation class Dgs

        @Bean
        @Dgs
        open fun dgsWebDataBinderFactory(adapter: ObjectProvider<RequestMappingHandlerAdapter>): WebDataBinderFactory {
            return ServletRequestDataBinderFactory(listOf(), adapter.ifAvailable?.webBindingInitializer)
        }

        @Bean
        open fun requestHeaderMapResolver(@Dgs dataBinderFactory: WebDataBinderFactory): ArgumentResolver {
            return HandlerMethodArgumentResolverAdapter(RequestHeaderMapMethodArgumentResolver(), dataBinderFactory)
        }

        @Bean
        open fun requestHeaderResolver(beanFactory: ConfigurableBeanFactory, @Dgs dataBinderFactory: WebDataBinderFactory): ArgumentResolver {
            return HandlerMethodArgumentResolverAdapter(
                RequestHeaderMethodArgumentResolver(beanFactory),
                dataBinderFactory
            )
        }

        @Bean
        open fun requestParamResolver(@Dgs dataBinderFactory: WebDataBinderFactory): ArgumentResolver {
            return HandlerMethodArgumentResolverAdapter(RequestParamMethodArgumentResolver(false), dataBinderFactory)
        }

        @Bean
        open fun requestParamMapResolver(@Dgs dataBinderFactory: WebDataBinderFactory): ArgumentResolver {
            return HandlerMethodArgumentResolverAdapter(RequestParamMapMethodArgumentResolver(), dataBinderFactory)
        }

        @Bean
        open fun cookieValueResolver(beanFactory: ConfigurableBeanFactory, @Dgs dataBinderFactory: WebDataBinderFactory): ArgumentResolver {
            return HandlerMethodArgumentResolverAdapter(ServletCookieValueMethodArgumentResolver(beanFactory), dataBinderFactory)
        }
    }
}
