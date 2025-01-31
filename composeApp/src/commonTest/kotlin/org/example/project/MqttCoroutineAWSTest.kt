import io.matthewnelson.kmp.file.SysTempDir
import io.matthewnelson.kmp.file.absolutePath
import io.matthewnelson.kmp.file.parentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import socket.tls.TLSClientSettings
import kotlin.test.Test

class MqttCoroutineAWSTest {
    private val home = SysTempDir.parentFile?.parentFile?.parentFile?.absolutePath
    private val root = "StudioProjects/SolarPanelControlApp/composeApp/src/commonMain/composeResources/files/certs"
    private val serverCertificate = "$home/$root/AmazonRootCA1.pem"
    private val privateKey = "$home/$root/private_key.pem.key"
    private val deviceCertificate = "$home/$root/device_cert.pem.crt"
    private val coroutineContext = CoroutineScope(Dispatchers.IO)


    fun listFiles(directoryPath: String): List<String> {
        val directory = directoryPath.toPath()
        return FileSystem.SYSTEM.list(directory).map { it.name }
    }

    @Test
    fun listFilesTest() {
        val files = listFiles("/Users/rental/StudioProjects/SolarPanelControlApp/composeApp/src/commonMain/composeResources/files/certs")
        println("FILES: ")
        println(files)
    }

    @Test
    fun mqttTest() = runBlocking {
//        coroutineContext.launch {
            val client = MQTTClient(
                MQTTVersion.MQTT5,            // Using MQTT v5 protocol
                "a12offtehlmcn0-ats.iot.us-east-1.amazonaws.com",         // Test broker
                8883,
                TLSClientSettings(
                    serverCertificate = serverCertificate,
                    clientCertificate = deviceCertificate,
                    clientCertificateKey = privateKey
                ),
                keepAlive = 30,
                debugLog = true,              // Enable debug logging
            ) {
                println(it.payload?.toByteArray()?.decodeToString())
                println("YEAH")
            }

            try {
                client.runSuspend(Dispatchers.IO)

                // Subscribe to a topic
                println("Subscribing to topic...")
                client.subscribe(listOf(Subscription("/home", SubscriptionOptions(Qos.AT_LEAST_ONCE))))

                // Publish a message to the topic
                println("Publishing message...")
                client.publish(false, Qos.AT_LEAST_ONCE, "/home", "OH YEAH".encodeToByteArray().toUByteArray())

                delay(1000) // Wait for messages
            } catch (e: Exception) {
                println("Error: ${e.message}")
                e.printStackTrace()
            }
    }
}