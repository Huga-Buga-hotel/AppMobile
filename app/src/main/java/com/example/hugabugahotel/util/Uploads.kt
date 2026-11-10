package com.example.hugabugahotel.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Monta as partes multipart das fotos da avaliação (campo "fotos", até 5 de 25 MB, como exige a API). */
object Uploads {
    const val MAX_FOTOS = 5
    const val MAX_BYTES = 25L * 1024 * 1024

    /** Lança IllegalArgumentException com mensagem amigável se alguma foto passar do limite. */
    fun partesDeFotos(context: Context, uris: List<Uri>): List<MultipartBody.Part> {
        require(uris.size <= MAX_FOTOS) { "Escolha no máximo $MAX_FOTOS fotos." }
        return uris.mapIndexed { i, uri ->
            val tamanho = tamanho(context, uri)
            require(tamanho <= MAX_BYTES) { "A foto ${i + 1} passa de 25 MB." }
            val tipo = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("Não conseguimos ler a foto ${i + 1}.")
            val ext = when (tipo) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
            MultipartBody.Part.createFormData("fotos", "foto${i + 1}.$ext", bytes.toRequestBody(tipo.toMediaTypeOrNull()))
        }
    }

    fun texto(valor: String) = valor.toRequestBody("text/plain".toMediaTypeOrNull())

    private fun tamanho(context: Context, uri: Uri): Long =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else 0L
        } ?: 0L
}
