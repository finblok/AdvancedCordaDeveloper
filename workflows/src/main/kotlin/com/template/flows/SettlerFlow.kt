package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.obligation.contracts.commands.ObligationCommands
import com.r3.corda.finance.obligation.contracts.states.Obligation
import com.r3.corda.finance.obligation.contracts.types.OffLedgerPayment
import com.r3.corda.finance.obligation.contracts.types.Payment
import com.r3.corda.finance.obligation.contracts.types.PaymentReference
import com.r3.corda.finance.obligation.contracts.types.PaymentStatus
import com.r3.corda.finance.obligation.oracle.flows.VerifySettlement
import com.r3.corda.finance.obligation.workflows.flows.MakeOffLedgerPayment
import com.r3.corda.finance.obligation.workflows.flows.NovateObligation
import com.r3.corda.finance.obligation.workflows.flows.OffLedgerSettleObligation
import com.r3.corda.finance.obligation.workflows.flows.UpdateSettlementMethod
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.utilities.toParty
import com.template.states.IOUState
import com.template.states.IOUToken
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker

class CordaSettlerNovateIOUFlow(
        val iou: IOUState
)  : FlowLogic<WireTransaction>() {

    @Suspendable
    override fun call(): WireTransaction {
        val oracleName = CordaX500Name("ExchangeRateOracleService", "New York","US")

        // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
        return serviceHub.signInitialTransaction(
                TransactionBuilder(notary = null)
        ).tx
    }
}

class CordaSettlerUpdateSettlementMethodFlow(
        val novatedIou: Obligation<TokenType>,
        val accountToPay: String
) : FlowLogic<WireTransaction>() {

    @Suspendable
    override fun call(): WireTransaction {
        // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
        return serviceHub.signInitialTransaction(
                TransactionBuilder(notary = null)
        ).tx
    }
}

class CordaSettlerBankApiSettlement(
        val novatedIou: Obligation<TokenType>
) : FlowLogic<WireTransaction>() {

    @Suspendable
    override fun call(): WireTransaction {
        // Placeholder code to avoid type error when running the tests. Remove before starting the flow task!
        return serviceHub.signInitialTransaction(
                TransactionBuilder(notary = null)
        ).tx
    }
}

@CordaService
class BankApiOracleService(val services: AppServiceHub) : SingletonSerializeAsToken() {
    // API methods here

    private fun checkObligeeReceivedPayment(
            payment: BankApiPayment<TokenType>,
            obligation: IOUState
    ): Boolean {
        return false
    }

    fun hasPaymentSettled(
            payment: BankApiPayment<TokenType>,
            obligation: IOUState
    ): VerifySettlement.VerifyResult {
        return VerifySettlement.VerifyResult.REJECTED
    }
}

data class BankApiPayment<T : TokenType>(
        override val paymentReference: PaymentReference,
        override val amount: Amount<T>,
        override var status: PaymentStatus = PaymentStatus.SENT
) : Payment<T> {
    override fun toString(): String {
        return "Amount: $amount, Payment Reference: $paymentReference, Status: $status"
    }
}

data class BankApiSettlement(
        override val accountToPay: String,
        override val settlementOracle: Party,
        override val paymentFlow: Class<MakeBankApiPayment<*>> = MakeBankApiPayment::class.java
) : OffLedgerPayment<MakeBankApiPayment<*>> {
    override fun toString(): String {
        return "Pay Bank address $accountToPay and use $settlementOracle as settlement."
    }
}

class MakeBankApiPayment<T : TokenType>(
        amount: Amount<T>,
        obligationStateAndRef: StateAndRef<Obligation<*>>,
        settlementMethod: OffLedgerPayment<*>,
        progressTracker: ProgressTracker = MakeOffLedgerPayment.tracker()
) : MakeOffLedgerPayment<T>(amount, obligationStateAndRef, settlementMethod) {

    @Suspendable
    override fun setup() {

    }

    @Suspendable
    override fun checkBalance(requiredAmount: Amount<*>) {

    }

    @Suspendable
    override fun makePayment(obligation: Obligation<*>, amount: Amount<T>): BankApiPayment<T> {
        return BankApiPayment("paymentReferenceHere", amount, PaymentStatus.FAILED)
    }
}