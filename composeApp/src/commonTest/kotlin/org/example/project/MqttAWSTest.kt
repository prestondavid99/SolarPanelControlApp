import mqtt.MQTTVersion
import mqtt.Subscription
import mqtt.packets.Qos
import mqtt.packets.mqttv5.SubscriptionOptions
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import socket.tls.TLSClientSettings
import kotlin.test.BeforeTest
import kotlin.test.Test

class MqttAWSTest {
    private val homeDirectory = "/Users/rental/StudioProjects/SolarPanelControlApp/composeApp/src/commonMain/composeResources/files/certs"
    private val serverCertificate = "$homeDirectory/AmazonRootCA1.pem"
    private val privateKey = "$homeDirectory/private_key.pem.key"
    private val deviceCertificate = "$homeDirectory/device_cert.pem.crt"


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
    fun mqttTest() {
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
            client.subscribe(listOf(Subscription("/home", SubscriptionOptions(Qos.AT_LEAST_ONCE))))
            client.publish(false, Qos.AT_LEAST_ONCE, "/home", "yes".encodeToByteArray().toUByteArray())
            client.run() // Blocking method, use step() if you don't want to block the thread
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}