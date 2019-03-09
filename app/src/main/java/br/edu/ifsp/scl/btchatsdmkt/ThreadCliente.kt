package br.edu.ifsp.scl.btchatsdmkt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.MENSAGEM_DESCONEXAO
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.UUID_SERVICO_BLUETOOTH
import java.io.IOException

// Thread Cliente
class ThreadCliente(val mainActivity: MainActivity) : Thread() {
    // Dispositivo remotor em modo Servidor selecionado da lista de descoberta
    private var dispositivo: BluetoothDevice? = null

    // Socket com o dispositivo em modo Servidor
    private var socket: BluetoothSocket? = null

    override fun run() {
        try {
            // Estabelece um Socket RFCOMM com o dispositivo escolhido usando o UUID do Serviço
            socket = dispositivo?.createRfcommSocketToServiceRecord(UUID_SERVICO_BLUETOOTH)
            // Estabelece a conexão. Isso vai encaixar lá no serverSocket.accept() do Servidor
            socket!!.connect()
            // Fecha o Dialog e instancia um Thread de Comunicação a partir do Socket estabelecido
            mainActivity.trataSocket(socket)
        } catch (e: IOException) {
            /* Em caso de desconexão pede para o Handler da tela principal mostrar um Toast para o
            usuário */
            mainActivity.mHandler?.obtainMessage(MENSAGEM_DESCONEXAO, e.message + "[2]")?.sendToTarget()
            e.printStackTrace()
        }
    }

    fun iniciar(dispositivo: BluetoothDevice?) {
        this.dispositivo = dispositivo
        start()
    }

    fun parar() {
        try {
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}