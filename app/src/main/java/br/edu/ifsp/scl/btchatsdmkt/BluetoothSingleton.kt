package br.edu.ifsp.scl.btchatsdmkt

import android.bluetooth.BluetoothAdapter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*

object BluetoothSingleton {

    object Constantes {
        // Constantes para identificar o serviço Bluetooth
        const val SERVICO_BLUETOOTH: String = "BLUETOOTH_CHAT_SDM"
        val UUID_SERVICO_BLUETOOTH: UUID = UUID.fromString("0a898042-40b7-11e7-a919-92ebcb67fe33")

        // Tempo em que o dispositivo ficará visível para descoberta
        const val TEMPO_DESCOBERTA_SERVICO_BLUETOOTH: Int = 30

        // RequestCode passado para chamar o SO por startActivityForResult
        const val ATIVA_BLUETOOTH: Int = 0
        const val ATIVA_DESCOBERTA_BLUETOOTH: Int = 1

        // Tipos de mensagens que serão manipuladas pelo Handler da Tela Principal
        const val MENSAGEM_TEXTO: Int = 0
        const val MENSAGEM_DESCONEXAO: Int = 2

        // RequestCode para chamar o SO para requestPermissions
        const val REQUER_PERMISSOES_LOCALIZACAO = 10
    }

    // Adaptador Bluetooth do dispositivo usado pelas Threads filhas
    var adaptadorBt: BluetoothAdapter? = null

    // Stream de entrada e saída
    var inputStream: DataInputStream? = null
    var outputStream: DataOutputStream? = null
}