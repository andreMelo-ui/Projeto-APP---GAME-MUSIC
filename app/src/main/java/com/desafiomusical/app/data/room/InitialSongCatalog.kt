package com.desafiomusical.app.data.room

import com.desafiomusical.app.data.room.entity.SongEntity

/**
 * Catálogo inicial de teste, usado para popular o banco na primeira execução
 * do app (ver [DesafioMusicalDatabase]). Os YouTube video IDs vêm como
 * placeholder — devem ser substituídos por IDs reais e oficiais antes do
 * lançamento (ver seção "Catálogo" do README).
 */
object InitialSongCatalog {

    private const val PLACEHOLDER_PREFIX = "REPLACE_WITH_YOUTUBE_ID__"

    val songs: List<SongEntity> by lazy {
        val now = System.currentTimeMillis()
        listOf(
            SongEntity(
                id = "song-brasileira-001",
                title = "Águas de Março",
                artist = "Elis Regina & Tom Jobim",
                category = "BRASILEIRA",
                work = null,
                difficulty = "MEDIO",
                youtubeVideoId = PLACEHOLDER_PREFIX + "AGUAS_DE_MARCO",
                hint1 = "Uma das duplas mais icônicas da MPB.",
                hint2 = "A letra é uma enumeração de pequenas coisas do cotidiano.",
                hint3 = "O título remete ao fim do verão e às chuvas no Brasil.",
                tagsCsv = "mpb,bossa_nova,classico",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-brasileira-002",
                title = "Mas Que Nada",
                artist = "Jorge Ben Jor",
                category = "BRASILEIRA",
                work = null,
                difficulty = "FACIL",
                youtubeVideoId = PLACEHOLDER_PREFIX + "MAS_QUE_NADA",
                hint1 = "Uma das canções brasileiras mais regravadas no mundo.",
                hint2 = "Tem uma versão internacional com o grupo Black Eyed Peas.",
                hint3 = "O refrão repete o próprio título da música.",
                tagsCsv = "samba,classico",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-internacional-001",
                title = "Billie Jean",
                artist = "Michael Jackson",
                category = "INTERNACIONAL",
                work = "Thriller",
                difficulty = "FACIL",
                youtubeVideoId = PLACEHOLDER_PREFIX + "BILLIE_JEAN",
                hint1 = "O artista é conhecido como o 'Rei do Pop'.",
                hint2 = "A faixa tem uma das linhas de baixo mais famosas da história.",
                hint3 = "O clipe tornou o piso iluminado embaixo dos pés um ícone.",
                tagsCsv = "pop,anos_80,classico",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-internacional-002",
                title = "Shape of You",
                artist = "Ed Sheeran",
                category = "INTERNACIONAL",
                work = "÷ (Divide)",
                difficulty = "FACIL",
                youtubeVideoId = PLACEHOLDER_PREFIX + "SHAPE_OF_YOU",
                hint1 = "Um dos maiores sucessos pop da década de 2010.",
                hint2 = "O artista é britânico e costuma se apresentar só com violão.",
                hint3 = "O título fala sobre a 'forma' de alguém.",
                tagsCsv = "pop,anos_2010",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-popular-001",
                title = "Someone Like You",
                artist = "Adele",
                category = "POPULAR",
                work = "21",
                difficulty = "MEDIO",
                youtubeVideoId = PLACEHOLDER_PREFIX + "SOMEONE_LIKE_YOU",
                hint1 = "A cantora britânica venceu diversos prêmios Grammy.",
                hint2 = "A canção fala sobre reencontrar um antigo amor.",
                hint3 = "É uma balada tocada ao piano.",
                tagsCsv = "pop,balada",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-popular-002",
                title = "Uptown Funk",
                artist = "Mark Ronson feat. Bruno Mars",
                category = "POPULAR",
                work = null,
                difficulty = "FACIL",
                youtubeVideoId = PLACEHOLDER_PREFIX + "UPTOWN_FUNK",
                hint1 = "A faixa mistura funk, soul e pop.",
                hint2 = "O clipe é gravado em preto e branco.",
                hint3 = "O refrão tem a expressão 'don't believe me, just watch'.",
                tagsCsv = "funk,pop",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-games-001",
                title = "Megalovania",
                artist = "Toby Fox",
                category = "GAMES",
                work = "Undertale",
                difficulty = "MEDIO",
                youtubeVideoId = PLACEHOLDER_PREFIX + "MEGALOVANIA",
                hint1 = "Trilha de um RPG independente lançado em 2015.",
                hint2 = "Toca durante o confronto contra um esqueleto muito forte.",
                hint3 = "Uma das trilhas de jogos mais remixadas da internet.",
                tagsCsv = "rpg,indie,chiptune",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-games-002",
                title = "Zelda's Lullaby",
                artist = "Koji Kondo",
                category = "GAMES",
                work = "The Legend of Zelda: Ocarina of Time",
                difficulty = "DIFICIL",
                youtubeVideoId = PLACEHOLDER_PREFIX + "ZELDAS_LULLABY",
                hint1 = "Trilha de uma franquia de aventura e exploração.",
                hint2 = "É tocada com um instrumento de sopro dentro do próprio jogo.",
                hint3 = "Leva o nome de uma princesa élfica.",
                tagsCsv = "aventura,nintendo,instrumental",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-personagens-001",
                title = "Hakuna Matata",
                artist = "Elton John & Tim Rice",
                category = "PERSONAGENS",
                work = "O Rei Leão",
                difficulty = "FACIL",
                youtubeVideoId = PLACEHOLDER_PREFIX + "HAKUNA_MATATA",
                hint1 = "A frase-título é uma expressão em suaíli.",
                hint2 = "É cantada por um javali e um suricato.",
                hint3 = "Significa 'sem problemas' e virou filosofia de vida na trama.",
                tagsCsv = "animacao,disney",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-personagens-002",
                title = "Batman Theme",
                artist = "Danny Elfman",
                category = "PERSONAGENS",
                work = "Batman (1989)",
                difficulty = "MEDIO",
                youtubeVideoId = PLACEHOLDER_PREFIX + "BATMAN_THEME",
                hint1 = "Tema instrumental de um herói vigilante de Gotham City.",
                hint2 = "O compositor também assina trilhas de vários filmes de Tim Burton.",
                hint3 = "O personagem também é conhecido como 'Cavaleiro das Trevas'.",
                tagsCsv = "cinema,heroi,instrumental",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-anime-001",
                title = "Gurenge",
                artist = "LiSA",
                category = "ANIME",
                work = "Demon Slayer",
                difficulty = "MEDIO",
                youtubeVideoId = PLACEHOLDER_PREFIX + "GURENGE",
                hint1 = "Abertura de um anime ambientado na era Taisho, no Japão.",
                hint2 = "O protagonista busca curar a irmã transformada em demônio.",
                hint3 = "A cantora também interpretou o tema 'Homura'.",
                tagsCsv = "anime,abertura,shonen",
                active = true,
                createdAt = now,
                updatedAt = now
            ),
            SongEntity(
                id = "song-anime-002",
                title = "Zankoku na Tenshi no These",
                artist = "Yoko Takahashi",
                category = "ANIME",
                work = "Neon Genesis Evangelion",
                difficulty = "DIFICIL",
                youtubeVideoId = PLACEHOLDER_PREFIX + "CRUEL_ANGEL_THESIS",
                hint1 = "Abertura de um clássico anime dos anos 90 sobre robôs gigantes.",
                hint2 = "O título em português é algo como 'A Tese do Anjo Cruel'.",
                hint3 = "É considerada uma das aberturas de anime mais famosas de todos os tempos.",
                tagsCsv = "anime,abertura,mecha,classico",
                active = true,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
