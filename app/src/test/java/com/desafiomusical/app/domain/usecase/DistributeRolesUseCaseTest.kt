package com.desafiomusical.app.domain.usecase

import com.desafiomusical.app.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DistributeRolesUseCaseTest {
    private val useCase = DistributeRolesUseCase()

    private fun players(vararg names: String) =
        names.mapIndexed { index, name ->
            Player(id = "p$index", name = name, createdAt = 0L)
        }

    @Test
    fun `escolhedor nunca e o respondente da propria rodada`() {
        val setups = useCase(players("André", "Maria", "João", "Ana"), roundCount = 8)
        setups.forEach { assertNotEquals(it.chooserId, it.mainResponderId) }
    }

    @Test
    fun `distribuicao de 4 jogadores segue o exemplo da especificacao`() {
        val (andre, maria, joao, ana) = players("André", "Maria", "João", "Ana")
        val setups = useCase(listOf(andre, maria, joao, ana), roundCount = 4)

        assertEquals(andre.id, setups[0].chooserId)
        assertEquals(maria.id, setups[0].mainResponderId)

        assertEquals(maria.id, setups[1].chooserId)
        assertEquals(joao.id, setups[1].mainResponderId)

        assertEquals(joao.id, setups[2].chooserId)
        assertEquals(ana.id, setups[2].mainResponderId)

        assertEquals(ana.id, setups[3].chooserId)
        assertEquals(andre.id, setups[3].mainResponderId)
    }

    @Test
    fun `cada jogador escolhe um numero equilibrado de vezes`() {
        val setups = useCase(players("A", "B", "C", "D"), roundCount = 20)
        val chooserCounts = setups.groupingBy { it.chooserId }.eachCount()
        assertEquals(setOf(5), chooserCounts.values.toSet())
    }

    @Test
    fun `jogadores elegiveis a roubo excluem escolhedor e respondente`() {
        val setups = useCase(players("A", "B", "C", "D"), roundCount = 1)
        val setup = setups.first()
        assert(setup.chooserId !in setup.eligibleStealerIds)
        assert(setup.mainResponderId !in setup.eligibleStealerIds)
        assertEquals(2, setup.eligibleStealerIds.size)
    }
}
