package com.template

import com.template.contracts.IOUContract
import com.template.flows.ExchangeRateOracleFlow
import com.template.flows.ExchangeRateOracleService
import com.template.flows.QueryHandler
import com.template.flows.SignHandler
import com.template.states.IOUToken
import com.template.states.IOUTokenState
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.TransactionBuilder
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AdvancedFlowConceptsExercises {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode(CordaX500Name("ExchangeRateOracleService", "New York", "US"))

    init {
        listOf(a, b, c).forEach {
            it.registerInitiatedFlow(QueryHandler::class.java)
            it.registerInitiatedFlow(SignHandler::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    /**
     * TODO: Implement the [createFilteredTransaction] method in [ExchangeRateOracleFlow].
     * Hint:
     * - Use the [buildFilteredTransaction] from the [SignedTransaction] argument passed into the method.
     * - [buildFilteredTransaction] takes a [Predicate] lambda argument.
     * -- Within the [Predicate] a simple when/is block allows you to filter to only the transaction components
     * that you want to add.
     * -- In our case, we only want the Oracle to see Command of which the Oracle is a required signer of.
     * -- Example usage:
     *      Predicate {
     *          when (it) {
     *              is Command<*> -> /* boolean expression */
     *              else -> false
     *          }
     *      }
     */
    @Test
    fun `filtered transaction`() {
        val notary = a.services.networkMapCache.notaryIdentities.get(0)
        val builder = TransactionBuilder(notary = notary)
        val output = IOUTokenState(
                Amount(3, IOUToken("CUSTOM_TOKEN", 2)),
                a.services.myInfo.legalIdentities.get(0),
                b.services.myInfo.legalIdentities.get(0))
        builder.addCommand(
                IOUContract.Commands.Exchange("USD", 1.25),
                listOf(c.info.legalIdentities.get(0).owningKey, a.info.legalIdentities.get(0).owningKey))
        builder.addCommand(
                IOUContract.Commands.Exchange("USD", 1.75),
                listOf(a.info.legalIdentities.get(0).owningKey))
        builder.addOutputState(output, IOUContract.IOU_CONTRACT_ID)
        val ptx = a.services.signInitialTransaction(builder)

        val flow = ExchangeRateOracleFlow(ptx)
        val ftx = flow.createFilteredTransaction(c.info.legalIdentities[0], ptx)

        assert(ftx != null)
        assertEquals(1, ftx!!.commands.size)
        assertEquals(1.25, (ftx.commands[0].value as IOUContract.Commands.Exchange).rate)
    }
}