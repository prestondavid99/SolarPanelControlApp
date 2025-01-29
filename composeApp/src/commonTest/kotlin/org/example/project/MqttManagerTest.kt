package org.example.project

import MQTTClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MqttManagerTest {
    private lateinit var mqttManager: MqttManager
    private lateinit var mockClient: MQTTClient

    @BeforeTest
    fun setup() {
        mqttManager = MqttManager()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testConnect() {
        mqttManager.subscribe("/randomTopic")
    }

    @Test
    fun testPublish() {
        mqttManager.publish("/randomTopic", "Hello from KMP!")
    }

    @Test
    fun testStart() {
        mqttManager.start()
    }

}