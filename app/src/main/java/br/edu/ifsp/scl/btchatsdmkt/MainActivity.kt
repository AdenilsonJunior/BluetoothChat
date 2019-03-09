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
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v4.content.PermissionChecker
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

class MainActivity : AppCompatActivity() {

    private var threadServer: ThreadServidor? = null
    private var threadClient: ThreadCliente? = null
    private var threadCommunication: ThreadComunicacao? = null

    var btFoundList = mutableListOf<BluetoothDevice>()

    var eventBroadcastReceiver: EventosBluetoothReceiver? = null

    var historyAdapter: ArrayAdapter<String>? = null

    var mHandler: TelaPrincipalHandler? = null

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                iniciarThreadServidor()
            }
        } else {
            toast("Não existe adaptador Bluetooth neste device")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ATIVA_BLUETOOTH -> {
                if (resultCode != Activity.RESULT_OK) {
                    toast("Bluetooth necessário")
                    finish()
                }
            }
            ATIVA_DESCOBERTA_BLUETOOTH -> {
                if(resultCode == Activity.RESULT_CANCELED){
                    toast("Visibilidade necessária")
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
                        toast("É necessário dar permissão antes")
                        finish()
                    }
                }
                pegandoAdaptadorBluetooth()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_modo_aplicativo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.modoClienteMenuItem -> {
                toast("Modo cliente")
                adaptadorBt = BluetoothAdapter.getDefaultAdapter()
                btFoundList = mutableListOf()

                if(adaptadorBt?.isEnabled == true){
                    registraReceiver()
                    adaptadorBt?.startDiscovery()
                    exibirAguardeDialog("Procurando dispositivos bluetooth", TEMPO_DESCOBERTA_SERVICO_BLUETOOTH)
                }else{
                    toast("Ative seu bluetooth")
                }

            }
            R.id.modoServidorMenuItem -> {
                toast("Modo servidor")

                val descobertaIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                descobertaIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, TEMPO_DESCOBERTA_SERVICO_BLUETOOTH)
                startActivityForResult(descobertaIntent, ATIVA_DESCOBERTA_BLUETOOTH)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun exibirDispositivosEncontrados() {
        progressDialog?.dismiss()
        val listaNomesBtsEncontrados = mutableListOf<String>()
        btFoundList.forEach {
            listaNomesBtsEncontrados.add(it.name ?: it.address)
        }

        val escolhaDispositivoDialog = AlertDialog.Builder(this)
            .setTitle("Dispositivos Encontrados")
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
                    toast("Desconectado")
                }
            }
        }
    }

    private fun registraReceiver(){
        eventBroadcastReceiver = eventBroadcastReceiver ?: EventosBluetoothReceiver(this)
        registerReceiver(eventBroadcastReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(eventBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
    }

    fun desregistraReceiver() = eventBroadcastReceiver?.let { unregisterReceiver(it) }

    private fun iniciarThreadServidor(){
        paraThreadFilhas()

        exibirAguardeDialog("Aguardando conexões", TEMPO_DESCOBERTA_SERVICO_BLUETOOTH)

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
                    outputStream?.writeUTF(mensagem)

                    historyAdapter?.add("Eu: $mensagem")
                    historyAdapter?.notifyDataSetChanged()
                }
            }catch (ioException: IOException){
                mHandler?.obtainMessage(MENSAGEM_DESCONEXAO, ioException.message + "[0]")?.sendToTarget()
                ioException.printStackTrace()
            }
        }
    }
}
