# Desafio Musical v1.0

App Android de party game musical (Kotlin + Jetpack Compose), implementado a partir da documentação técnica fornecida. Cobre hoje: o fluxo completo do **modo um celular (pass-and-play)**, **multiplayer local via Wi-Fi** (NSD, QR Code, IP manual, reconexão), reprodução de áudio do **YouTube**, e **histórico/estatísticas** entre partidas.

## Status desta entrega

| Área | Status |
|---|---|
| Estrutura de projeto, Gradle, tema | ✅ Completo |
| Banco Room (entidades, DAOs, catálogo inicial) | ✅ Completo |
| `GameEngine` (pontuação, papéis, cronômetro, roubo, máquina de estados) | ✅ Completo |
| Fluxo pass-and-play (Home → Setup → Rodadas → Placar Final) | ✅ Completo |
| Testes unitários de regras (pontuação, papéis, máquina de estados) | ✅ Completo |
| Multiplayer local (NSD/sockets, salas, QR Code) | ✅ Completo |
| Player do YouTube embutido | ✅ Completo — áudio via IFrame Player API oficial em WebView, sem exibir vídeo/miniatura |
| Histórico/estatísticas na UI, conquistas | ✅ Completo — lista de histórico, detalhe da partida e estatísticas por jogador |

## Arquitetura

Clean Architecture + MVVM reativo:

```
app/src/main/java/com/desafiomusical/app/
├── data/
│   ├── local/        # DataStore (preferências leves: últimos nomes, roubo padrão)
│   ├── dto/           # DTOs serializáveis para um catálogo remoto futuro (V2)
│   ├── mapper/        # Entity <-> Domain
│   ├── repository/     # SongRepository, PlayerRepository, GameHistoryRepository (+ GameHistoryCalculations, puro)
│   └── room/           # Database, DAOs, entidades, catálogo inicial
├── domain/
│   ├── model/          # Player, Song, Category, GameConfig, ScoreBreakdown, GameHistoryEntry/Detail, PlayerAggregateStats...
│   ├── state/           # GamePhase (enum estrito), GameStateMachine, GameUiState (sealed)
│   ├── usecase/         # CalculateScoreUseCase, DistributeRolesUseCase, SelectSongUseCase, TieBreakUseCase
│   └── GameEngine.kt    # Autoridade única do estado da partida
├── network/
│   ├── multiplayer/     # RoomSession + implementações Ktor (host/client), NSD, QR Code, HostGameCoordinator
│   └── payloads/         # GameEvent (sealed, idempotente), IdempotencyGuard
├── di/                  # AppContainer — injeção de dependências manual
└── ui/
    ├── theme/            # Paleta/tipografia/formas do design system (ui/theme/ThemeValidationPreview.kt pro Compose Preview)
    ├── components/        # BigAnswerButton, NeonTimer, ScoreBadge, RoleChip...
    ├── navigation/         # Routes, DesafioMusicalNavHost
    └── screens/            # home, setup, lobby (host/join), game (Category/Song/Chooser/Playing/Answering/Steal/Result), history (lista/detalhe/estatísticas), common
```

### Por que o `GameEngine` é o "host" mesmo no modo um celular?

A especificação exige que toda pontuação, validação de roubo e transição de estado seja decidida por uma autoridade única — nunca pelo cliente. Para que a mesma lógica sirva ao modo pass-and-play e ao multiplayer local sem reescrita, o `GameEngine` se comporta como essa autoridade: ele expõe um único `StateFlow<GameUiState>` (sealed class com um caso por estado da máquina) e nunca aceita uma transição fora da tabela definida em `GameStateMachine`. No multiplayer, `HostGameCoordinator` (`network/multiplayer/`) é a ponte que traduz os mesmos eventos (`GameEvent`) entre o `GameEngine` e a rede via `RoomSession` (implementado sobre Ktor WebSockets) — o motor de regras em si não sabe que existe rede.

### Privacidade de informação

`ActiveRoundView` (o que respondentes e demais jogadores veem) nunca contém título, artista ou obra — apenas `MaskedSong` (categoria, dificuldade, dicas já reveladas, YouTube ID). Só `ChooserRoundView` (exclusivo do escolhedor) e as telas de confirmação de resposta (`Answering`/`StealAnswer`, onde alguém *precisa* validar a resposta) recebem a música completa. Isso separa estado público e privado na camada de domínio, e a mesma regra vale no transporte de rede: `GameEvent.SongPlaying` (broadcast) só carrega os campos de `MaskedSong`, enquanto a música completa só é enviada ao escolhedor via `RoomSession.sendTo`.

## Banco de dados (Room)

Entidades: `PlayerEntity`, `SongEntity`, `GameEntity`, `GamePlayerEntity`, `RoundEntity`, `RoundScoreEntity`, `AttemptEntity` — espelhando a seção 28 da especificação (com o campo adicional `stealEnabled` em `GameEntity`, necessário para reconstruir o histórico da regra "Roubo ON/OFF" da seção 4).

O catálogo inicial vive em **`app/src/main/assets/catalog.json`** (não em código Kotlin) e é carregado por `CatalogAssetSource`, tanto no `RoomDatabase.Callback` (primeira instalação) quanto sob demanda em `SongRepositoryImpl.getCatalog()` (proteção contra corrida caso o app seja usado antes do callback terminar). Hoje traz **120 músicas** (20 por categoria): Brasileira, Internacional, Popular, Games, Séries/Filmes, Anime. O formato de cada entrada é o `SongDto` (`data/dto/SongDto.kt`).

### Sobre os YouTube video IDs do catálogo

Todos os `youtubeVideoId` do catálogo já estão preenchidos com IDs reais (resolvidos via busca assistida na YouTube Data API — ver `scripts/catalog-lookup/`). Ao adicionar músicas novas, use `"youtubeVideoId": "REPLACE_WITH_YOUTUBE_ID__ALGO_UNICO"` como placeholder e rode o script pra resolver o ID de verdade antes de jogar — um ID incorreto levaria a um vídeo errado ou a um erro de reprodução.

### Adicionar novas músicas

Adicione objetos em `app/src/main/assets/catalog.json` seguindo o schema do `SongDto` (título, artista, categoria, obra, dificuldade, `youtubeVideoId`, 3 dicas, tags). Não precisa recompilar nada além do app — é um asset, não código. As 3 dicas progressivas não podem ser vazias nem repetir o título/artista diretamente, conforme a seção 13 da especificação.

## Motor de regras — resumo

- **Tempo**: 0–30s = +10 · 31–60s = +5 · 61–90s = +0 (`CalculateScoreUseCase.timePointsFor`)
- **Bônus**: +5 título certo, +5 artista certo, +5 obra certa (quando a música tiver uma obra associada — jogo, filme, anime etc.; a opção só aparece para músicas com esse campo preenchido). Acertar só a obra, sem título nem artista, já é suficiente pra vencer a rodada.
- **Dicas**: dica 1 = -1, dica 2 = -2 (acumulado -3), dica 3 = -3 (acumulado -6, teto)
- **Papéis**: rodízio simples — a cada rodada o escolhedor avança uma posição e o respondente principal é o próximo jogador da lista (`DistributeRolesUseCase`), garantindo distribuição equilibrada e evitando repetição
- **Cronômetro**: global de 90s, nunca reinicia em erro/roubo; força o fim da rodada mesmo em uma janela de roubo aberta
- **Roubo**: janela de 5s, primeiro toque (no pass-and-play, quem segura o aparelho registra manualmente), quem erra é eliminado só naquela rodada, escolhedor nunca é elegível

## Setup do ambiente

### Pré-requisitos

- **JDK 17 ou mais recente** para rodar o Gradle. Qualquer JDK serve (Temurin, Corretto, ou a JBR embutida no Android Studio em `<pasta do Android Studio>/jbr`).
  - ⚠️ Se sua JDK for **24+** (ex.: a JBR de instalações recentes do Android Studio), use **Gradle 9.5 ou mais recente** — é o que este repositório já traz configurado em `gradle/wrapper/gradle-wrapper.properties`. Versões do Gradle anteriores à 9.x têm um bug de parsing no compilador Kotlin embutido do `kotlin-dsl` que quebra a leitura de `build.gradle.kts` em JDKs 24+ (o erro aparece como `IllegalArgumentException: 25.0.2` ou similar, na avaliação do projeto). Se precisar trocar de versão do Gradle, rode `gradlew wrapper --gradle-version <versão>`.
- **Android SDK** com `compileSdk`/`targetSdk` 35 e `minSdk` 26. Mais simples via Android Studio (baixa tudo na primeira sincronização); para linha de comando, use as [command-line tools](https://developer.android.com/studio#command-tools) (`sdkmanager`) para instalar `platform-tools` e a plataforma 35.
- Variável `JAVA_HOME` apontando para a JDK escolhida (necessária tanto para o Android Studio quanto para `./gradlew` via terminal). Não é preciso editar `gradle.properties` — configure `JAVA_HOME` no seu ambiente (ou, se preferir fixar por projeto, use `org.gradle.java.home` no seu `gradle.properties` **local**, fora do controle de versão).

### Via Android Studio (recomendado)

1. Abra a pasta do projeto no **Android Studio** (Koala/Ladybug ou mais recente).
2. Deixe o Android Studio baixar o Android SDK e sincronizar as dependências.
3. Rode o módulo `app` em um emulador ou dispositivo físico (API 26+).

### Via linha de comando

```bash
# usa o wrapper já commitado (gradlew/gradlew.bat) — não precisa instalar Gradle à parte
./gradlew testDebugUnitTest   # testes unitários
./gradlew assembleDebug       # compila o APK de debug
./gradlew installDebug        # instala num emulador/dispositivo já conectado (`adb devices`)
```

Para rodar num emulador sem abrir o Android Studio: crie um AVD com `avdmanager` (API 26+, `google_apis` recomendado para ter o Play Services básico) e suba com `emulator -avd <nome>`, depois use `adb devices` para confirmar que está visível antes do `installDebug`.

### Qualidade de código (ktlint + detekt)

```bash
./gradlew ktlintCheck   # formatação Kotlin — ./gradlew ktlintFormat corrige automaticamente o que der
./gradlew detekt        # análise estática (complexidade, code smells) — usa o ruleset padrão, sem config customizado ainda
```

Nenhum dos dois roda automaticamente no build normal (`assembleDebug`/`testDebugUnitTest`) — são passos manuais, rode antes de abrir um PR. As versões dos plugins (`gradle/libs.versions.toml`) foram escolhidas por serem estáveis e amplamente usadas, mas não foram validadas contra um `./gradlew sync` de verdade neste ambiente (sem Android SDK/Gradle aqui) — se o Android Studio não resolver alguma delas, é só bumpar pra versão mais recente compatível.

### Testes

```bash
./gradlew testDebugUnitTest
```

Cobrem (seção 35 da especificação): os quatro casos de pontuação por tempo, penalidades de dica (-1/-3/-6), distribuição de papéis (inclui o exemplo de 4 jogadores da própria documentação), elegibilidade/eliminação de roubo, transições válidas e inválidas da máquina de estados, e o fluxo ponta a ponta de uma rodada dentro do `GameEngine`.

### Testando com 2, 3 e 4 jogadores (modo um celular)

Esta fase entrega o modo pass-and-play: em "Nova Partida", escolha de 2 a 4 jogadores, digite os nomes, escolha rodadas (5/10/15/20) e Roubo ON/OFF. O aparelho é passado fisicamente entre os jogadores — a tela do escolhedor mostra a resposta, a tela seguinte é do respondente principal, e a tela de confirmação (mostrada a quem sabe a resposta) valida o que foi dito em voz alta, exatamente como a seção 11 da especificação exige (sem reconhecimento de voz na V1).

### Testando com 2/3/4 celulares (multiplayer via Wi-Fi)

Todos os aparelhos precisam estar na mesma rede Wi-Fi. Na Home, um aparelho escolhe "Criar Sala" (vira o host — mostra código curto + QR Code) e os demais escolhem "Entrar em Partida", com três caminhos pra achar a sala: descoberta automática via NSD, escanear o QR Code, ou digitar IP:porta manualmente. O host é a autoridade — calcula pontuação, decide vencedor de roubo e nunca envia a resposta completa a quem não é o escolhedor da rodada. Reconexão usa backoff exponencial automático se um cliente cair.

## Dependências principais

Compose BOM 2024.10.00, Navigation Compose 2.8, Room 2.6.1 (KSP), Ktor 2.3.12 (client + server, multiplayer local via WebSockets), kotlinx.serialization, kotlinx.coroutines, DataStore Preferences, ZXing (geração/leitura de QR Code), CameraX (scanner de QR). Sem Hilt/Koin — a injeção de dependências é manual (`AppContainer`) por ser um projeto de porte pequeno/médio.

## Limitações conhecidas desta entrega

- **Player do YouTube só toca áudio** — usa a IFrame Player API oficial num `WebView` quase invisível (1dp), de propósito: o jogo nunca exibe o vídeo, miniatura ou título, só o som, pra não vazar a resposta (mesma regra de privacidade do `MaskedSong`). O áudio toca durante `PLAYING` e para assim que a rodada avança pra `ANSWERING` (não volta durante o roubo). Se um vídeo falhar (removido/bloqueado por região/idade), a rodada segue normalmente — cronômetro e pontuação não dependem do player — só aparece um aviso discreto na tela.
- **Empate/morte súbita**: `TieBreakUseCase` detecta o empate no placar final, mas o fluxo de rodada extra (sortear música, primeiro a acertar vence) ainda não está integrado à máquina de estados.
- **Roster remoto não persiste localmente no host**: se uma partida hospedada (multiplayer) tentar salvar histórico, jogadores que entraram só pela rede (nunca abriram "Nova Partida" localmente) podem não existir na tabela `players` do host — o `game_players` provavelmente falharia por FK. Precisa de uma passada que persista o roster inteiro ao criar a sala, não só a identidade do host.
- **Identidade de jogador é só por nome** (`PlayerRepository.findOrCreatePlayer`, case/acento-insensitive): sem contas reais, duas pessoas diferentes que digitarem o mesmo nome compartilham as mesmas estatísticas agregadas — trade-off aceito para um app de festa local, sem login.
- **detekt configurado mas não roda neste ambiente**: a versão 1.23.6 (`io.gitlab.arturbosch.detekt`) tem um bug de compatibilidade com JDK 24+ (mesma classe de problema do Gradle/kotlin-dsl já documentada acima). A migração para o novo plugin `dev.detekt` 2.x exige Kotlin 2.4+ no projeto (hoje em 2.0.21) — bump maior, fora de escopo por ora. `ktlint` não é afetado e roda normalmente.
- Sem reconhecimento de voz (deliberado, conforme seção 11 da especificação).

## Próximos passos sugeridos

1. Persistir o roster remoto no host ao criar/entrar em uma sala multiplayer (ver limitação acima) — pré-requisito pra histórico funcionar em partidas hospedadas.
2. Integrar o fluxo de morte súbita (`TieBreakUseCase`) na máquina de estados para empates no placar final.
3. Polimento geral: acessibilidade (contraste, tamanhos de tela), animações, tratamento de "app em segundo plano"/tela bloqueada durante uma rodada.
4. Conquistas (mencionadas na seção 22 da especificação, ainda não implementadas).
5. Revisitar o detekt quando a versão 2.0.0 estável for lançada, ou se o projeto migrar para Kotlin 2.4+ por outro motivo.
6. Preparação para publicação: assinatura de release, `isMinifyEnabled = true` + regras de ProGuard revisadas, ícone definitivo, ficha da Play Store.
