package br.edu.ifsp.scl.btchatsdmkt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EventosBluetoothReceiver(val mainActivity: MainActivity) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if(BluetoothDevice.ACTION_FOUND == intent?.action){

            val dispositivoEncontrado: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            mainActivity.btFoundList.add(dispositivoEncontrado)
        }else {
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED == intent?.action){
                mainActivity.exibirDispositivosEncontrados()

                mainActivity.desregistraReceiver()
            }
        }
    }
}