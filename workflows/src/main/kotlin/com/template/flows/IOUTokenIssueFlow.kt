package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.issue.IssueTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.issue.addIssueTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.heldBy
import com.template.contracts.IOUContract
import com.template.states.*
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import javax.annotation.Signed

@InitiatingFlow
@StartableByRPC
class IOUTokenIssueFlow(val tokenAmount: Int): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val issuedTokenType = IOUToken("CUSTOM_TOKEN", 0) issuedBy ourIdentity
        val fungibleTokens = tokenAmount of issuedTokenType heldBy ourIdentity
        return subFlow(IssueTokens(listOf(fungibleTokens)));

//       ALTERNATIVE SYNTAX
//       val issuedTokenType = IssuedTokenType(ourIdentity, state.amount.token);
//       val fungibleToken =
//                FungibleToken(
//                        Amount(10000, issuedTokenType),
//                        state.lender
//       );
    }
}

@InitiatingFlow
@StartableByRPC
class DeliveryVersusPaymentTokenFlow(
        val ourPayment: ExampleFixedToken, val counterPartyAsset: ExampleFixedToken, val counterParty: Party
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val fungibleToken = 10 of ourPayment

        var builder = TransactionBuilder(notary)
        builder = addMoveFungibleTokens(builder, serviceHub, fungibleToken, counterParty, ourIdentity)
        builder = addMoveNonFungibleTokens(builder, serviceHub, counterPartyAsset, ourIdentity)

        return serviceHub.signInitialTransaction(builder)
    }
}