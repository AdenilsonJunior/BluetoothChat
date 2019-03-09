package br.edu.ifsp.scl.btchatsdmkt

import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.MENSAGEM_DESCONEXAO
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.SERVICO_BLUETOOTH
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.UUID_SERVICO_BLUETOOTH
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.adaptadorBt
import java.io.IOException

class ThreadServidor(val mainActivity: MainActivity) : Thread() {
    // ServerSocket que vai aguardar conexões
    private var serverSocket: BluetoothServerSocket? = null

    // Socket que será estabelecido com o cliente quando uma conexão for estabelecida
    private var clientSocket: BluetoothSocket? = null

    override fun run() {
        try {
            // Aguarda conexões usando RFCOMM com base no Nome e no UUID do serviço
            serverSocket = adaptadorBt?.listenUsingRfcommWithServiceRecord(
                SERVICO_BLUETOOTH,
                UUID_SERVICO_BLUETOOTH
            )
            // Quando uma conexão chega, aceita e estabelece uma Socket com o cliente
            clientSocket = serverSocket!!.accept()
            // Fecha o Dialog e instancia um Thread de Comunicação a partir do Socket estabelecido
            mainActivity.trataSocket(clientSocket)
        } catch (e: IOException) {
            /* Em caso de desconexão pede para o Handler da tela principal mostrar um Toast para o
               * usuário */
            mainActivity.mHandler?.obtainMessage(MENSAGEM_DESCONEXAO, e.message + "[1]")?.sendToTarget()
            e.printStackTrace()
        }

    }

    // Método para iniciar, mantendo um padrão
    fun iniciar() = start()

    // Método para parar a Thread Filha
    fun parar() {
        try {
            serverSocket!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}