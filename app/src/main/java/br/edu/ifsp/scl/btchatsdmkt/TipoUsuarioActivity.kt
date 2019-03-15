package br.edu.ifsp.scl.btchatsdmkt

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_tipo_usuario.*

class TipoUsuarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tipo_usuario)

        btnServidor.setOnClickListener {
            startMainActivity(Tipo.SERVIDOR)
        }

        btnCliente.setOnClickListener {
            startMainActivity(Tipo.CLIENTE)
        }
    }

    private fun startMainActivity(tipo: Tipo) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_TIPO, tipo)
        }
        startActivity(intent)
        finish()
    }

    enum class Tipo{
        SERVIDOR, CLIENTE
    }
}
