package com.concordium.wallet.ui.common.delegates

import android.app.Activity
import android.content.Intent
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.model.IdentityStatus
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.identity.identityconfirmed.IdentityConfirmedActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.Timer
import kotlin.concurrent.schedule

interface IdentityStatusDelegate {
    fun startCheckForPendingIdentity(
        activity: Activity?,
        specificIdentityId: Int?,
        showForFirstIdentity: Boolean,
        statusChanged: (Identity) -> Unit
    )

    fun identityDone(
        activity: Activity,
        identity: Identity,
        statusChanged: (Identity) -> Unit
    )

    fun identityError(
        activity: Activity,
        identity: Identity,
        statusChanged: (Identity) -> Unit
    )

    fun stopCheckForPendingIdentity()
}

class IdentityStatusDelegateImpl : IdentityStatusDelegate {
    private var job: Job? = null
    private var showForFirstIdentity = false

    override fun startCheckForPendingIdentity(
        activity: Activity?,
        specificIdentityId: Int?,
        showForFirstIdentity: Boolean,
        statusChanged: (Identity) -> Unit
    ) {
        this.showForFirstIdentity = showForFirstIdentity
        if (activity == null || activity.isFinishing || activity.isDestroyed)
            return
        if (App.appCore.session.newIdentities.isNotEmpty()) {
            for (newIdentity in App.appCore.session.newIdentities) {
                if (specificIdentityId == null || specificIdentityId == newIdentity.key) {
                    CoroutineScope(Dispatchers.IO).launch {
                        job = launch {
                            val identityRepository = IdentityRepository(
                                App.appCore.session.walletStorage.database.identityDao()
                            )
                            val identity = identityRepository.findById(newIdentity.key)
                            identity?.let {
                                activity.runOnUiThread {
                                    if ((activity as BaseActivity).isActive) {
                                        when (identity.status) {
                                            IdentityStatus.DONE -> identityDone(
                                                activity,
                                                identity,
                                                statusChanged
                                            )

                                            IdentityStatus.ERROR -> identityError(
                                                activity,
                                                identity,
                                                statusChanged
                                            )
                                        }
                                    }
                                    startCheckForPendingIdentity(
                                        activity,
                                        specificIdentityId,
                                        showForFirstIdentity,
                                        statusChanged
                                    )
                                }
                            }
                            delay(1000)
                        }
                    }
                }
            }
        } else {
            Timer().schedule(1000) {
                startCheckForPendingIdentity(
                    activity,
                    specificIdentityId,
                    showForFirstIdentity,
                    statusChanged
                )
            }
        }
    }

    override fun stopCheckForPendingIdentity() {
        job?.cancel()
    }

    override fun identityDone(
        activity: Activity,
        identity: Identity,
        statusChanged: (Identity) -> Unit
    ) {
        if (App.appCore.session.newIdentities[identity.id] == null)
            return
        App.appCore.session.newIdentities.remove(identity.id)

        if (showForFirstIdentity) {
            statusChanged(identity)
            return
        }

        val builder = MaterialAlertDialogBuilder(activity)
        builder.setTitle(R.string.identities_overview_identity_verified_title)
        builder.setMessage(
            activity.getString(
                R.string.identities_overview_identity_verified_message,
                identity.name
            )
        )
        builder.setPositiveButton(activity.getString(R.string.identities_overview_identity_create_account_now)) { dialog, _ ->
            dialog.dismiss()
            val intent = Intent(activity, IdentityConfirmedActivity::class.java)
            intent.putExtra(IdentityConfirmedActivity.EXTRA_IDENTITY, identity)
            intent.putExtra(IdentityConfirmedActivity.SHOW_FOR_CREATE_ACCOUNT, true)
            activity.startActivity(intent)
            EventBus.getDefault().post(MainViewModel.State.Home)
        }
        builder.setNegativeButton(activity.getString(R.string.identities_overview_identity_later)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        statusChanged(identity)
        dialog.show()
    }

    override fun identityError(
        activity: Activity,
        identity: Identity,
        statusChanged: (Identity) -> Unit
    ) {
        if (App.appCore.session.newIdentities[identity.id] == null)
            return
        App.appCore.session.newIdentities.remove(identity.id)

        if (showForFirstIdentity) {
            statusChanged(identity)
        } else {
            val builder = MaterialAlertDialogBuilder(activity)
            builder.setTitle(R.string.identities_overview_identity_rejected_title)
            identityErrorNextIdentity(activity, identity, builder, statusChanged)

            val dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)
            statusChanged(identity)
            dialog.show()
        }
    }

    private fun identityErrorNextIdentity(
        activity: Activity,
        identity: Identity,
        builder: MaterialAlertDialogBuilder,
        statusChanged: (Identity) -> Unit
    ) {
        builder.setMessage(
            activity.getString(
                R.string.identities_overview_identity_rejected_text,
                "${identity.name}\n${identity.detail ?: ""}"
            )
        )
        builder.setPositiveButton(activity.getString(R.string.identities_overview_identity_request_another)) { dialog, _ ->
            dialog.dismiss()
            activity.startActivity(Intent(activity, IdentityProviderListActivity::class.java))
        }
        builder.setNegativeButton(activity.getString(R.string.identities_overview_identity_later)) { dialog, _ ->
            dialog.dismiss()
            statusChanged(identity)
        }
    }
}
