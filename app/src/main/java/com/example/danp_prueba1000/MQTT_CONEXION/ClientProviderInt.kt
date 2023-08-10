package com.example.danp_proyecto.MQTT

import org.eclipse.paho.client.mqttv3.MqttClient

interface ClientProviderInt {
    fun provideMqttClient(): MqttClient
}