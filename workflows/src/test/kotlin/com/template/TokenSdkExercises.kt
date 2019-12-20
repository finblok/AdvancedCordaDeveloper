package com.template

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.template.flows.DeliveryVersusPaymentTokenFlow
import com.template.flows.IOUTokenIssueFlow
import com.template.states.*
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import kotlin.test.assertEquals

class TokenSdkExercises {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts")
    )))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()

    init {
        listOf(a, b, c).forEach {}
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    /**
     * TODO: Implement [ExampleFixedToken].
     * Hint:
     * - Fixed Tokens need to extend the [TokenType] class from Token SDK.
     * - [TokenType] classes need to override two fields:
     * -- [tokenIdentifier], String identifying the token, used in contracts to token states are of the same type.
     * -- [fractionDigits], Int defining the number of decimal digits (ex. 2 => 10.00)
     */
    @Test
    fun testCreateFixedToken() {
        assert(TokenType::class.java.isAssignableFrom(ExampleFixedToken::class.java))
    }

    /**
     * TODO: Implement [ExampleEvolvableToken].
     * Hint:
     * - Evolvable Tokens need to extend the [EvolvableTokenType] class from Token SDK.
     * - [EvolvableTokenType] classes need to override three fields:
     * -- [maintainers], List<Party> which specifies which parties will be notified when the token is updated.
     * -- [fractionDigits], Int defining the number of decimal digits (ex. 2 => 10.00).
     * -- [linearId] - remember that Evolvable Tokens have a linearId since they can evolve over time.
     * - In addition to these fields, we can have any number of additional fields like any other [LinearState].
     */
    @Test
    fun testCreateEvolvableToken() {
        assert(EvolvableTokenType::class.java.isAssignableFrom(ExampleEvolvableToken::class.java))
    }

    /**
     * TODO: Implement [createNonFungibleFixedToken] method.
     * Hint:
     * - Tokens can be Fixed versus Evolvable, but also Fungible versus Non-fungible.
     * - For our method, we want to return a [NonFungibleToken] object containing an [ExampleFixedToken]
     * - [NonFungibleToken] takes 3 parameters:
     * -- an [IssuedTokenType] instance, a token holder [Party], and a linearId to identify this [NonFungibleToken].
     * -- [IssuedTokenType] is a wrapper object that pairs a [TokenType] with a issuer [Party].
     */
    @Test
    fun testCreateNonFungibleFixedToken() {
        val issuer = b.info.legalIdentities.get(0);
        val holder = a.info.legalIdentities.get(0);
        val result = createNonFungibleFixedToken(issuer, holder)
        assert(NonFungibleToken::class.java.isAssignableFrom(result!!::class.java))
        assertEquals(ExampleFixedToken::class.java, result.tokenType.tokenClass)
    }

    /**
     * TODO: Implement [createNonFungibleEvolvableToken] method.
     * Hint:
     * - Now we want to create a [NonFungibleToken] containing an [ExampleEvolvableToken]
     * - This will be the same as in the previous exercise, except that since EvolvableTokens use
     * a [TokenPointer] to pair the evolvable token data [LinearState] with the actual [TokenType].
     * - Our [IssuedTokenType] will wrap our [TokenPointer] with the issuer [Party].
     * - The [TokenPointer] is a [TokenType] implementation that wraps a [LinearPointer] with a
     * displayTokenSize which will be the [fractionDigits] from our [ExampleEvolvableToken].
     * - The [LinearPointer] takes as parameter the linear Id of our [ExampleEvolvableToken]
     * and the class definition for the token type.
     * -- Hint: You can use the [ExampleEvolvableToken::class.java] notation.
     */
    @Test
    fun testCreateNonFungibleEvolvableToken() {
        val issuer = b.info.legalIdentities.get(0);
        val holder = a.info.legalIdentities.get(0);
        val result = createNonFungibleEvolvableToken(issuer, holder)
        assert(NonFungibleToken::class.java.isAssignableFrom(result!!::class.java))
        assertEquals(ExampleEvolvableToken::class.java, result.tokenType.tokenClass)
    }

    /**
     * TODO: Implement [createFungibleFixedToken] method.
     * Hint:
     * - Now we want to create a [FungibleToken] containing an [ExampleFixedToken]
     * - When creating a [FungibleToken] we need to supply an [Amount] of the [IssuedTokenType]
     * as well as the [Party] which is the owner of these tokens.
     */
    @Test
    fun testCreateFungibleFixedToken() {
        val issuer = b.info.legalIdentities.get(0);
        val holder = a.info.legalIdentities.get(0);
        val result = createFungibleFixedToken(issuer, holder, 1000)
        assert(FungibleToken::class.java.isAssignableFrom(result!!::class.java))
        assertEquals(ExampleFixedToken::class.java, result.tokenType.tokenClass)
        assertEquals(1000, result.amount.quantity)
    }

    /**
     * TODO: Implement [createFungibleEvolvableToken] method.
     * Hint:
     * - Now we want to create a [FungibleToken] containing an [ExampleEvolvableToken]
     * - Use the [createFungibleFixedToken] and [createNonFungibleEvolvableToken] methods as a guide here.
     */
    @Test
    fun testCreateFungibleEvolvableToken() {
        val issuer = b.info.legalIdentities.get(0);
        val holder = a.info.legalIdentities.get(0);
        val result = createFungibleEvolvableToken(issuer, holder, 1000)
        assert(FungibleToken::class.java.isAssignableFrom(result!!::class.java))
        assertEquals(ExampleEvolvableToken::class.java, result.tokenType.tokenClass)
        assertEquals(1000, result.amount.quantity)
    }

    /**
     * TODO: Convert IOUState to be in terms of an Amount of Token SDK Tokens rather than Currency
     * Hint:
     *  -
     */
    @Test
    fun hasIOUAmountFieldOfCorrectType() {
        // Does the amount field exist?
        val field = IOUState::class.java.getDeclaredField("amount")

        // Is the amount field of the correct type?
        assertEquals(field.type, Amount::class.java)

        // Does the amount field have the correct paramerized type?
        val signature = (field.genericType as ParameterizedTypeImpl).actualTypeArguments[0]
        assertEquals(signature, TokenType::class.java)
    }

    /**
     * TODO: Implement [IOUTokenIssueFlow].
     * Hint:
     * - Now we know how to create and instantiate Fixed/Evolvable Fungible/Non-fungible Tokens,
     * the next step is to actually ISSUE them on the ledger as immutable facts.
     * - To do this we will implement the [IOUTokenIssueFlow] which will be a simple flow
     * that will create and issue specified amount of FungibleTokens using our fixed IOUToken.
     * - Once we have created the [FungibleToken] object, we subFlow the [IssueTokens] Flow from
     * the Token SDK.
     * - The [IssueTokens] Flow simply takes as argument a list containing the [FungibleToken]s to
     * issue. In our case this will be a list containing our single [FungibleToken] instance.
     * -- You can use the [listOf] Kotlin command to easily make a list.
     */
    @Test
    fun implementIOUTokenIssueFlow() {
        val future = b.startFlow(IOUTokenIssueFlow(25))
        network.runNetwork()
        val stx = future.getOrThrow()

        assertEquals(stx.tx.outputStates.size, 1)
        assertEquals((stx.tx.outputStates.get(0) as FungibleToken).amount.quantity, 25)
    }

    /**
     * TODO: Implement [DeliveryVersusPaymentTokenFlow].
     * Hint:
     * - This flow will implement a simple delivery versus payment use case to exchange two different
     * token types between two parties in an atomic transaction.
     * - First, we'll need to create a [TransactionBuilder] with a [Notary] identity.
     * - Then, we need to create a [FungibleToken] of some amount of the [ExampleFixedToken]
     * in the [ourPayment] parameter.
     * - Then, we need to add a Move command to our TransactionBuilder for both the payment and
     * counter party asset.
     * -- Here, the [addMoveFungibleTokens] and [addMoveNonFungibleTokens] helper methods from
     * the Token SDK will come in handy.
     * -- Control+Click the above helper methods to see the usage.
     * - Finally, to get our unit test passing, use the [serviceHub] to sign the initial transaction
     * and return the partially signed transaction.
     */
    @Test
    fun implementDeliveryVersusPaymentFlow() {
        val partyA = a.info.legalIdentities.get(0);
        val partyB = b.info.legalIdentities.get(0)
        val fungibleToken = createFungibleFixedToken(partyA, partyA, 1000)!!
        val nonFungibleToken = createNonFungibleFixedToken(partyB, partyB)!!

        val fungibleFuture = a.startFlow(IssueTokensFlow(fungibleToken))
        network.runNetwork()
        val stx = fungibleFuture.getOrThrow()
        assertEquals(1, stx.tx.outputStates.size)

        val nonFungibleFuture = b.startFlow(IssueTokens(listOf(nonFungibleToken), listOf(partyA)))
        network.runNetwork()
        val stx2 = nonFungibleFuture.getOrThrow()
        assertEquals(1, stx2.tx.outputStates.size)

        val states = a.services.vaultService.queryBy(ContractState::class.java).states

        val dvpFuture = a.startFlow(DeliveryVersusPaymentTokenFlow(
                ExampleFixedToken("CUSTOMTOKEN", 2),
                ExampleFixedToken("CUSTOMTOKEN", 2),
                partyB
        ))
        network.runNetwork()
        val stx3 = dvpFuture.getOrThrow()
        assertEquals(2, stx3.tx.outputStates.size)
        assertEquals(1, stx3.tx.toLedgerTransaction(a.services).outputsOfType(FungibleToken::class.java).size)
        assertEquals(1, stx3.tx.toLedgerTransaction(a.services).outputsOfType(NonFungibleToken::class.java).size)
        var f = stx3.tx.toLedgerTransaction(a.services).outputsOfType(FungibleToken::class.java).get(0)
        var nf = stx3.tx.toLedgerTransaction(a.services).outputsOfType(NonFungibleToken::class.java).get(0)
        assertEquals(partyB, f.holder)
        assertEquals(partyA, nf.holder)
    }
}