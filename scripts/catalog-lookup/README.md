# Catalog lookup — busca assistida de vídeos do YouTube

Ferramenta de linha de comando para preencher os `youtubeVideoId` do catálogo (`app/src/main/assets/catalog.json`) sem precisar copiar/colar links manualmente para cada música.

**Não escolhe nada sozinho** — busca candidatos via YouTube Data API v3 e pede para você confirmar qual é o vídeo certo antes de salvar.

## Setup

1. Crie uma API key gratuita no [Google Cloud Console](https://console.cloud.google.com) com a **YouTube Data API v3** ativada (ver instruções detalhadas que o Claude te passou no chat).
2. Crie o arquivo `scripts/catalog-lookup/.env` (já está no `.gitignore`, não será commitado):
   ```
   YOUTUBE_API_KEY=sua_chave_aqui
   ```
   Ou passe a chave direto na hora de rodar, sem salvar em arquivo:
   ```bash
   YOUTUBE_API_KEY=sua_chave node scripts/catalog-lookup/lookup.mjs
   ```

## Uso

```bash
node scripts/catalog-lookup/lookup.mjs
```

Para cada música do catálogo que ainda tem um `youtubeVideoId` placeholder (`REPLACE_WITH_YOUTUBE_ID__...`), o script mostra até 5 candidatos e você digita o número do correto (ou `0` para pular). O `catalog.json` é salvo a cada escolha — pode interromper (`Ctrl+C`) e retomar depois, ele continua de onde parou.

## Adicionar músicas novas ao catálogo

Esse script só preenche IDs de vídeo de músicas que já existem no `catalog.json`. Para adicionar uma música nova, edite `app/src/main/assets/catalog.json` primeiro (título, artista, categoria, obra, dificuldade, as 3 dicas, tags) com `"youtubeVideoId": "REPLACE_WITH_YOUTUBE_ID__ALGO_UNICO"` — depois rode o script para ele resolver o ID de verdade.

## Cota da API

A YouTube Data API tem cota gratuita de 10.000 unidades/dia; cada busca (`search.list`) custa 100 unidades — então dá para resolver uns 100 músicas por dia sem custo. Se estourar a cota, o script vai mostrar erro 403/`quotaExceeded` — é só esperar o reset (meia-noite Pacífico/EUA) ou pedir aumento de cota no console.
