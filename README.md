# Desafio Musical v1.0 — Fase 1

App Android de party game musical (Kotlin + Jetpack Compose), implementado a partir da documentação técnica fornecida. Esta entrega cobre a **Fase 1**: estrutura base do projeto, arquitetura, banco de dados local, motor de regras (`GameEngine`) e o fluxo completo do **modo um celular (pass-and-play)**.

## Status desta entrega

| Área | Status |
|---|---|
| Estrutura de projeto, Gradle, tema | ✅ Completo |
| Banco Room (entidades, DAOs, catálogo inicial) | ✅ Completo |
| `GameEngine` (pontuação, papéis, cronômetro, roubo, máquina de estados) | ✅ Completo |
| Fluxo pass-and-play (Home → Setup → Rodadas → Placar Final) | ✅ Completo |
| Testes unitários de regras (pontuação, papéis, máquina de estados) | ✅ Completo |
| Multiplayer local (NSD/sockets, salas, QR Code) | ⏳ Fase 3 — apenas contratos (`RoomSession`, `GameEvent`) prontos |
| Player do YouTube embutido | ⏳ Fase 4 — `youtubeVideoId` já modelado, UI de player ainda não |
| Histórico/estatísticas na UI, conquistas | ⏳ Fase 6 — persistência já implementada, tela de histórico é um placeholder |

## Arquitetura

Clean Architecture + MVVM reativo:

```
app/src/main/java/com/desafiomusical/app/
├── data/
│   ├── local/        # DataStore (preferências leves: últimos nomes, roubo padrão)
│   ├── dto/           # DTOs serializáveis para um catálogo remoto futuro (V2)
│   ├── mapper/        # Entity <-> Domain
│   ├── repository/     # SongRepository, PlayerRepository, GameHistoryRepository
│   └── room/           # Database, DAOs, entidades, catálogo inicial
├── domain/
│   ├── model/          # Player, Song, Category, GameConfig, ScoreBreakdown, ...
│   ├── state/           # GamePhase (enum estrito), GameStateMachine, GameUiState (sealed)
│   ├── usecase/         # CalculateScoreUseCase, DistributeRolesUseCase, SelectSongUseCase, TieBreakUseCase
│   └── GameEngine.kt    # Autoridade única do estado da partida
├── network/
│   ├── multiplayer/     # RoomSession (contrato — implementação na Fase 3)
│   ├── payloads/         # GameEvent (sealed, idempotente), IdempotencyGuard
│   └── sockets/          # (reservado para Fase 3 — Ktor/NSD)
├── di/                  # AppContainer — injeção de dependências manual
└── ui/
    ├── theme/            # Cores neon, tipografia, formas — tema único e escuro
    ├── components/        # BigAnswerButton, NeonTimer, ScoreBadge, RoleChip...
    ├── navigation/         # Routes, DesafioMusicalNavHost
    └── screens/            # home, setup, game (Category/Song/Chooser/Playing/Answering/Steal/Result), common
```

### Por que o `GameEngine` é o "host" mesmo no modo um celular?

A especificação exige que toda pontuação, validação de roubo e transição de estado seja decidida por uma autoridade única — nunca pelo cliente. Para que a mesma lógica sirva ao modo pass-and-play (Fase 1) e ao multiplayer local (Fase 3) sem reescrita, o `GameEngine` já se comporta como essa autoridade: ele expõe um único `StateFlow<GameUiState>` (sealed class com um caso por estado da máquina) e nunca aceita uma transição fora da tabela definida em `GameStateMachine`. Na Fase 3, um `RoomSession` (contrato já definido em `network/multiplayer`) vai apenas transportar os mesmos eventos entre host e clientes — o motor de regras não muda.

### Privacidade de informação

`ActiveRoundView` (o que respondentes e demais jogadores veem) nunca contém título, artista ou obra — apenas `MaskedSong` (categoria, dificuldade, dicas já reveladas, YouTube ID). Só `ChooserRoundView` (exclusivo do escolhedor) e as telas de confirmação de resposta (`Answering`/`StealAnswer`, onde alguém *precisa* validar a resposta) recebem a música completa. Isso já separa estado público e privado na camada de domínio, então a mesma regra vale quando o transporte de rede for plugado na Fase 3.

## Banco de dados (Room)

Entidades: `PlayerEntity`, `SongEntity`, `GameEntity`, `GamePlayerEntity`, `RoundEntity`, `RoundScoreEntity`, `AttemptEntity` — espelhando a seção 28 da especificação (com o campo adicional `stealEnabled` em `GameEntity`, necessário para reconstruir o histórico da regra "Roubo ON/OFF" da seção 4).

O catálogo inicial (`InitialSongCatalog.kt`) é inserido automaticamente na primeira execução via `RoomDatabase.Callback`, com **12 músicas** (2 por categoria): Brasileira, Internacional, Popular, Games, Personagens, Anime.

### Sobre os YouTube video IDs do catálogo

Os IDs vêm como placeholder (`REPLACE_WITH_YOUTUBE_ID__...`) — a especificação já antecipa isso ("YouTubeVideoId: configurável"). Eu não inventei IDs de vídeo reais porque um ID incorreto levaria a um vídeo errado ou a um erro de reprodução, o que é pior do que deixar explícito que precisa ser preenchido. **Antes de testar a integração com YouTube (Fase 4), edite `data/room/InitialSongCatalog.kt`** e substitua cada placeholder pelo ID real do vídeo oficial correspondente.

### Adicionar novas músicas

Adicione entradas em `InitialSongCatalog.songs` (ou insira via `SongDao.upsertAll` em runtime) preenchendo todos os campos, especialmente as 3 dicas progressivas — elas não podem ser vazias nem repetir o título/artista diretamente, conforme a seção 13 da especificação.

## Motor de regras — resumo

- **Tempo**: 0–30s = +10 · 31–60s = +5 · 61–90s = +0 (`CalculateScoreUseCase.timePointsFor`)
- **Bônus**: +5 título certo, +5 artista certo
- **Dicas**: dica 1 = -1, dica 2 = -2 (acumulado -3), dica 3 = -3 (acumulado -6, teto)
- **Papéis**: rodízio simples — a cada rodada o escolhedor avança uma posição e o respondente principal é o próximo jogador da lista (`DistributeRolesUseCase`), garantindo distribuição equilibrada e evitando repetição
- **Cronômetro**: global de 90s, nunca reinicia em erro/roubo; força o fim da rodada mesmo em uma janela de roubo aberta
- **Roubo**: janela de 5s, primeiro toque (no pass-and-play, quem segura o aparelho registra manualmente), quem erra é eliminado só naquela rodada, escolhedor nunca é elegível

## Executando o projeto

1. Abra a pasta do projeto no **Android Studio** (Koala/Ladybug ou mais recente). O Android Studio gera o `gradlew`/`gradle-wrapper.jar` automaticamente na primeira sincronização — este repositório já inclui `gradle/wrapper/gradle-wrapper.properties` apontando para o Gradle 8.9.
   - Se preferir gerar o wrapper manualmente antes de abrir no Studio: com o Gradle instalado localmente, rode `gradle wrapper --gradle-version 8.9` na raiz do projeto.
2. Deixe o Android Studio baixar o Android SDK (compileSdk/targetSdk 35, minSdk 26) e sincronizar as dependências.
3. Rode o módulo `app` em um emulador ou dispositivo físico (API 26+).

### Testes

```bash
./gradlew testDebugUnitTest
```

Cobrem (seção 35 da especificação): os quatro casos de pontuação por tempo, penalidades de dica (-1/-3/-6), distribuição de papéis (inclui o exemplo de 4 jogadores da própria documentação), elegibilidade/eliminação de roubo, transições válidas e inválidas da máquina de estados, e o fluxo ponta a ponta de uma rodada dentro do `GameEngine`.

### Testando com 2, 3 e 4 jogadores (modo um celular)

Esta fase entrega o modo pass-and-play: em "Nova Partida", escolha de 2 a 4 jogadores, digite os nomes, escolha rodadas (5/10/15/20) e Roubo ON/OFF. O aparelho é passado fisicamente entre os jogadores — a tela do escolhedor mostra a resposta, a tela seguinte é do respondente principal, e a tela de confirmação (mostrada a quem sabe a resposta) valida o que foi dito em voz alta, exatamente como a seção 11 da especificação exige (sem reconhecimento de voz na V1).

**Testar com 2/3/4 celulares simultâneos (multiplayer via Wi-Fi) ainda não é possível nesta fase** — ver limitações abaixo.

## Dependências principais

Compose BOM 2024.10.00, Navigation Compose 2.8, Room 2.6.1 (KSP), Ktor 2.3.12 (client + server, já incluído para a Fase 3), kotlinx.serialization, kotlinx.coroutines, DataStore Preferences, ZXing (QR Code, para a Fase 3). Sem Hilt/Koin — a injeção de dependências é manual (`AppContainer`) por ser um projeto de porte pequeno/médio.

## Limitações conhecidas desta entrega

- **Multiplayer local (Wi-Fi) não está implementado** — apenas os contratos (`RoomSession`, `GameEvent`, `IdempotencyGuard`) que a Fase 3 vai usar para não precisar redesenhar o `GameEngine`.
- **Player do YouTube não está embutido** — o `youtubeVideoId` já existe no modelo e no catálogo, mas a tela de reprodução (WebView oficial ou YouTube Player SDK) é escopo da Fase 4. Por ora, `startPlayback()` apenas inicia o cronômetro.
- **IDs de vídeo do catálogo são placeholders** (ver seção acima) — preencher antes de testar a reprodução real.
- **Histórico e estatísticas agregadas** já são persistidos no Room ao final de cada partida (`GameHistoryRepository`), mas a tela de "Histórico" no menu principal ainda é um placeholder ("em breve") — a tela de estatísticas por partida (resultado final) já está completa.
- **Empate/morte súbita**: `TieBreakUseCase` detecta o empate no placar final, mas o fluxo de rodada extra (sortear música, primeiro a acertar vence) ainda não está integrado à máquina de estados — fica para quando o modo multiplayer também precisar dele.
- Sem reconhecimento de voz (deliberado, conforme seção 11 da especificação).

## Próximos passos sugeridos

1. **Fase 2**: polir o modo um celular (transições/animações, tratamento de "app em segundo plano" durante uma rodada).
2. **Fase 3**: implementar `RoomSession` com NSD (descoberta) + Ktor (WebSocket) sobre Wi-Fi, sala com código curto + QR Code (ZXing), reconexão.
3. **Fase 4**: integrar o YouTube (WebView oficial ou YouTube Player SDK — nunca extração de áudio).
4. **Fase 5**: acessibilidade (contraste, tamanhos de tela), animações.
5. **Fase 6**: tela de histórico/estatísticas na UI e conquistas.
