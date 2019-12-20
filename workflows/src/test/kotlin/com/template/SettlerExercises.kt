package com.template

import com.r3.corda.finance.obligation.contracts.commands.ObligationCommands
import com.r3.corda.finance.obligation.contracts.flows.AbstractSendToSettlementOracle
import com.r3.corda.finance.obligation.contracts.states.Obligation
import com.r3.corda.finance.obligation.contracts.types.PaymentStatus
import com.r3.corda.finance.obligation.contracts.types.SettlementMethod
import com.r3.corda.finance.obligation.oracle.flows.VerifySettlement
import com.r3.corda.finance.obligation.workflows.flows.NovateObligation
import com.r3.corda.finance.obligation.workflows.flows.OffLedgerSettleObligation
import com.r3.corda.finance.obligation.workflows.flows.SendToSettlementOracle
import com.r3.corda.finance.obligation.workflows.flows.UpdateSettlementMethod
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.template.flows.*
import com.template.states.IOUState
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toLtxDjvmInternal
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import kotlin.test.assertEquals
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker


class SettlerExercises {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows"),
            TestCordapp.findCordapp("com.r3.corda.finance.obligation.workflows.flows"),
            TestCordapp.findCordapp("com.r3.corda.finance.obligation.oracle.flows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode(CordaX500Name("ExchangeRateOracleService", "New York", "US"))
    private val d = network.createNode(CordaX500Name("SettlerOracleService", "New York", "US"))


    init {
        listOf(a, b, c, d).forEach {
            it.registerInitiatedFlow(QueryHandler::class.java)
            it.registerInitiatedFlow(SignHandler::class.java)
            it.registerInitiatedFlow(VerifySettlement::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    /**
     * TODO: Update IOUState to subclass the Obligation state from the Settler CorDapp in order
     * to allow integration of Settler with our IOU applicaton.
     * Hint:
     *  -
     */
    @Test
    fun testIOUStateExtendsSettlerObligation() {
        assert(Obligation::class.java.isAssignableFrom(IOUState::class.java))
    }

    /**
     * TODO: Implement the [CordaSettlerNovateIOUFlow] to novate the IOU in terms of the settlement currency.
     * Hint:
     * - We're going to leverage the [NovateObligation.Intiator] subflow from the Settler CorDapp.
     * - First, get the oracle identity from the network map cache using the [getPeerByLegalName] method.
     * - Next, we'll get the exchange rate by querying our Exchange Rate Oracle.
     * -- Subflow the [QueryExchangeRate] which returns an exchange rate [Double].
     * - Finally, subflow the [NovateObligation.Initiator] flow.
     * -- This flow takes the linearId of the obligation to novate and an [ObligationCommands.Novate] command.
     * -- In this case we'll use the [ObligationCommands.Novate.UpdateFaceAmountToken]
     * -- The UpdateFaceAmountToken command takes 4 parameters:
     * --- The original token (Our [IOUToken])
     * --- The new token to novate with (We can use [FiatCurrency.getInstance] utility method),
     * --- Our oracle [Party],
     * --- The exchange rate [Double] we got from the Oracle.
     */
    @Test
    fun testCordaSettlerNovateIOUFlow() {
        // 1. Create obligation
        val iou = IOUState(
                Amount(50, IOUToken("CUSTOM_TOKEN", 0)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        val createIouFuture = b.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = createIouFuture.getOrThrow()
        val issuedIou = result.tx.outputStates.get(0) as IOUState

        // 2. Novate
        val novateFuture = b.startFlow(CordaSettlerNovateIOUFlow(issuedIou))
        network.runNetwork()
        val resultFromNovate = novateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        assertEquals(resultFromNovate.faceAmount, Amount(7500, TokenType("USD", 2)));
    }

    /**
     * TODO: Implement the [CordaSettlerUpdateSettlementMethodFlow] to set the settlement method to our payment rail.
     * Hint:
     * - We're going to leverage the [UpdateSettlementMethod.Intiator] subflow from the Settler CorDapp.
     * - First, get the oracle identity from the network map cache using the [getPeerByLegalName] method.
     * - Next, we subflow the [UpdateSettlementMethod.Intiator] flow.
     * -- This subflow takes 2 parameters:
     * --- The linear ID of our obligation to update the settlment method of.
     * --- An implementation of the [SettlementMethod] interface from the Settler CorDapp.
     * ---- In our case we'll use our [BankApiSettlement] implementation.
     */
    @Test
    fun testCordaSettlerUpdateSettlementMethodFlow() {
        // 1. Create obligation
        val iou = IOUState(
                Amount(50, IOUToken("CUSTOM_TOKEN", 0)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        val createIouFuture = b.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = createIouFuture.getOrThrow()
        val issuedIou = result.tx.outputStates.get(0) as IOUState

        // 2. Novate
        val novateFuture = a.startFlow(CordaSettlerNovateIOUFlow(issuedIou))
        network.runNetwork()
        val novatedIOU = novateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        val updateFuture = a.startFlow(CordaSettlerUpdateSettlementMethodFlow(novatedIOU, "ABCD1234"))
        network.runNetwork()
        val resultFromUpdate = updateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        assertEquals(resultFromUpdate.settlementMethod!!::class.java, BankApiSettlement::class.java);
    }

    /**
     * TODO: Update [VerifySettlement] flow from Settler CorDapp to include our Bank API payment rail.
     * Hint:
     * - We need to update the VerifySettlement flow to handle the case of receiving our custom payment
     * - rail as a SettlementMethod.
     * - In order to do this, check in the [call] function at the code labeled as step 4 in the comments.
     * -- There you'll find a when/is clause which you'll need to add a case for our [BankApiSettlement]
     * - Then we can implement a custom function to specify the conditions for a BankApiSettlement to be verified.
     * -- We'll use the methods from the [BankApiOracleService].
     * -- Check the [VerifySettlement.verifySwiftSettlement] method for inspiration.
     * -- For our case, if the [BankApiOracleService.checkObligeeReceivedPayment] and
     * [BankApiOracleService.hasPaymentSettled] return true, then we can consider the payment as confirmed.
     * --- Hint: You may need to modify these methods to ensure that they return the correct result.
     *
     */
    @Test
    fun testUpdateVerifySettlementForBankApiSettlement() {
        // 1. Create obligation
        val iou = IOUState(
                Amount(50, IOUToken("CUSTOM_TOKEN", 2)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        val createIouFuture = b.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = createIouFuture.getOrThrow()
        val issuedIou = result.tx.outputStates.get(0) as IOUState

        // 2. Novate
        val novateFuture = a.startFlow(CordaSettlerNovateIOUFlow(issuedIou))
        network.runNetwork()
        val novatedIOU = novateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        val updateFuture = a.startFlow(CordaSettlerUpdateSettlementMethodFlow(novatedIOU, "ABCD1234"))
        network.runNetwork()
        val resultUpdate = updateFuture.getOrThrow().toLedgerTransaction(a.services)
        val resultFromUpdate = resultUpdate.outputStates.get(0) as Obligation<TokenType>
        val stateRef = resultUpdate.outRef<Obligation<TokenType>>(0)

        val tx = b.startFlow(
                MakeBankApiPayment(Amount(75, FiatCurrency.getInstance("USD")),
                        stateRef, resultFromUpdate.settlementMethod as BankApiSettlement))
        network.runNetwork()

        val oracleFuture = b.startFlow(SendToSettlementOracle(resultFromUpdate.linearId))
        network.runNetwork()
        val oracleResult = oracleFuture.getOrThrow()
    }

    /**
     * TODO: Implement the [CordaSettlerBankApiSettlement] Flow which will make the off-ledger payment and settle the IOU.
     * Hint:
     * - This flow will use our custom [MakeBankApiPayment] implementation of the
     * [MakeOffLedgerPayment] flow from Settler CorDapp.
     * - All our [CordaSettlerBankApiSettlement] flow needs to do is subflow the [OffLedgerSettleObligation.Initiator]
     * flow from the Settler CorDapp with our novated IOU.
     * -- This flow take as parameter two property:
     * --- The [Amount] of our IOU in the novated currency.
     * --- The [UniqueIdentifier] linear ID of our IOU to settle.
     * -- The Settler CorDapp and our Settlement Oracle will do the rest!
     */
    @Test
    fun testCordaSettlerBankApiSettlement() {
        // 1. Create obligation
        val iou = IOUState(
                Amount(50, IOUToken("CUSTOM_TOKEN", 0)),
                a.info.legalIdentities.get(0),
                b.info.legalIdentities.get(0))
        val createIouFuture = b.startFlow(IOUIssueFlow(iou))
        network.runNetwork()
        val result = createIouFuture.getOrThrow()
        val issuedIou = result.tx.outputStates.get(0) as IOUState

        // 2. Novate
        val novateFuture = a.startFlow(CordaSettlerNovateIOUFlow(issuedIou))
        network.runNetwork()
        val novatedIOU = novateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        // Update Settlement Terms
        val updateFuture = a.startFlow(CordaSettlerUpdateSettlementMethodFlow(novatedIOU, "ABCD1234"))
        network.runNetwork()
        val resultUpdate = updateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        // Settle
        val updateSettle = b.startFlow(CordaSettlerBankApiSettlement(resultUpdate))
        network.runNetwork()
        val resultFromSettle = updateFuture.getOrThrow().toLedgerTransaction(a.services).outputStates.get(0) as Obligation<TokenType>

        // no ouputs, 1 input
        // see that original IOU was settled. There should be no unconsumed IOU or Obligations in vault at this point
        b.transaction {
            assertEquals(0, b.services.vaultService.queryBy(IOUState::class.java).states.size)
            assertEquals(1, b.services.vaultService.queryBy(Obligation::class.java).states.size)
            assertEquals(PaymentStatus.SETTLED, b.services.vaultService.queryBy(Obligation::class.java).states.get(0).state.data.payments.get(0).status)
        }

    }
}