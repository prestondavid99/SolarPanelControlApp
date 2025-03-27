package org.example.project.model

import kotlinx.serialization.Serializable

/**
 * A simple data class that contains the necessary data to send to AWS IoT Core.
 *
 * @Serializable means that the class can be packaged into a JSON object (necessary for IoT Core).
 * **/
@Serializable
data class Payload(
    var raise_array: Int = 0
)


