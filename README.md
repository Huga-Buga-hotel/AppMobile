# 🪨 Huga Buga Hotel · Android App

Bem-vindo ao **Huga Buga Hotel**, um aplicativo Android moderno desenvolvido em Kotlin (Jetpack Compose e Material 3 / Views), integrando arquitetura robusta, consumo de API REST via Retrofit/Coroutines e uma temática pré-histórica imersiva.

---

## 🦖 Funcionalidades Principais

1. **Cavernas e Acomodações**:
   - Catálogo de cavernas (Toca do Mamute, Gruta do Casal, Covil dos Caçadores, etc.).
   - Busca com filtros por datas (Check-in / Check-out), capacidade de hóspedes, comodidades e ordenação por preço/nota.
   - Detalhes completos, fotos, comodidades e cotação dinâmica de estadia.

2. **Autenticação e Perfil (`/me`)**:
   - Login, Cadastro, Renovação de token (Refresh Token) e Logout.
   - Gestão de membros da tribo (adultos e crianças, idades e observações alimentares).
   - Saldo de Ossos (programa de fidelidade), extrato, nível da tribo e cavernas favoritas.

3. **Reservas e Checkout**:
   - Fluxo completo de reserva (Passo 1: Hóspedes, Passo 2: Extras, Passo 3: Cupons e Uso de Ossos).
   - Pagamentos seguros (Cartão de crédito, Pix com desconto de 5% e temporizador de expiração, ou pagamento na chegada).
   - Check-in online antecipado e visualização de Voucher completo / Download de Tábua em PDF.
   - Histórico de reservas divididas por abas (*Próximas*, *Passadas*, *Canceladas*).

4. **Experiências e Aventuras da Tribo**:
   - Catálogo de atividades e oficinas para adultos e crianças.
   - Agendamento por dia e horário relacionado à estadia da reserva.
   - Sincronização dinâmica de vagas disponíveis direto da API.
   - Sistema de alertas de vaga com notificações integradas.
   - Gerenciamento de experiências agendadas com opção de cancelamento e acerto pendente (Pix/Cartão) pelo app.

5. **O Banquete (Restaurante)**:
   - Cardápio completo por categorias e turnos.
   - Sistema de reserva de mesa na fogueira central.

6. **Ajuda da Tribo e Chatbot com a Xamã**:
   - Tópicos de suporte temáticos (Check-in, Wi-Fi "Espírito da Caverna", Fogueira, Ossos, Cancelamentos).
   - Chatbot integrado (`POST /chat` e `DELETE /chat/{id_sessao}`) para conversar com o oráculo ("Xamã").

7. **Otimização e Segurança (ProGuard / R8)**:
   - Minificação e compactação de recursos ativadas (`isMinifyEnabled = true`, `isShrinkResources = true`).
   - Regras personalizadas de ProGuard configuradas em `proguard-rules.pro`.

---

## 🛠️ Tecnologias e Arquitetura

- **Linguagem**: Kotlin
- **UI**: Jetpack Compose, Material 3, ViewBinding, ConstraintLayout, RecyclerView
- **Conectividade**: Retrofit, OkHttp (Logging Interceptor), Gson
- **Concorrência**: Kotlin Coroutines, LifecycleScope
- **Gerenciamento de Estado**: SessionManager, SharedPreferences

---

## 🚀 Como Executar o Projeto

1. Clone o repositório ou abra o projeto no **Android Studio**.
2. Certifique-se de que o backend da API está rodando (ex.: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`).
3. Sincronize o projeto com o Gradle (`Sync Project with Gradle Files`).
4. Execute o aplicativo em um emulador ou dispositivo físico Android (API 26+).
