package com.template.webserver

import com.template.flows.IOUIssueFlow
import com.template.states.IOUState
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import org.junit.Test
import java.util.concurrent.Future
import kotlin.test.assertEquals


class ApiExercises {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    /**
     * TODO: Implement the [getIOUs] method of the RPC API [Controller]
     * Hint:
     * - Use the [vaultQuery] RPC method and parameterize by IOUState
     * - You can specify a query criteria or simply pass in a Class reference into the vaultQuery method
     * -- Indicate a class reference in kotlin using the [::class.java] syntax
     */
    @Test
    fun `vault query`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        val iou = IOUState(Amount(50, IOUToken("IOU_TOKEN", 2)),
                partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)

        partyBHandle.rpc.startFlow(::IOUIssueFlow, iou).returnValue.getOrThrow()

        val result = Controller(partyAHandle.rpc).getIOUs()!!
        assertEquals(1, result.size)
        assertEquals(50, result.get(0).state.data.amount.quantity)
    }

    /**
     * TODO: Implement the [getIousWithLinearId] method of the RPC API [Controller]
     * Hint:
     * - Use the [vaultQueryBy] RPC method using a LinearStateQueryCriteria
     * - First, we need to convert the String linear ID into a UniqueIdentifier.
     * -- Use the [fromString] static method from the [UniqueIdentifier] class
     * - Create a [QueryCriteria.LinearStateQueryCriteria] instance that will filter by our linear ID.
     * -- The LinearStateQueryCrtieria takes a list of UUID linear IDs as the second parameter
     * --- Use the [listOf] to create a list on the fly
     * --- You can leave all other constructor parameters null since we don't care to override the default
     * for those fields.
     * - Parameterize the [vaultQueryBy] method by IOUState,
     *      ex) vaultQueryBy<MyState>
     */
    @Test
    fun `vault query linear id`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        val iou = IOUState(Amount(50, IOUToken("IOU_TOKEN", 2)),
                partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)
        val iou2 = IOUState(Amount(51, IOUToken("IOU_TOKEN", 2)),
                partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)

        partyBHandle.rpc.startFlow(::IOUIssueFlow, iou).returnValue.getOrThrow()

        val result = Controller(partyAHandle.rpc).getIousWithLinearId(iou.linearId.toString())!!
        assertEquals(1, result.size)
        assertEquals(50, result.get(0).state.data.amount.quantity)
    }

    /**
     * TODO: Implement the [getIOUsWithAmountGreaterThan] method of the RPC API [Controller]
     * Hint:
     * - Use [vaultQueryBy] with a [VaultCustomQueryCriteria] within a [builder] DSL lambda block
     * - Create a lambda scope by using the [builder] function from [QueryCriteriaUtils]
     *    ex)
     *    builder {
     *      // create criteria using DSL helper functions (greatherThan, lessThan, sum, etc..)
     *      proxy.vaultQueryBy<IOUState>(myCriteria)
     *    }
     * - Within the builder block, create a [VaultCustomQueryCrtieria]
     * -- Pass in filter function as argument to VaultCustomQueryCritiera
     *    ex)
     *    VaultCustomQueryCriteria(
     *       PersistentCashState::pennies
     *      .greaterThanOrEqual(10L)
     *    )
     * - Call [vaultQueryBy] within the [builder] block passing in our custom criteria object
     *   ex) vaultQueryBy<IOUState>(criteria)queryBy<IOUState>(criteria)
     */
    @Test
    fun `vault query custom schema`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        partyBHandle.rpc.startFlow(::IOUIssueFlow,
                IOUState(Amount(49, IOUToken("IOU_TOKEN", 2)),
                partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)
        ).returnValue.getOrThrow()

        partyBHandle.rpc.startFlow(::IOUIssueFlow,
                IOUState(Amount(52, IOUToken("IOU_TOKEN", 2)),
                        partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)
        ).returnValue.getOrThrow()

        val result = Controller(partyAHandle.rpc).getIOUsWithAmountGreaterThan(50)!!
        assertEquals(1, result.size)
        assertEquals(52, result.get(0).state.data.amount.quantity)
    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true, cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows")))
    ) { test() }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()
}