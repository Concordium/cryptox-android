package com.concordium.wallet.ui.cis2.defaults

import com.concordium.wallet.data.AccountContractRepository
import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.data.room.AccountContract
import com.concordium.wallet.util.Log

class DefaultFungibleTokensManager(
    private val defaults: Collection<DefaultFungibleToken>,
    private val contractTokensRepository: ContractTokensRepository,
    private val accountContractRepository: AccountContractRepository,
) {
    suspend fun addForAccount(accountAddress: String) {
        Log.d(
            "adding_tokens:" +
                    "\naccountAddress=$accountAddress," +
                    "\ntokens=${defaults.size}"
        )

        defaults.forEach { tokenToAdd ->
            val accountContract =
                accountContractRepository.find(
                    accountAddress = accountAddress,
                    contractIndex = tokenToAdd.contractIndex,
                )
            if (accountContract == null) {
                accountContractRepository.insert(
                    AccountContract(
                        id = 0,
                        accountAddress = accountAddress,
                        contractIndex = tokenToAdd.contractIndex,
                    )
                )

                Log.d(
                    "created_account_contract:" +
                            "\ncontractIndex=${tokenToAdd.contractIndex}"
                )
            }

            val contractToken = tokenToAdd.toContractToken(accountAddress)
            contractTokensRepository.insert(contractToken)
        }

        Log.d("tokens_added")
    }
}
