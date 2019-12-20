package com.template.states

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import net.corda.core.identity.Party

// TODO: Create Fixed Token type
// Remove the placeholder parameter.
class ExampleFixedToken(vararg placeholder: Any) {}

// TODO: Create Evolvable Token type
// Remove the placeholder parameter.
class ExampleEvolvableToken(vararg placeholder: Any)

// TODO: Create non fungible fixed token method
fun createNonFungibleFixedToken(issuer: Party, tokenHolder: Party): NonFungibleToken? { return null }

// TODO: Create non fungible evolvable token method
fun createNonFungibleEvolvableToken(issuer: Party, tokenHolder: Party): NonFungibleToken? { return null }

// TODO: Create fungible fixed token method
fun createFungibleFixedToken(issuer: Party, tokenHolder: Party, tokenQuantity: Long): FungibleToken? { return null }

// TODO: Create fungible evolvable token method
fun createFungibleEvolvableToken(issuer: Party, tokenHolder: Party, tokenQuantity: Long): FungibleToken? { return null; }