package com.example.hugabugahotel.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Para chamadas que precisam terminar mesmo se a tela fechar (ex.: salvar preferência e navegar). */
object AppScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)
