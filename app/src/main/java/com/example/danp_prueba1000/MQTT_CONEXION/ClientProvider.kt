package com.example.danp_proyecto.MQTT

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.UUID
import javax.inject.Inject
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class ClientProvider constructor(private val context: Context) : ClientProviderInt {

    companion object {
        private val endpoint = "ssl://a3v14ql51wn11e-ats.iot.us-east-2.amazonaws.com"
        //NUMERO RANDOM
        private val clientId = "iotconsole-4213bde3-ae02-47c1-bc9c-d0c3c07c3dec"//UUID.randomUUID()
        val topic = "my/topic"
        val message = "Hola desde Android!"
    }

    //INSTANCIA DE LA LIBRERIA PAHO
    private val mqttClient: MqttClient

    init {
        //SUBIR CERTIFICADO
        val certificateInputStream: InputStream = context.assets.open("cert.der")
        val certificate = readCertificate(certificateInputStream)

        val keyInputStream: InputStream = context.assets.open("private.der")
        val privateKey =readPrivateKey(keyInputStream);

        //SSL CONEXIONES MQTT
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null)

        //CONFIGURACION CERTIFICADOS PARA SSL
        keyStore.setCertificateEntry("alias", certificate)
        keyStore.setKeyEntry("alias", privateKey, null, arrayOf(certificate))

        val sslContext = SSLContext.getInstance("TLSv1.2")
        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, null)
        sslContext.init(keyManagerFactory.keyManagers, null, null)

        //INSTANCIA PARA CONECTAR
        val options = MqttConnectOptions()
        options.socketFactory = sslContext.socketFactory
        mqttClient = MqttClient(endpoint, clientId.toString(), MemoryPersistence())
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                CoroutineScope(Dispatchers.IO).launch {
                    reconnect(options)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            override fun messageArrived(topic: String?, message: MqttMessage?) {}
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {}
        })
        CoroutineScope(Dispatchers.Main).launch {
            reconnect(options)
        }
    }

    //RECONEXION SI ES NECESARIA
    private suspend fun reconnect(options: MqttConnectOptions) {
        try {
            println("Intentando reconexión...")
            mqttClient.connect(options)
            if (mqttClient.isConnected) {
                println("Reconexión exitosa")
            }
        } catch (e: MqttException) {
            println("Error en la reconexión: ${e.message}")
            //CADA 5 SEGUNDOS
            delay(5000)
            reconnect(options)
        }
    }

    private fun readPrivateKey(privateKeyInputStream: InputStream): PrivateKey {
        val privateKeyBytes = privateKeyInputStream.readBytes()
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        return keyFactory.generatePrivate(privateKeySpec)
    }

    private fun readCertificate(certificateInputStream: InputStream): X509Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        return certificateFactory.generateCertificate(certificateInputStream) as X509Certificate
    }

    override fun provideMqttClient(): MqttClient {
        return mqttClient
    }

}