package com.concordium.wallet.ui.more.dev

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.data.AccountRepository
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.RecipientRepository
import com.concordium.wallet.data.TransferRepository
import com.concordium.wallet.data.backend.OfflineMockInterceptor
import com.concordium.wallet.data.model.ArDescription
import com.concordium.wallet.data.model.ArsInfo
import com.concordium.wallet.data.model.AttributeList
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.IdentityAttribute
import com.concordium.wallet.data.model.IdentityObject
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.model.IdentityProviderDescription
import com.concordium.wallet.data.model.IdentityProviderInfo
import com.concordium.wallet.data.model.IdentityProviderMetaData
import com.concordium.wallet.data.model.PreIdentityObject
import com.concordium.wallet.data.model.PubInfoForIp
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.model.TransactionType
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.Recipient
import com.concordium.wallet.data.room.Transfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger

class DevViewModel(application: Application) : AndroidViewModel(application) {

    private val _waitingLiveData = MutableLiveData<Boolean>()
    val waitingLiveData: LiveData<Boolean>
        get() = _waitingLiveData

    private val identityRepository =
        IdentityRepository(App.appCore.session.walletStorage.database.identityDao())
    private val accountRepository =
        AccountRepository(App.appCore.session.walletStorage.database.accountDao())
    private val transferRepository =
        TransferRepository(App.appCore.session.walletStorage.database.transferDao())
    private val recipientRepository =
        RecipientRepository(App.appCore.session.walletStorage.database.recipientDao())

    fun initialize() {
    }

    fun createData() = viewModelScope.launch {
        createIdentity()
        createAccounts()
        createTransfers()
        createRecipients()
    }

    private suspend fun createIdentity() {
        val identityProviderInfo = IdentityProviderInfo(
            0,
            IdentityProviderDescription("description", "ID Provider", "url"),
            "",
            ""
        )
        val arsInfos = HashMap<String, ArsInfo>()
        arsInfos.put("1", ArsInfo(1, "", ArDescription("", "", "")))
        val identityProvider =
            IdentityProvider(
                identityProviderInfo,
                arsInfos,
                IdentityProviderMetaData("", "", "", "")
            )
        val pubInfoForIP = PubInfoForIp("", RawJson("{}"), "")
        val preIdentityObject =
            PreIdentityObject(
                RawJson("{}"), pubInfoForIP, "",
                RawJson("{}"), "",
                RawJson("{}"), "", ""
            )
        val identityObject =
            IdentityObject(
                AttributeList(HashMap(), "202003", 345, "201903"),
                preIdentityObject,
                RawJson("{}")
            )
        val identity = Identity(
            0,
            "identity name",
            "",
            "",
            "",
            0,
            identityProvider,
            identityObject,
            null,
            identityProvider.ipInfo.ipIdentity,
            0
        )
        identityRepository.insert(identity)
    }

    private suspend fun createAccounts() {
        val identityList = identityRepository.getAll()
        val identityId = identityList.firstOrNull()?.id ?: 0

        val accountList = ArrayList<Account>()
        val revealedAttributes = ArrayList<IdentityAttribute>().apply {
            add(IdentityAttribute("name1", "value1"))
            add(IdentityAttribute("name2", "value2"))
            add(IdentityAttribute("name3", "value3"))
        }
        val credential = RawJson("{}")
        accountList.add(
            Account(
                id = 0,
                identityId = identityId,
                name = "Finalized account",
                address = "address01",
                submissionId = "a01",
                transactionStatus = TransactionStatus.FINALIZED,
                encryptedAccountData = null,
                revealedAttributes = revealedAttributes,
                credential = CredentialWrapper(credential, 0),
                balance = BigInteger.ZERO,
                shieldedBalance = BigInteger.ZERO,
                encryptedBalance = null,
                encryptedBalanceStatus = ShieldedAccountEncryptionStatus.ENCRYPTED,
                readOnly = false,
                releaseSchedule = null,
                credNumber = 0,
                iconId = 1
            )
        )
        accountList.add(
            Account(
                id = 0,
                identityId = identityId,
                name = "Commited account",
                address = "address02",
                submissionId = "a02",
                transactionStatus = TransactionStatus.COMMITTED,
                encryptedAccountData = null,
                revealedAttributes = revealedAttributes,
                credential = CredentialWrapper(credential, 0),
                balance = BigInteger.ZERO,
                shieldedBalance = BigInteger.ZERO,
                encryptedBalance = null,
                encryptedBalanceStatus = ShieldedAccountEncryptionStatus.ENCRYPTED,
                readOnly = false,
                releaseSchedule = null,
                credNumber = 1,
                iconId = 2
            )
        )
        accountList.add(
            Account(
                id = 0,
                identityId = identityId,
                name = "Received account",
                address = "address03",
                submissionId = "a03",
                transactionStatus = TransactionStatus.RECEIVED,
                encryptedAccountData = null,
                revealedAttributes = revealedAttributes,
                credential = CredentialWrapper(credential, 0),
                balance = BigInteger.ZERO,
                shieldedBalance = BigInteger.ZERO,
                encryptedBalance = null,
                encryptedBalanceStatus = ShieldedAccountEncryptionStatus.ENCRYPTED,
                readOnly = false,
                releaseSchedule = null,
                credNumber = 2,
                iconId = 3
            )
        )
        accountList.add(
            Account(
                id = 0,
                identityId = identityId,
                name = "Absent account",
                address = "address04",
                submissionId = "a04",
                transactionStatus = TransactionStatus.ABSENT,
                encryptedAccountData = null,
                revealedAttributes = revealedAttributes,
                credential = CredentialWrapper(credential, 0),
                balance = BigInteger.ZERO,
                shieldedBalance = BigInteger.ZERO,
                encryptedBalance = null,
                encryptedBalanceStatus = ShieldedAccountEncryptionStatus.ENCRYPTED,
                readOnly = false,
                releaseSchedule = null,
                credNumber = 3,
                iconId = 4
            )
        )
        accountRepository.insertAll(accountList)
    }

    private suspend fun createTransfers() {
        val account = accountRepository.findByAddress("address01")
        val accountId = account?.id ?: 0
        val transferList = ArrayList<Transfer>()
        val createAtBaseLine = OfflineMockInterceptor.initialTimestampSecs * 1000L
        val minuteInMillis = 60000
        val hourInMillis = minuteInMillis * 60

        transferList.add(
            Transfer(
                0,
                accountId,
                1000.toBigInteger(),
                59.toBigInteger(),
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 1 * minuteInMillis),
                "t01",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null
            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                2000.toBigInteger(),
                59.toBigInteger(),
                "address01",
                "address02_not_in_recipientlist",
                1903773385,
                "",
                (createAtBaseLine - 2 * minuteInMillis),
                "t02",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null
            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                3000.toBigInteger(),
                59.toBigInteger(),
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 3 * minuteInMillis),
                "t03",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null
            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                4000.toBigInteger(),
                59.toBigInteger(),
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 4 * minuteInMillis),
                "t04",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null

            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                5000.toBigInteger(),
                59.toBigInteger(),
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 5 * minuteInMillis),
                "t05",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null
            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                6000.toBigInteger(),
                59.toBigInteger(),
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 6 * minuteInMillis),
                "t06",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null

            )
        )
        transferList.add(
            Transfer(
                0,
                accountId,
                7000.toBigInteger(),
                59.toBigInteger(),
                "address01",
                "address02",
                1903773385,
                "",
                (createAtBaseLine - 7 * minuteInMillis),
                "t07",
                TransactionStatus.RECEIVED,
                TransactionOutcome.Success,
                TransactionType.TRANSFER,
                null,
                0,
                null

            )
        )
        // Some more to test merging
        var timeStamp = (createAtBaseLine - 1 * hourInMillis)
        for (i in 1..10) {
            transferList.add(
                Transfer(
                    0,
                    accountId,
                    1000.toBigInteger(),
                    59.toBigInteger(),
                    "address01",
                    "Local",
                    1903773385,
                    "",
                    timeStamp,
                    "t01",
                    TransactionStatus.RECEIVED,
                    TransactionOutcome.Success,
                    TransactionType.TRANSFER,
                    null,
                    0,
                    null
                )
            )
            timeStamp -= 6 * hourInMillis
        }

        transferRepository.insertAll(transferList)
    }

    private suspend fun createRecipients() {
        val recipientLis = ArrayList<Recipient>()
        recipientLis.add(Recipient(0, "Carl1", "address01"))
        recipientLis.add(Recipient(0, "Sara", "address02"))
        recipientLis.add(Recipient(0, "Mohamed", "address03"))
        recipientLis.add(Recipient(0, "Salma", "address04"))
        recipientLis.add(Recipient(0, "Heba", "address05"))
        recipientLis.add(Recipient(0, "Hoda", "address06"))
        recipientLis.add(Recipient(0, "John", "address07"))
        recipientLis.add(Recipient(0, "Jane", "address08"))
        recipientLis.add(Recipient(0, "Brian", "address09"))
        recipientLis.add(Recipient(0, "Ronald", "address10"))
        recipientLis.add(Recipient(0, "James", "address11"))
        recipientLis.add(Recipient(0, "Brittany", "address12"))
        recipientLis.add(Recipient(0, "Jack", "address13"))
        recipientRepository.insertAll(recipientLis)
    }

    fun clearData() = viewModelScope.launch(Dispatchers.IO) {
        identityRepository.deleteAll()
        accountRepository.deleteAll()
        transferRepository.deleteAll()
        recipientRepository.deleteAll()
    }
}
