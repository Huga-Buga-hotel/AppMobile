# 🪨 Huga Buga Hotel · App Android

Aplicativo Android do **Huga Buga Hotel** (projeto de conclusão de curso). Consome a API REST do backend ([Backend-Huga-Buga](../Backend-Huga-Buga)): busca de cavernas, reserva com checkout em 3 passos, pagamento simulado, voucher, atividades, restaurante O Banquete, avaliações com fotos e a conta do visitante.

> Tudo o que o app mostra vem da API: reservas, agendamentos e pagamentos não são simulados no aparelho. A referência completa das rotas está no Swagger do backend (`/api/v1/docs`).

---

## 🦖 Funcionalidades

1. **Cavernas:** catálogo e busca por datas e tribo (só aparecem cavernas com toca livre), filtros, detalhe com fotos e comodidades, cotação da estadia e favoritos (com conta).
2. **Conta:** login, cadastro (nome, e-mail, celular, CPF), "esqueci a senha", renovação automática do token, perfil, membros da tribo, idioma (sincronizado com o perfil) e histórico de pagamentos.
3. **Reserva e checkout:**
   - Passo 1: responsável (nome, e-mail, celular, CPF) e idade das crianças.
   - Passo 2: extras e **combo quarto + Banquete** (jantar todas as noites, no horário escolhido).
   - Passo 3: cartão (aprovação/recusa na hora), **Pix** (código gerado pela API, 10 min, consulta a cada 3 s) ou pagamento na chegada.
   - Voucher com QR (código da reserva), PDF ("tábua"), compartilhar e check-in online.
   - Reserva **sem conta**: o e-mail do responsável é pedido e enviado no header `X-Responsavel-Email`.
4. **Minhas reservas:** abas Próximas / Passadas / Canceladas com contagem, cancelamento (com reembolso/multa calculados pela API) e **avaliação** da estadia (nota, comentário e até 5 fotos de até 25 MB).
5. **Experiências:** catálogo, horários com vagas por dia da estadia, agendamento para N pessoas, cancelamento (até 4 h antes) e aviso de vaga. O valor vai para a conta da toca (pago no hotel).
6. **O Banquete:** turnos, cardápio por aba e reserva de mesa — de hóspede (com desconto) ou de visitante de fora — com horários LIVRE/LOTADO (salão de 50 lugares).
7. **Ajuda e Xamã:** tópicos de ajuda e chatbot (contrato provisório, ver "Pendências").

## 🛠️ Tecnologias

- Kotlin, Views + ViewBinding, Material Components, RecyclerView
- Retrofit + Gson, OkHttp (renovação de token com `Authenticator`)
- Coroutines (`lifecycleScope`), Coil (imagens), ZXing (QR code)
- R8/ProGuard no release (`isMinifyEnabled`, `isShrinkResources`)

## 🚀 Como executar

1. Suba o backend (no repositório do backend: `docker compose up -d` ou `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`).
2. Abra este projeto no **Android Studio** e sincronize o Gradle.
3. Rode no **emulador** (API 26+). No debug o app usa `http://10.0.2.2:8080/api/v1/`, que é o `localhost` do seu computador visto pelo emulador.

### Celular físico na mesma rede Wi-Fi

Descubra o IP do computador (ex.: `192.168.0.10`) e gere o debug com:

```bash
./gradlew installDebug -Phugabuga.apiUrlDebug=http://192.168.0.10:8080/api/v1/
```

### Build de produção (AWS)

O release só aceita **HTTPS** (`usesCleartextTraffic=false`). Informe a URL pública da API:

```bash
./gradlew assembleRelease -Phugabuga.apiUrl=https://api.seudominio.com.br/api/v1/
```

Ou deixe fixo em `~/.gradle/gradle.properties` (`hugabuga.apiUrl=...`). O APK sai sem assinatura; para instalar/publicar, assine com a sua keystore (Android Studio → *Build → Generate Signed App Bundle / APK*).

## 🧪 Dados de teste (seed do backend)

| Item | Valor |
|---|---|
| Visitante | `marina.souza@example.com` / `Uga!Buga2026` |
| Cartão aprovado | `4111 1111 1111 1111`, validade `12/30`, CVV `123` |
| Cartão recusado | final `0002` (saldo) ou `0069` (vencido) |
| Cupom | `UGABUGA10` |
| Pix | no perfil `dev` do backend é aprovado sozinho após ~20 s |

## 📁 Estrutura

```
app/src/main/java/com/example/hugabugahotel/
├── data/api/     ApiClient (URL por build, token, refresh), HugaBugaApi (rotas), ApiErrors, SessionManager
├── data/model/   DTOs espelhando os records do backend (centavos e datas ISO)
├── ui/           Telas por área: home, caverns, checkout, account, experiences, banquet, states, auth
└── util/         Formatos (R$ e datas), Uploads (fotos), LanguageManager, QrCodeUtils, AppScope
app/src/main/res-*/  Recursos separados por área (home, caverns, checkout, account, experiences, states)
```

## ⏳ Pendências conhecidas

- **Chatbot (Xamã):** as rotas `POST /chat` e `DELETE /chat/{id_sessao}` ainda serão alinhadas ao repositório do chatbot.
- **Login com Google/Apple:** os botões são só visuais (exigem credenciais OAuth).
- **Notificações:** são locais (ex.: alerta de vaga criado). O backend não tem caixa de notificações nem push.
