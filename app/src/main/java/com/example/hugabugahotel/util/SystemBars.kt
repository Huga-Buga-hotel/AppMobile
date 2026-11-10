package com.example.hugabugahotel.util

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.hugabugahotel.R

/**
 * Com targetSdk 35 o Android 15 força edge-to-edge: a tela desenha atrás da status bar e da barra de
 * navegação. Aqui o conteúdo recebe padding com o tamanho dessas barras (e do teclado), a faixa da
 * status bar fica escura como o cabeçalho e a faixa de baixo acompanha a cor da tela.
 * Fazemos o mesmo em todas as versões do Android para o visual ser igual.
 */
fun Activity.aplicarBarrasDoSistema() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = false     // ícones claros sobre o cabeçalho escuro
        isAppearanceLightNavigationBars = true  // ícones escuros sobre o fundo claro
    }

    val conteudo = findViewById<ViewGroup>(android.R.id.content)
    val corTopo = ContextCompat.getColor(this, R.color.hb_dark)

    ViewCompat.setOnApplyWindowInsetsListener(conteudo) { view, insets ->
        val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.setPadding(barras.left, barras.top, barras.right, maxOf(barras.bottom, teclado.bottom))

        // Telas com a barra de navegação do app usam o "papel" embaixo; as demais, o fundo da tela.
        val temBottomNav = view.findViewById<View>(R.id.bottomNav) != null
        val corBase = ContextCompat.getColor(this, if (temBottomNav) R.color.hb_paper else R.color.hb_cream)
        view.background = LayerDrawable(arrayOf(ColorDrawable(corBase), ColorDrawable(corTopo))).apply {
            setLayerGravity(1, android.view.Gravity.TOP or android.view.Gravity.FILL_HORIZONTAL)
            setLayerHeight(1, barras.top)
        }
        WindowInsetsCompat.CONSUMED
    }
}
