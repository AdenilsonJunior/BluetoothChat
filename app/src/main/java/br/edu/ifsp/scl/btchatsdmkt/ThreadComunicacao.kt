package br.edu.ifsp.scl.btchatsdmkt

import android.bluetooth.BluetoothSocket
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.MENSAGEM_DESCONEXAO
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.MENSAGEM_TEXTO
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.inputStream
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.outputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class ThreadComunicacao(val mainActivity: MainActivity) : Thread() {
    private var socket: BluetoothSocket? = null

    override fun run() {
        try {
            // Recupera o nome do dispositivo remoto
            var nome = socket!!.remoteDevice.name
            // Recupera uma referência para os InputStream e OutputStream a partir do Socket
            inputStream = DataInputStream(socket!!.inputStream)
            outputStream = DataOutputStream(socket!!.outputStream)
            // Lendo mensagens e escrevendo na Tela Principal
            var mensagem: String?
            while (true) {
                // Lê o InputStream e armazena numa String
                mensagem = inputStream?.readUTF()
                // Aciona o Handler da Tela Principal para mostrar a String recebida no ListView
                mainActivity.mHandler?.obtainMessage(MENSAGEM_TEXTO, nome + ": " + mensagem)?.sendToTarget()
            }
        } catch (e: IOException) {
            /* Em caso de desconexão pede para o Handler da tela principal mostrar um Toast para o
            * usuário */
            mainActivity.mHandler?.obtainMessage(MENSAGEM_DESCONEXAO, e.message + "[3]")?.sendToTarget()
            e.printStackTrace()
        }

    }

    fun iniciar(socket: BluetoothSocket?) {
        this.socket = socket
        start()
    }

    fun parar() {
        try {
            // Fecha os Streams
            inputStream?.close()
            outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}