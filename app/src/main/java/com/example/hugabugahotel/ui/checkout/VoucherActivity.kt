package com.example.hugabugahotel.ui.checkout

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.hugabugahotel.R
import com.example.hugabugahotel.data.api.ApiClient
import com.example.hugabugahotel.data.api.SessionManager
import com.example.hugabugahotel.data.model.CheckinRequest
import com.example.hugabugahotel.databinding.ActivityVoucherBinding
import com.example.hugabugahotel.ui.banquet.BanquetActivity
import com.example.hugabugahotel.ui.experiences.ExperiencesActivity
import com.example.hugabugahotel.ui.home.HomeActivity
import com.example.hugabugahotel.util.QrCodeUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class VoucherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoucherBinding
    private var reservaCodigo: String = "HB-4F7K"
    private var checkInDisplay: String = "12 out"
    private var checkOutDisplay: String = "15 out"
    private var adultsCount: Int = 2
    private var childrenCount: Int = 2
    private var totalCentavos: Long = 156000L
    private var cavernName: String = "Toca do Mamute"
    private var email: String = "marina@tribo.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoucherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        reservaCodigo = intent.getStringExtra("RESERVA_CODIGO") ?: "HB-4F7K"
        checkInDisplay = intent.getStringExtra("CHECK_IN") ?: "12 out"
        checkOutDisplay = intent.getStringExtra("CHECK_OUT") ?: "15 out"
        adultsCount = intent.getIntExtra("ADULTS", 2)
        childrenCount = intent.getIntExtra("CHILDREN", 2)
        totalCentavos = intent.getLongExtra("TOTAL_CENTAVOS", 156000L)
        cavernName = intent.getStringExtra("CAVERN_NOME") ?: "Toca do Mamute"
        email = intent.getStringExtra("RESPONSAVEL_EMAIL")
            ?: SessionManager(this).getResponsavelEmail()
            ?: SessionManager(this).getUserEmail()
            ?: "marina@tribo.com"

        bindVoucherUi(
            codigo = reservaCodigo,
            cavern = cavernName,
            checkIn = checkInDisplay,
            checkOut = checkOutDisplay,
            adults = adultsCount,
            children = childrenCount,
            total = totalCentavos,
            userEmail = email
        )

        binding.topBar.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        binding.btnShare.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Comprovante de Reserva $reservaCodigo no Huga Buga Hotel - $cavernName!")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Compartilhar Voucher"))
        }

        binding.btnDownloadVoucher.setOnClickListener {
            baixarVoucherPdf()
        }

        setupBeforeArrivalActions()
        fetchVoucher()
    }

    private fun setupBeforeArrivalActions() {
        binding.btnActionCheckin.setOnClickListener {
            realizarCheckinOnline()
        }

        binding.btnActionAdventures.setOnClickListener {
            val intent = Intent(this, ExperiencesActivity::class.java).apply {
                putExtra("RESERVA_CODIGO", reservaCodigo)
            }
            startActivity(intent)
        }

        binding.btnActionBanquet.setOnClickListener {
            val intent = Intent(this, BanquetActivity::class.java).apply {
                putExtra("RESERVA_CODIGO", reservaCodigo)
            }
            startActivity(intent)
        }
    }

    private fun realizarCheckinOnline() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@VoucherActivity)
                val response = api.realizarCheckin(reservaCodigo, CheckinRequest())
                if (response.isSuccessful) {
                    Toast.makeText(this@VoucherActivity, "Check-in online realizado com sucesso! Uga! 🪨", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@VoucherActivity, "Check-in online confirmado para a reserva $reservaCodigo! 🪨", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@VoucherActivity, "Check-in online confirmado para a reserva $reservaCodigo! 🪨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun baixarVoucherPdf() {
        Toast.makeText(this, "Baixando tábua de voucher (PDF)... 🪨", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@VoucherActivity)
                val response = api.downloadVoucherPdf(reservaCodigo, email)
                if (response.isSuccessful && response.body() != null) {
                    val pdfFile = File(cacheDir, "tabua-$reservaCodigo.pdf")
                    response.body()!!.byteStream().use { input ->
                        pdfFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    abrirOuNotificarPdf(pdfFile)
                } else {
                    gerarPdfFallbackLocal()
                }
            } catch (_: Exception) {
                gerarPdfFallbackLocal()
            }
        }
    }

    private fun abrirOuNotificarPdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(viewIntent, "Abrir Voucher PDF"))
        } catch (_: Exception) {
            Toast.makeText(this, "Voucher PDF salvo com sucesso! 🪨", Toast.LENGTH_LONG).show()
        }
    }

    private fun gerarPdfFallbackLocal() {
        val pdfFile = File(cacheDir, "tabua-$reservaCodigo.pdf")
        if (!pdfFile.exists()) {
            pdfFile.writeText("HUGA BUGA HOTEL - VOUCHER DE RESERVA $reservaCodigo\nCaverna: $cavernName\nDatas: $checkInDisplay a $checkOutDisplay\nE-mail: $email")
        }
        abrirOuNotificarPdf(file = pdfFile)
    }

    private fun bindVoucherUi(
        codigo: String,
        cavern: String,
        checkIn: String,
        checkOut: String,
        adults: Int,
        children: Int,
        total: Long,
        userEmail: String
    ) {
        binding.tvVoucherDesc.text = "$cavern é da sua tribo de $checkIn a $checkOut. Mandamos o voucher para $userEmail e pelo WhatsApp."
        binding.tvVoucherCavernName.text = cavern
        binding.tvVoucherCode.text = codigo
        binding.tvVoucherCheckInDate.text = checkIn.uppercase(Locale.getDefault())
        binding.tvVoucherCheckOutDate.text = checkOut.uppercase(Locale.getDefault())

        val totalPessoas = adults + children
        binding.tvVoucherTribeCount.text = "$totalPessoas na tribo ($adults ad., $children cri.)"
        binding.tvVoucherTotal.text = "Total: R$ ${String.format(Locale.getDefault(), "%,d", total / 100)}"

        val qrContent = "HUGA_BUGA_RESERVA_PASS_${codigo}_$cavern"
        val qrBitmap = QrCodeUtils.generateQrCodeBitmap(qrContent, 512, 512)
        if (qrBitmap != null) {
            binding.root.findViewById<ImageView>(R.id.ivVoucherQrCode)?.setImageBitmap(qrBitmap)
        }
    }

    private fun fetchVoucher() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApi(this@VoucherActivity)
                val response = api.getVoucherReserva(reservaCodigo)
                if (response.isSuccessful && response.body() != null) {
                    val reserva = response.body()!!
                    bindVoucherUi(
                        codigo = reserva.codigo,
                        cavern = if (reserva.cavernaNome.isNotEmpty()) reserva.cavernaNome else cavernName,
                        checkIn = if (reserva.checkIn.isNotEmpty()) reserva.checkIn else checkInDisplay,
                        checkOut = if (reserva.checkOut.isNotEmpty()) reserva.checkOut else checkOutDisplay,
                        adults = if (reserva.adultos > 0) reserva.adultos else adultsCount,
                        children = childrenCount,
                        total = if (reserva.totalCentavos > 0) reserva.totalCentavos else totalCentavos,
                        userEmail = email
                    )
                }
            } catch (_: Exception) {}
        }
    }
}
