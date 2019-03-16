package br.edu.ifsp.scl.btchatsdmkt

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.ATIVA_BLUETOOTH
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.ATIVA_DESCOBERTA_BLUETOOTH
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.MENSAGEM_DESCONEXAO
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.MENSAGEM_TEXTO
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.REQUER_PERMISSOES_LOCALIZACAO
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.Constantes.TEMPO_DESCOBERTA_SERVICO_BLUETOOTH
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.adaptadorBt
import br.edu.ifsp.scl.btchatsdmkt.BluetoothSingleton.outputStream
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.lang.IllegalArgumentException

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TIPO = "EXTRA_TIPO"
        const val EXTRA_NOME = "EXTRA_NOME"
    }

    private var threadServer: ThreadServidor? = null
    private var threadClient: ThreadCliente? = null
    private var threadCommunication: ThreadComunicacao? = null

    var btFoundList = mutableListOf<BluetoothDevice>()

    var eventBroadcastReceiver: EventosBluetoothReceiver? = null

    var historyAdapter: ArrayAdapter<String>? = null

    var mHandler: TelaPrincipalHandler? = null

    private var progressDialog: ProgressDialog? = null

    private lateinit var tipo: TipoUsuarioActivity.Tipo
    private var nomeUsuario: String = "Usuário"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Pega o modo que o usuário escolheu
        tipo = intent.getSerializableExtra(EXTRA_TIPO) as TipoUsuarioActivity.Tipo
        nomeUsuario = intent.getStringExtra(EXTRA_NOME)

        when(tipo){
            TipoUsuarioActivity.Tipo.SERVIDOR -> setTitle(R.string.texto_modo_servidor)
            TipoUsuarioActivity.Tipo.CLIENTE -> setTitle(R.string.texto_modo_cliente)
        }

        historyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        historicoListView.adapter = historyAdapter

        mHandler = TelaPrincipalHandler()

        preparandoAdaptadorBluetooth()

    }

    private fun preparandoAdaptadorBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PermissionChecker.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PermissionChecker.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                    ), REQUER_PERMISSOES_LOCALIZACAO
                )
            }else{
                pegandoAdaptadorBluetooth()
            }
        } else {
            pegandoAdaptadorBluetooth()
        }
    }

    private fun pegandoAdaptadorBluetooth() {
        adaptadorBt = BluetoothAdapter.getDefaultAdapter()
        if (adaptadorBt != null) {
            if (adaptadorBt!!.isEnabled.not()) {
                val ativaBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(ativaBluetooth, ATIVA_BLUETOOTH)
            }else{
                when(tipo){
                    TipoUsuarioActivity.Tipo.SERVIDOR -> ativarDescobertaDoServidor()
                    TipoUsuarioActivity.Tipo.CLIENTE -> procurarDevicesParaParear()
                }
            }
        } else {
            toast(getString(R.string.texto_sem_adaptador_bluetooth))
        }
    }

    private fun procurarDevicesParaParear() {
        adaptadorBt = BluetoothAdapter.getDefaultAdapter()
        btFoundList = mutableListOf()

        if(adaptadorBt?.isEnabled == true){
            registraReceiver()
            adaptadorBt?.startDiscovery()
            exibirAguardeDialog(getString(R.string.texto_procurando_dispositivos), TEMPO_DESCOBERTA_SERVICO_BLUETOOTH)
        }else{
            toast(getString(R.string.texto_ative_bluetooth))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ATIVA_BLUETOOTH -> {
                if (resultCode != Activity.RESULT_OK) {
                    toast(getString(R.string.texto_bluetooth_necessario))
                    finish()
                }
            }
            ATIVA_DESCOBERTA_BLUETOOTH -> {
                if(resultCode == Activity.RESULT_CANCELED){
                    toast(getString(R.string.texto_visibilidade_necessaria))
                    finish()
                }else{
                    iniciarThreadServidor()
                }
            }
        }
    }

    private fun exibirAguardeDialog(message: String, tempo: Int){
        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage(message)
        progressDialog?.isIndeterminate = true
        progressDialog?.setOnCancelListener {
            onCancelDialog(it)
        }
        progressDialog?.show()

        if(tempo > 0){
            mHandler?.postDelayed({
                if(threadCommunication == null){
                    progressDialog?.dismiss()
                }
            }, tempo * 1000L)
        }
    }

    private fun onCancelDialog(dialog: DialogInterface) {
        adaptadorBt?.cancelDiscovery()
        paraThreadFilhas()
        dialog.dismiss()
    }

    private fun paraThreadFilhas() {
        threadClient?.parar()
        threadClient = null
        threadServer?.parar()
        threadServer = null
        threadCommunication?.parar()
        threadCommunication = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUER_PERMISSOES_LOCALIZACAO -> {
                permissions.forEachIndexed { index, _ ->
                    if (grantResults[index] != PermissionChecker.PERMISSION_GRANTED) {
                        toast(getString(R.string.texto_permissao_localizacao_obrigatoria))
                        finish()
                    }
                }
                pegandoAdaptadorBluetooth()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_atualizar, menu)
        return true
    }

    private fun ativarDescobertaDoServidor() {
        val descobertaIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        descobertaIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, TEMPO_DESCOBERTA_SERVICO_BLUETOOTH)
        startActivityForResult(descobertaIntent, ATIVA_DESCOBERTA_BLUETOOTH)
    }

    fun exibirDispositivosEncontrados() {
        progressDialog?.dismiss()
        val listaNomesBtsEncontrados = mutableListOf<String>()
        btFoundList.forEach {
            listaNomesBtsEncontrados.add(it.name ?: it.address)
        }

        val escolhaDispositivoDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.texto_dispositivos_encontrados))
            .setSingleChoiceItems(listaNomesBtsEncontrados.toTypedArray(), -1){ dialog, which ->
                trataSelecaoServidor(dialog, which)
            }.create()


        escolhaDispositivoDialog.show()
    }

    private fun trataSelecaoServidor(dialog: DialogInterface, which: Int) {
        iniciaThreadClient(which)
        adaptadorBt?.cancelDiscovery()
        dialog.dismiss()
    }

    private fun toast(mensagem: String) = Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()

    inner class TelaPrincipalHandler : Handler() {

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg?.what == MENSAGEM_TEXTO) {
                historyAdapter?.add(msg.obj.toString())
                historyAdapter?.notifyDataSetChanged()
            } else {
                if (msg?.what == MENSAGEM_DESCONEXAO) {
                    toast(getString(R.string.texto_desconectado))
                }
            }
        }
    }

    private fun registraReceiver(){
        eventBroadcastReceiver = eventBroadcastReceiver ?: EventosBluetoothReceiver(this)
        registerReceiver(eventBroadcastReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(eventBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
    }

    fun desregistraReceiver() {
        try{
            eventBroadcastReceiver?.let { unregisterReceiver(it) }
        }catch (e: IllegalArgumentException){
            Log.e("Receiver", "Receiver nao registrado: ${e.message}")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(tipo){
            TipoUsuarioActivity.Tipo.CLIENTE -> procurarDevicesParaParear()
            TipoUsuarioActivity.Tipo.SERVIDOR -> ativarDescobertaDoServidor()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun iniciarThreadServidor(){
        paraThreadFilhas()

        exibirAguardeDialog(getString(R.string.texto_aguardando_conexoes), TEMPO_DESCOBERTA_SERVICO_BLUETOOTH)

        threadServer = ThreadServidor(this)
        threadServer?.iniciar()
    }

    private fun iniciaThreadClient(position: Int){
        paraThreadFilhas()

        threadClient = ThreadCliente(this)
        threadClient?.iniciar(btFoundList[position])

    }

    override fun onDestroy() {
        desregistraReceiver()
        paraThreadFilhas()
        super.onDestroy()
    }

    fun trataSocket(socket: BluetoothSocket?) {
        progressDialog?.dismiss()
        threadCommunication = ThreadComunicacao(this)
        threadCommunication?.iniciar(socket)

    }

    fun enviarMensagem(view: View){
        val mensagem = mensagemEditText.text.toString()
        if(mensagem.isNotEmpty()){
            mensagemEditText.text.clear()

            try {
                if (outputStream != null) {
                    val nomeEMensagem = "$nomeUsuario : $mensagem"
                    outputStream?.writeUTF(nomeEMensagem)

                    historyAdapter?.add(nomeEMensagem)
                    historyAdapter?.notifyDataSetChanged()
                }
            }catch (ioException: IOException){
                mHandler?.obtainMessage(MENSAGEM_DESCONEXAO, ioException.message + "[0]")?.sendToTarget()
                ioException.printStackTrace()
            }
        }
    }
}
