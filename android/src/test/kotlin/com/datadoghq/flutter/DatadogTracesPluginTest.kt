/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package com.datadoghq.flutter

import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeExtension
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.opentracing.Tracer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.reflect.typeOf

@Extensions(
    ExtendWith(ForgeExtension::class),
    ExtendWith(MockitoExtension::class))
@OptIn(kotlin.ExperimentalStdlibApi::class)
class DatadogTracesPluginTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var mockTracer: Tracer
    private lateinit var plugin: DatadogTracesPlugin

    @BeforeEach
    fun beforeEach() {
        plugin = DatadogTracesPlugin(mockTracer)
    }

    private val contracts = listOf(
        Contract("startRootSpan", mapOf(
            "operationName" to typeOf<String>()
        )),
        Contract("startSpan", mapOf(
            "operationName" to typeOf<String>()
        )),
    )

    @Test
    fun `M report contract violation W missing parameters in contract`(
        forge: Forge
    ) {
        testContracts(contracts, forge, plugin)
    }

    @Test
    fun `M report a contract violation W startRootSpan has bad operation name`(
        @IntForgery operationName: Int
    ) {
        // GIVEN
        val call = MethodCall("startRootSpan", mapOf<String, Any>(
            "operationName" to operationName
        ))
        val mockResult = mock<MethodChannel.Result>()

        // WHEN
        plugin.onMethodCall(call, mockResult)

        // THEN
        verify(mockResult).error(eq(DatadogSdkPlugin.CONTRACT_VIOLATION), any(), anyOrNull())
    }

    @Test
    fun `M report a contract violation W startRootSpan has bad start time`(
        @StringForgery operationName: String,
        @StringForgery startTime: String
    ) {
        // GIVEN
        val call = MethodCall("startRootSpan", mapOf<String, Any>(
            "operationName" to operationName,
            "startTime" to startTime
        ))
        val mockResult = mock<MethodChannel.Result>()

        // WHEN
        plugin.onMethodCall(call, mockResult)

        // THEN
        verify(mockResult).error(eq(DatadogSdkPlugin.CONTRACT_VIOLATION), any(), anyOrNull())
    }

    @Test
    fun `M report a contract violation W startSpan has bad operation name`(
        @IntForgery operationName: Int
    ) {
        // GIVEN
        val call = MethodCall("startSpan", mapOf<String, Any>(
            "operationName" to operationName
        ))
        val mockResult = mock<MethodChannel.Result>()

        // WHEN
        plugin.onMethodCall(call, mockResult)

        // THEN
        verify(mockResult).error(eq(DatadogSdkPlugin.CONTRACT_VIOLATION), any(), anyOrNull())
    }

    private fun createSpan(operationName: String): Long {
        val call = MethodCall("startRootSpan", mapOf<String, Any>(
            "operationName" to operationName
        ))
        val mockResult = mock<MethodChannel.Result>()
        val captor = argumentCaptor<Long>()
        plugin.onMethodCall(call, mockResult)

        verify(mockResult).success(captor.capture())
        return captor.firstValue
    }

    private val spanContracts = listOf(
        Contract("span.setError", mapOf(
            "kind" to typeOf<String>(), "message" to typeOf<String>()
        )),
        Contract("span.setTag", mapOf(
            "key" to typeOf<String>(), "value" to typeOf<String>()
        )),
        Contract("span.setBaggageItem", mapOf(
            "key" to typeOf<String>(), "value" to typeOf<String>()
        )),
        Contract("span.log", mapOf(
            "fields" to typeOf<Map<String, Any?>>()
        ))
    )

    @Test
    fun `M report a contract violation W missing parameters in span contract`(
        forge: Forge,
        @StringForgery operationName: String,
    ) {
        val spanId = createSpan(operationName)
        testContracts(spanContracts, forge, plugin, mapOf(
            "spanHandle" to spanId
        ))
    }
}