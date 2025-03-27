package org.example.project

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.example.project.model.Payload
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MqttManagerTest {

    @Test
    fun testInit() = runBlocking {
        val testTopic = "test/topic"

        // Initialize MqttManager with a test topic
        try {
            MqttManager.init(testTopic, TestResourceLoader())
        } catch (e: Exception) {
            error("Initialization failed: ${e.message}")
        }

        // Wait until the connection state is true (with a 10-second timeout)
        withTimeout(10_000L) {
            while (!MqttManager.connectionState.value) {
                delay(100)
            }
        }

        // Assert that the MqttManager successfully connected (connectionState == true)
        assertTrue { MqttManager.connectionState.value }
    }

    @Test
    fun testPublish() = runBlocking {
        val testTopic = "test/topic"
        val expectedPayload = Payload(raise_array = 1)

        // Initialize MqttManager with a test topic
        try {
            MqttManager.init(testTopic, TestResourceLoader())
        } catch (e: Exception) {
            error("Initialization failed: ${e.message}")
        }

        // Wait until the connection state is true (with a 10-second timeout)
        withTimeout(10_000L) {
            while (!MqttManager.connectionState.value) {
                delay(100)
            }
        }

        // Publish the test payload
        MqttManager.publish(testTopic, expectedPayload)

        // Wait until a payload is received (with a 10-second timeout)
        val received = withTimeout(10_000L) {
            MqttManager.receivedPayload.first { it != null }
        }

        // Assert that the received payload matches the expected payload value
        assertEquals(expectedPayload.raise_array, received?.raise_array)
    }
}