package com.concordium.wallet.ui.cis2.defaults

import com.concordium.wallet.data.ContractTokensRepository
import com.concordium.wallet.util.Log

class DefaultContractTokensManager(
    private val defaults: Collection<DefaultContractToken>,
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
