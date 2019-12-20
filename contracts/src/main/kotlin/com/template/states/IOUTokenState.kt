package com.template.states

import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.template.contracts.IOUContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

/**
 * The IOU State object, with the following properties:
 * - [amount] The amount owed by the [borrower] to the [lender]
 * - [lender] The lending party.
 * - [borrower] The borrowing party.
 * - [contract] Holds a reference to the [IOUContract]
 * - [paid] Records how much of the [amount] has been paid.
 * - [linearId] A unique id shared by all LinearState states representing the same agreement throughout history within
 *   the vaults of all parties. Verify methods should check that one input and one output share the id in a transaction,
 *   except at issuance/termination.
 */
@BelongsToContract(IOUContract::class)
data class IOUTokenState(val amount: Amount<IOUToken>,
                    val lender: Party,
                    val borrower: Party,
                    val paid: Amount<IOUToken> = Amount(0, amount.token),
                    override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState, QueryableState {
    /**
     *  This property holds a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    override val participants: List<Party> get() = listOf(lender, borrower)

    /**
     * Helper methods for when building transactions for settling and transferring IOUs.
     * - [pay] adds an amount to the paid property. It does no validation.
     * - [withNewLender] creates a copy of the current state with a newly specified lender. For use when transferring.
     */
    fun pay(amountToPay: Amount<IOUToken>) = copy(paid = paid.plus(amountToPay))
    fun withNewLender(newLender: Party) = copy(lender = newLender)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun supportedSchemas(): Iterable<MappedSchema> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}