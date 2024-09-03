package com.concordium.wallet.ui.cis2.defaults

import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.util.Log

class DefaultFungibleTokensManager(
    private val defaults: Collection<DefaultFungibleToken>,
    private val contractTokensRepository: ContractTokensRepository,
) {
    suspend fun addForAccount(accountAddress: String) {
        Log.d(
            "adding_tokens:" +
                    "\naccountAddress=$accountAddress," +
                    "\ntokens=${defaults.size}"
        )

        defaults.forEach { tokenToAdd ->
            val contractToken = tokenToAdd.toContractToken(
                accountAddress = accountAddress,
            )
            contractTokensRepository.insert(contractToken)
        }

        Log.d("tokens_added")
    }
}
