#!/usr/bin/env node
/**
 * Busca assistida de IDs de vídeo do YouTube para o catálogo do Desafio Musical.
 *
 * Para cada música em app/src/main/assets/catalog.json cujo youtubeVideoId
 * ainda seja um placeholder (REPLACE_WITH_YOUTUBE_ID__...), busca candidatos
 * via YouTube Data API v3 e pede para você escolher qual está correto — nunca
 * escolhe sozinho. Grava o progresso no próprio catalog.json a cada escolha,
 * então pode ser interrompido e retomado a qualquer momento.
 *
 * Uso:
 *   YOUTUBE_API_KEY=sua_chave node scripts/catalog-lookup/lookup.mjs
 * ou crie scripts/catalog-lookup/.env com a linha: YOUTUBE_API_KEY=sua_chave
 *
 * Flag opcional:
 *   --category=GAMES   restringe às músicas pendentes dessa categoria
 */

import { readFile, writeFile } from "node:fs/promises";
import { createInterface } from "node:readline/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CATALOG_PATH = path.resolve(__dirname, "../../app/src/main/assets/catalog.json");
const PLACEHOLDER_PREFIX = "REPLACE_WITH_YOUTUBE_ID__";

async function loadApiKey() {
  if (process.env.YOUTUBE_API_KEY) return process.env.YOUTUBE_API_KEY;
  try {
    const envPath = path.resolve(__dirname, ".env");
    const text = await readFile(envPath, "utf-8");
    const match = text.match(/^YOUTUBE_API_KEY=(.+)$/m);
    if (match) return match[1].trim();
  } catch {
    // sem .env, tudo bem — segue sem chave e falha com mensagem clara abaixo
  }
  return null;
}

async function searchCandidates(apiKey, query) {
  const url = new URL("https://www.googleapis.com/youtube/v3/search");
  url.searchParams.set("part", "snippet");
  url.searchParams.set("q", query);
  url.searchParams.set("type", "video");
  url.searchParams.set("maxResults", "5");
  url.searchParams.set("videoEmbeddable", "true");
  url.searchParams.set("key", apiKey);

  const response = await fetch(url);
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`YouTube API retornou ${response.status}: ${body}`);
  }
  const data = await response.json();
  return data.items.map((item) => ({
    videoId: item.id.videoId,
    title: item.snippet.title,
    channelTitle: item.snippet.channelTitle,
  }));
}

function stripHtmlEntities(text) {
  return text.replace(/&amp;/g, "&").replace(/&#39;/g, "'").replace(/&quot;/g, '"');
}

async function main() {
  const apiKey = await loadApiKey();
  if (!apiKey) {
    console.error(
      "Nenhuma YOUTUBE_API_KEY encontrada.\n" +
        "Rode com: YOUTUBE_API_KEY=sua_chave node scripts/catalog-lookup/lookup.mjs\n" +
        "ou crie scripts/catalog-lookup/.env com YOUTUBE_API_KEY=sua_chave"
    );
    process.exit(1);
  }

  const categoryArg = process.argv.find((a) => a.startsWith("--category="));
  const categoryFilter = categoryArg ? categoryArg.split("=")[1].toUpperCase() : null;

  const catalog = JSON.parse(await readFile(CATALOG_PATH, "utf-8"));
  const pending = catalog.filter(
    (song) => song.youtubeVideoId.startsWith(PLACEHOLDER_PREFIX) && (!categoryFilter || song.category === categoryFilter)
  );

  if (pending.length === 0) {
    console.log(
      categoryFilter
        ? `Nenhuma música pendente na categoria ${categoryFilter}.`
        : "Nenhuma música pendente — todos os youtubeVideoId já foram preenchidos."
    );
    return;
  }

  console.log(`${pending.length} música(s) pendente(s) de ${catalog.length} no catálogo.\n`);

  const rl = createInterface({ input: process.stdin, output: process.stdout });

  for (const song of pending) {
    console.log(`\n=== ${song.title} — ${song.artist} (${song.category}) ===`);
    const query = `${song.artist} ${song.title}${song.work ? " " + song.work : ""}`;

    let candidates;
    try {
      candidates = await searchCandidates(apiKey, query);
    } catch (err) {
      console.error(`Erro ao buscar "${query}": ${err.message}`);
      continue;
    }

    if (candidates.length === 0) {
      console.log("Nenhum resultado encontrado. Pulei essa música.");
      continue;
    }

    candidates.forEach((c, i) => {
      console.log(`  [${i + 1}] ${stripHtmlEntities(c.title)} — ${c.channelTitle} (id: ${c.videoId})`);
    });
    console.log("  [0] pular / decidir depois");

    const answer = await rl.question("Escolha o número correto: ");
    const index = parseInt(answer.trim(), 10);

    if (!Number.isInteger(index) || index === 0 || index > candidates.length) {
      console.log("Pulado.");
      continue;
    }

    song.youtubeVideoId = candidates[index - 1].videoId;
    await writeFile(CATALOG_PATH, JSON.stringify(catalog, null, 2) + "\n", "utf-8");
    console.log(`Salvo: ${song.title} -> ${song.youtubeVideoId}`);
  }

  rl.close();
  console.log("\nConcluído. Revise app/src/main/assets/catalog.json antes de commitar.");
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
