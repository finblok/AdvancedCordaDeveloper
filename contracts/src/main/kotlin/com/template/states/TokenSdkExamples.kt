package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

data class ExampleFixedToken(
        override val tokenIdentifier: String,
        override val fractionDigits: Int = 0
) : TokenType(tokenIdentifier, fractionDigits)

data class ExampleEvolvableToken(
        override val maintainers: List<Party>,
        override val fractionDigits: Int,
        val exampleDataProperty: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : EvolvableTokenType()

fun createNonFungibleFixedToken(issuer: Party, tokenHolder: Party): NonFungibleToken? {
    val token = ExampleFixedToken("CUSTOMTOKEN", 0);
    val issuedFixedToken =
            IssuedTokenType(issuer, token);
    val nonFungibleToken = NonFungibleToken(issuedFixedToken, tokenHolder, UniqueIdentifier());
    return nonFungibleToken
}

fun createNonFungibleEvolvableToken(issuer: Party, tokenHolder: Party): NonFungibleToken? {
    val token = ExampleEvolvableToken(listOf(), 0, "test");
    val linearPointer = LinearPointer(
            token.linearId, ExampleEvolvableToken::class.java
    );
    val tokenPointer = TokenPointer(linearPointer, token.fractionDigits);
    val issuedToken = IssuedTokenType(issuer, tokenPointer)
    val nonFungibleToken = NonFungibleToken(issuedToken, tokenHolder, UniqueIdentifier());
    return nonFungibleToken
}

fun createFungibleFixedToken(issuer: Party, tokenHolder: Party, tokenQuantity: Long): FungibleToken? {
    val token = ExampleFixedToken("CUSTOMTOKEN", 0);
    val issuedTokenType = IssuedTokenType(issuer, token);
    val fungibleToken = FungibleToken(Amount(tokenQuantity, issuedTokenType), tokenHolder);
    return fungibleToken
}

fun createFungibleEvolvableToken(issuer: Party, tokenHolder: Party, tokenQuantity: Long): FungibleToken? {
    val token = ExampleEvolvableToken(listOf(), 0, "test");
    val linearPointer = LinearPointer(
            token.linearId, ExampleEvolvableToken::class.java
    );
    val tokenPointer = TokenPointer(linearPointer, token.fractionDigits);
    val issuedToken = IssuedTokenType(issuer, tokenPointer)
    val fungibleToken = FungibleToken(Amount(tokenQuantity, issuedToken), tokenHolder);
    return fungibleToken
}