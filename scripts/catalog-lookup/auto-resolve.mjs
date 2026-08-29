#!/usr/bin/env node
/**
 * Resolução semi-automática de IDs de vídeo do YouTube para o catálogo do
 * Desafio Musical — variante do lookup.mjs pensada para lotes grandes.
 *
 * Diferente do lookup.mjs (que pede confirmação humana música por música),
 * este script busca candidatos via YouTube Data API v3 e usa uma heurística
 * de correspondência de texto (título + artista batendo com o resultado)
 * para decidir sozinho quando a confiança é alta. Toda decisão — inclusive
 * as automáticas — fica registrada em auto-resolve-report.json com link
 * direto pro vídeo escolhido, para revisão visual humana em lote depois.
 * Quando nenhum candidato bate com confiança suficiente, a música é deixada
 * como placeholder para resolução manual (nunca escolhe no escuro).
 *
 * Por padrão ignora a categoria GAMES (resolvida manualmente à parte, já
 * que o pedido de trilhas específicas de jogos merece conferência humana).
 *
 * Uso:
 *   YOUTUBE_API_KEY=sua_chave node scripts/catalog-lookup/auto-resolve.mjs
 * ou usando o mesmo scripts/catalog-lookup/.env do lookup.mjs.
 *
 * Flags opcionais:
 *   --include-games   também tenta resolver GAMES automaticamente
 *   --dry-run         não grava no catalog.json, só gera o relatório
 *   --limit=N         processa só as N primeiras pendentes (útil pra testar
 *                      a heurística gastando pouca cota da API antes do lote todo)
 */

import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CATALOG_PATH = path.resolve(__dirname, "../../app/src/main/assets/catalog.json");
const REPORT_PATH = path.resolve(__dirname, "auto-resolve-report.json");
const PLACEHOLDER_PREFIX = "REPLACE_WITH_YOUTUBE_ID__";

const args = process.argv.slice(2);
const includeGames = args.includes("--include-games");
const dryRun = args.includes("--dry-run");
const limitArg = args.find((a) => a.startsWith("--limit="));
const limit = limitArg ? parseInt(limitArg.split("=")[1], 10) : null;

// Confiança mínima pra aceitar automaticamente: título bate (peso 2) +
// pelo menos um sinal de artista (peso 1) = 3. Abaixo disso, fica pendente
// pra decisão manual em vez de arriscar o áudio errado.
const AUTO_ACCEPT_THRESHOLD = 3;

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

function normalize(text) {
  return text
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "") // remove acentos
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function significantWords(text) {
  const stop = new Set(["the", "a", "an", "de", "da", "do", "e", "and", "of", "&"]);
  return normalize(text)
    .split(" ")
    .filter((w) => w.length > 1 && !stop.has(w));
}

function scoreCandidate(song, candidate) {
  const titleWords = significantWords(song.title);
  const candTitleNorm = normalize(candidate.title);
  const candChannelNorm = normalize(candidate.channelTitle);

  const titleWordsMatched = titleWords.filter((w) => candTitleNorm.includes(w)).length;
  const titleMatchRatio = titleWords.length > 0 ? titleWordsMatched / titleWords.length : 0;

  // Primeiro artista citado (antes de "&", "feat", "," etc.) — o mais
  // provável de aparecer no título/canal do vídeo oficial.
  const primaryArtist = song.artist.split(/&|feat\.?|,|\bft\.?\b/i)[0].trim();
  const artistWords = significantWords(primaryArtist);
  const artistInTitle = artistWords.length > 0 && artistWords.every((w) => candTitleNorm.includes(w));
  const artistInChannel = artistWords.length > 0 && artistWords.some((w) => candChannelNorm.includes(w));

  let score = 0;
  if (titleMatchRatio >= 0.6) score += 2;
  else if (titleMatchRatio >= 0.4) score += 1;
  if (artistInTitle) score += 1;
  if (artistInChannel) score += 1;
  if (candChannelNorm.includes("topic")) score += 1; // canal "Artista - Topic" = auto-gerado pelo YouTube Music, geralmente confiável
  if (candChannelNorm.includes("vevo")) score += 0.5;

  return score;
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
        "Rode com: YOUTUBE_API_KEY=sua_chave node scripts/catalog-lookup/auto-resolve.mjs\n" +
        "ou use o scripts/catalog-lookup/.env já existente."
    );
    process.exit(1);
  }

  const catalog = JSON.parse(await readFile(CATALOG_PATH, "utf-8"));
  let pending = catalog.filter((song) => {
    if (!song.youtubeVideoId.startsWith(PLACEHOLDER_PREFIX)) return false;
    if (!includeGames && song.category === "GAMES") return false;
    return true;
  });

  if (pending.length === 0) {
    console.log("Nenhuma música pendente pra resolver automaticamente.");
    return;
  }

  if (limit && limit > 0) {
    pending = pending.slice(0, limit);
  }

  console.log(`${pending.length} música(s) para resolução automática${dryRun ? " (dry-run, nada será salvo no catálogo)" : ""}.\n`);

  const report = { generatedAt: new Date().toISOString(), autoAccepted: [], needsReview: [], errors: [] };
  let quotaExhausted = false;

  for (const song of pending) {
    if (quotaExhausted) {
      report.needsReview.push({ id: song.id, title: song.title, artist: song.artist, category: song.category, reason: "Cota da API esgotada antes de processar esta música." });
      continue;
    }

    const query = `${song.artist} ${song.title}${song.work ? " " + song.work : ""}`;
    let candidates;
    try {
      candidates = await searchCandidates(apiKey, query);
    } catch (err) {
      if (/quotaExceeded/i.test(err.message)) {
        quotaExhausted = true;
        console.error(`\nCota da API do YouTube esgotada. Pare por aqui e retome amanhã (o script grava progresso, é só rodar de novo).`);
      } else {
        console.error(`Erro ao buscar "${query}": ${err.message}`);
      }
      report.errors.push({ id: song.id, title: song.title, artist: song.artist, error: err.message });
      continue;
    }

    if (candidates.length === 0) {
      report.needsReview.push({ id: song.id, title: song.title, artist: song.artist, category: song.category, reason: "Nenhum resultado encontrado na busca." });
      continue;
    }

    const scored = candidates
      .map((c) => ({ ...c, score: scoreCandidate(song, c), title: stripHtmlEntities(c.title) }))
      .sort((a, b) => b.score - a.score);

    const best = scored[0];
    const entry = {
      id: song.id,
      title: song.title,
      artist: song.artist,
      category: song.category,
      work: song.work,
      chosenVideoId: best.videoId,
      chosenVideoTitle: best.title,
      chosenChannel: best.channelTitle,
      confidenceScore: best.score,
      youtubeUrl: `https://www.youtube.com/watch?v=${best.videoId}`,
      thumbnailUrl: `https://i.ytimg.com/vi/${best.videoId}/mqdefault.jpg`,
      alternatives: scored.slice(1, 3).map((c) => ({ videoId: c.videoId, title: c.title, channelTitle: c.channelTitle, score: c.score })),
    };

    if (best.score >= AUTO_ACCEPT_THRESHOLD) {
      if (!dryRun) {
        song.youtubeVideoId = best.videoId;
      }
      report.autoAccepted.push(entry);
      console.log(`[OK ${best.score.toFixed(1)}] ${song.title} — ${song.artist} -> ${best.videoId} (${best.channelTitle})`);
    } else {
      entry.reason = `Confiança baixa (score ${best.score.toFixed(1)} < ${AUTO_ACCEPT_THRESHOLD}) — decidir manualmente.`;
      report.needsReview.push(entry);
      console.log(`[REVISAR ${best.score.toFixed(1)}] ${song.title} — ${song.artist}`);
    }
  }

  if (!dryRun) {
    await writeFile(CATALOG_PATH, JSON.stringify(catalog, null, 2) + "\n", "utf-8");
  }
  await writeFile(REPORT_PATH, JSON.stringify(report, null, 2) + "\n", "utf-8");

  console.log(`\nConcluído: ${report.autoAccepted.length} aceitas automaticamente, ${report.needsReview.length} precisam de revisão manual, ${report.errors.length} erro(s).`);
  console.log(`Relatório completo em: ${REPORT_PATH}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
