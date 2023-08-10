package com.example.danp_proyecto.MQTT

import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.StandardCharsets

class Client constructor ( private val mqttClientPro: ClientProviderInt )
{
    private var client = mqttClientPro.provideMqttClient()

    fun connect() {
        try {
            client.connect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun isConnected(): Boolean{
        return client.isConnected
    }

    fun disconnect() {
        try {
            client.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            client.publish(topic, mqttMessage)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String, qos: Int, onDataReceived: (String) -> Unit) {
        try {
            client.subscribe(topic, qos) { _, message ->
                val data = message.payload.toString(StandardCharsets.UTF_8)
                onDataReceived(data)
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        try {
            client.unsubscribe(topic)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}