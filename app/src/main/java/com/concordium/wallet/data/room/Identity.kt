package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.model.IdentityObject
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.data.room.typeconverter.IdentityTypeConverters
import java.io.Serializable

@Entity(
    tableName = "identity_table",
    indices = [Index(value = ["identity_provider_id", "identity_index"], unique = true)]
)
@TypeConverters(IdentityTypeConverters::class)
data class Identity(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    var name: String,
    var status: String,
    var detail: String?,
    @ColumnInfo(name = "code_uri")
    val codeUri: String,
    @ColumnInfo(name = "next_account_number")
    var nextAccountNumber: Int, // Used for V0 key creation.
    @ColumnInfo(name = "identity_provider")
    var identityProvider: IdentityProvider,
    @ColumnInfo(name = "identity_object")
    var identityObject: IdentityObject?,
    @ColumnInfo(name = "private_id_object_data_encrypted")
    var privateIdObjectDataEncrypted: EncryptedData?, // Used for V0 key creation.
    @ColumnInfo(name = "identity_provider_id")
    var identityProviderId: Int,
    @ColumnInfo(name = "identity_index")
    var identityIndex: Int
) : Serializable
