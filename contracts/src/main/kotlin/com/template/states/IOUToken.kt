package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.util.*


//@BelongsToContract(IOUContract::class)
//data class IOUToken(val data: String = "data"): ContractState {
//    override val participants: List<Party> get() = listOf()
//}

data class IOUToken(
         override val tokenIdentifier: String,
         override val fractionDigits: Int = 0
): TokenType(tokenIdentifier, fractionDigits)
