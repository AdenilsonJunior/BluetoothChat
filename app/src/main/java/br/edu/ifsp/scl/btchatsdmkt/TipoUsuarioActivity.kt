package br.edu.ifsp.scl.btchatsdmkt

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_tipo_usuario.*

class TipoUsuarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tipo_usuario)

        btnServidor.setOnClickListener {
            validateAndStartMainActivity(Tipo.SERVIDOR)
        }

        btnCliente.setOnClickListener {
            validateAndStartMainActivity(Tipo.CLIENTE)
        }
    }

    private fun validateAndStartMainActivity(tipo: Tipo) {
        val nome = nomeEdtiText.text.toString()
        if(!TextUtils.isEmpty(nome)){
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_TIPO, tipo)
                putExtra(MainActivity.EXTRA_NOME, nome)
            }
            startActivity(intent)
            finish()
        }else{
            nomeEdtiText.error = getString(R.string.texto_erro_nome_obrigatorio)
        }

    }

    enum class Tipo{
        SERVIDOR, CLIENTE
    }
}
