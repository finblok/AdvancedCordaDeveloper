package com.template

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.flows.IOUIssueFlow
import com.template.states.IOUState
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.Vault
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import org.junit.Test
import rx.Observable
import java.util.concurrent.Future
import kotlin.test.assertEquals

class DriverBasedTest {
    private val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    private val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test
    fun `node test`() = withDriver {
        // Start a pair of nodes and wait for them both to be ready.
        val (partyAHandle, partyBHandle) = startNodes(bankA, bankB)

        // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
        // nodes have started and can communicate.

        // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
        // and other important metrics to ensure that your CorDapp is working as intended.
        assertEquals(bankB.name, partyAHandle.resolveName(bankB.name))
        assertEquals(bankA.name, partyBHandle.resolveName(bankA.name))

        val bobVaultUpdates: Observable<Vault.Update<IOUState>> = partyAHandle.rpc.vaultTrackBy<IOUState>().updates

        val token = IOUState(Amount(50, IOUToken("IOU_TOKEN", 2)),
                partyAHandle.rpc.wellKnownPartyFromX500Name(bankA.name)!!, partyAHandle.rpc.wellKnownPartyFromX500Name(bankB.name)!!)


        partyBHandle.rpc.startFlow(::IOUIssueFlow, token).returnValue.getOrThrow()

        bobVaultUpdates.expectEvents {
            expect { update ->
                println("Bob got vault update of $update")
                val amount: Amount<TokenType> = update.produced.first().state.data.amount
                assertEquals(51, amount.quantity)
            }
        }

    }

    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true)
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