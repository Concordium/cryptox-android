package com.concordium.wallet.ui.seed.setup

import cash.z.ecc.android.bip39.Mnemonics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenerateSeedPhraseUseCase() {

    suspend operator fun invoke(): List<String> = withContext(Dispatchers.Default) {
        Mnemonics
            .MnemonicCode(Mnemonics.WordCount.COUNT_24)
            .words
            .map(CharArray::concatToString)
    }
}
