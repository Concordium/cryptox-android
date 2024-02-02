package com.concordium.wallet.ui.common.delegates

import androidx.core.app.ComponentActivity
import com.concordium.wallet.App
import com.concordium.wallet.data.IdentityRepository
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.room.WalletDatabase
import com.concordium.wallet.ui.base.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

interface IdentityStatusDelegate {
    fun startCheckForPendingIdentity(
        activity: ComponentActivity?,
        specificIdentityId: Int?,
        showForFirstIdentity: Boolean,
        statusChanged: (Identity) -> Unit
    )

    fun stopCheckForPendingIdentity()
}

class IdentityStatusDelegateImpl : IdentityStatusDelegate {
    private var job: Job? = null
    private var timerTask: TimerTask? = null
    private var showForFirstIdentity = false

    override fun startCheckForPendingIdentity(
        activity: ComponentActivity?,
        specificIdentityId: Int?,
        showForFirstIdentity: Boolean,
        statusChanged: (Identity) -> Unit
    ) {
        this.showForFirstIdentity = showForFirstIdentity

        if (activity == null || activity.isFinishing || activity.isDestroyed)
            return
        if (App.appCore.newIdentities.isNotEmpty()) {
            for (newIdentity in App.appCore.newIdentities) {
                if (specificIdentityId == null || specificIdentityId == newIdentity.key) {
                    CoroutineScope(Dispatchers.IO).launch {
                        job = launch {
                            val identityRepository = IdentityRepository(
                                WalletDatabase.getDatabase(activity).identityDao()
                            )
                            val identity = identityRepository.findById(newIdentity.key)
                            if (isActive) {
                                identity?.let {
                                    activity.runOnUiThread {
                                        if ((activity as BaseActivity).isActive) {
                                            statusChanged(identity)
                                        }
                                    }
                                }

                                delay(1000)

                                startCheckForPendingIdentity(
                                    activity,
                                    specificIdentityId,
                                    showForFirstIdentity,
                                    statusChanged
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // TODO refactor to use one way of scheduling and cancellation.
            // Weird... Seems like there could be a race
            // between the timer and the delayed coroutine from above.
            timerTask = Timer().schedule(1000) {
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
        timerTask?.cancel()
    }
}
