package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.money.USD
import com.template.contracts.IOUContract
import com.template.states.IOUState
import com.template.states.IOUTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.util.*
import java.util.function.Predicate

@InitiatingFlow
@StartableByRPC
class ExchangeRateOracleFlow(val ptx: SignedTransaction) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    fun createFilteredTransaction(oracle: Party, stx: SignedTransaction): FilteredTransaction? {
        return null
    }

    @Suspendable
    override fun call(): SignedTransaction {
        val oracleName = CordaX500Name("ExchangeRateOracleService", "New York","US")

        // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
        return serviceHub.signInitialTransaction(
                TransactionBuilder(notary = null)
        )
    }
}

@InitiatingFlow
class QueryExchangeRate(val oracle: Party, val currencyCode: String) : FlowLogic<Double>() {
    @Suspendable
    override fun call(): Double {
        return initiateFlow(oracle).sendAndReceive<Double>(currencyCode).unwrap { it }
    }
}

@InitiatedBy(QueryExchangeRate::class)
class QueryHandler(val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val request = session.receive<String>().unwrap { it }
        val response = serviceHub.cordaService(ExchangeRateOracleService::class.java).query(request)
        session.send(response)
    }
}

@InitiatingFlow
class SignExchangeRate(val oracle: Party, val ftx: FilteredTransaction) : FlowLogic<TransactionSignature>() {
    @Suspendable override fun call(): TransactionSignature {
        val session = initiateFlow(oracle)
        return session.sendAndReceive<TransactionSignature>(ftx).unwrap { it }
    }
}

@InitiatedBy(SignExchangeRate::class)
class SignHandler(val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val request = session.receive<FilteredTransaction>().unwrap { it }
        val response = serviceHub.cordaService(ExchangeRateOracleService::class.java).sign(request)
        session.send(response)
    }
}

@CordaService
class ExchangeRateOracleService(val services: ServiceHub) : SingletonSerializeAsToken() {
    private val myKey = services.myInfo.legalIdentities.first().owningKey
    // Placeholder code to avoid type error when running the tests.
    fun query(arg: Any): Any { return 0; }
    fun sign(arg: Any): Any { return 0; }
}