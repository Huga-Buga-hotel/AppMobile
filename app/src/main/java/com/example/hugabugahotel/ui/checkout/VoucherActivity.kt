package com.example.hugabugahotel.ui.checkout

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.ApiErrors
import com.example.hugabugahotel.data.model.ReservaResponse
import com.example.hugabugahotel.databinding.ActivityVoucherBinding
import com.example.hugabugahotel.ui.banquet.BanquetActivity
import com.example.hugabugahotel.ui.experiences.ExperiencesActivity
import com.example.hugabugahotel.ui.home.HomeActivity
import com.example.hugabugahotel.util.Formatos
import com.example.hugabugahotel.util.QrCodeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Voucher ("tábua") da reserva. Tudo vem de GET /reservas/{codigo}; nada é montado localmente. */
class VoucherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoucherBinding
    private var reservaCodigo: String = ""
    private var reserva: ReservaResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoucherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: run { finish(); return }
        binding.tvVoucherCode.text = reservaCodigo

        binding.topBar.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
        binding.btnShare.setOnClickListener {
            val r = reserva
            val texto = if (r == null) "Reserva $reservaCodigo no Huga Buga Hotel"
            else "Reserva ${r.codigo} no Huga Buga Hotel: ${r.cavernaNome}, de ${Formatos.dataCompleta(r.checkIn)} a ${Formatos.dataCompleta(r.checkOut)}."
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, texto); type = "text/plain"
            }, "Compartilhar voucher"))
        }
        binding.btnDownloadVoucher.setOnClickListener { baixarPdf() }
        binding.btnActionCheckin.setOnClickListener { checkinOnline() }
        binding.btnActionAdventures.setOnClickListener {
            startActivity(Intent(this, ExperiencesActivity::class.java).putExtra("RESERVA_CODIGO", reservaCodigo))
        }
        binding.btnActionBanquet.setOnClickListener {
            startActivity(Intent(this, BanquetActivity::class.java).putExtra("RESERVA_CODIGO", reservaCodigo))
        }
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    private fun carregar() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getApi(this@VoucherActivity).getVoucherReserva(reservaCodigo)
                val r = response.body()
                if (response.isSuccessful && r != null) bind(r) else erro(ApiErrors.mensagem(response))
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            }
        }
    }

    private fun bind(r: ReservaResponse) {
        reserva = r
        val pagamento = if (r.pago) "Pagamento confirmado." else "Pagamento na chegada."
        binding.tvVoucherDesc.text = "${r.cavernaNome} é da sua tribo de ${Formatos.dataCompleta(r.checkIn)} a " +
            "${Formatos.dataCompleta(r.checkOut)}. Status: ${Formatos.status(r.status)}. $pagamento"
        binding.tvVoucherCavernName.text = listOfNotNull(r.cavernaNome, r.unidadeCodigo).joinToString(" · ")
        binding.tvVoucherCode.text = r.codigo
        binding.tvVoucherCheckInDate.text = Formatos.dataCurta(r.checkIn).uppercase()
        binding.tvVoucherCheckOutDate.text = Formatos.dataCurta(r.checkOut).uppercase()
        binding.tvVoucherTribeCount.text = "${r.adultos + r.criancas} na tribo (${r.adultos} ad., ${r.criancas} cri.)" +
            (r.combo?.let { "\nCombo ${it.nome}: jantar às ${it.horario.take(5)}" } ?: "")
        binding.tvVoucherTotal.text = "Total: ${Formatos.reais(r.total)}"
        binding.btnActionCheckin.alpha = if (r.checkinOnlineEm != null) 0.5f else 1f // check-in online já feito

        // O QR leva só o código da reserva: a recepção consulta o resto na API
        QrCodeUtils.generateQrCodeBitmap(r.codigo, 512, 512)?.let {
            binding.root.findViewById<ImageView>(R.id.ivVoucherQrCode)?.setImageBitmap(it)
        }
    }

    /** Abre 3 dias antes da chegada (senão a API responde 422 FORA_DO_PRAZO com a data certa). */
    private fun checkinOnline() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getApi(this@VoucherActivity).realizarCheckin(reservaCodigo)
                val r = response.body()
                if (response.isSuccessful && r != null) {
                    bind(r)
                    Toast.makeText(this@VoucherActivity, "Check-in online feito! Uga! 🪨", Toast.LENGTH_SHORT).show()
                } else {
                    erro(ApiErrors.mensagem(response))
                }
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            }
        }
    }

    private fun baixarPdf() {
        Toast.makeText(this, "Baixando a tábua (PDF)... 🪨", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val response = ApiClient.getApi(this@VoucherActivity).downloadVoucherPdf(reservaCodigo)
                val body = response.body()
                if (!response.isSuccessful || body == null) return@launch erro(ApiErrors.mensagem(response))
                val arquivo = File(cacheDir, "tabua-$reservaCodigo.pdf")
                withContext(Dispatchers.IO) {
                    body.byteStream().use { input -> arquivo.outputStream().use { input.copyTo(it) } }
                }
                abrirPdf(arquivo)
            } catch (e: Exception) {
                erro(ApiErrors.mensagem(e))
            }
        }
    }

    private fun abrirPdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Abrir voucher"))
        } catch (_: Exception) {
            Toast.makeText(this, "Nenhum leitor de PDF encontrado no aparelho.", Toast.LENGTH_LONG).show()
        }
    }

    private fun erro(msg: String) {
        AlertDialog.Builder(this).setTitle("Ugh!").setMessage(msg).setPositiveButton("OK", null).show()
    }
}
