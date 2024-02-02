package com.concordium.wallet.uicore.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.ui.more.export.ExportActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CustomDialogFragment : DialogFragment() {

    companion object {

        val TAG = "CustomDialogFragmentTag"
        val KEY_REQUEST_CODE = "key_request_code"
        val KEY_TYPE = "key_type"
        val KEY_TITLE = "key_title"
        val KEY_MESSAGE = "key_message"
        val KEY_POSITIVE = "key_positive"
        val KEY_NEUTRAL = "key_neutral"
        val KEY_NEGATIVE = "key_negative"
        val KEY_SUPPORT = "key_support"
        val KEY_SUPPORT_EMAIL = "key_support_email"

        val KEY_SUPPORT_TIMESTAMP = "key_support_timestamp"

        var dialogAccountFinalized: Dialog? = null

        //region Create/cancel dialogs
        // ************************************************************

        fun dismissCustomDialog(activity: AppCompatActivity) {
            val dialogFragment = activity.supportFragmentManager.findFragmentByTag(TAG)
            if (dialogFragment is DialogFragment) {
                dialogFragment.dismiss()
                // dialogFragment?.dismissAllowingStateLoss();
            }
        }

        private fun createCustomDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            dialogType: EDialogType,
            title: String,
            message: String,
            positiveButton: String?,
            neutralButton: String?,
            negativeButton: String?,
            uriSession: String?,
            supportEmail: String?
        ): CustomDialogFragment {
            dismissCustomDialog(activity)

            val fragment = CustomDialogFragment()
            val args = Bundle()
            args.putInt(KEY_REQUEST_CODE, requestCode)
            args.putInt(KEY_TYPE, dialogType.ordinal)
            args.putString(KEY_TITLE, title)
            args.putString(KEY_MESSAGE, message)
            if (dialogType == EDialogType.PositiveSupport) {
                args.putString(KEY_POSITIVE, positiveButton)
                args.putString(KEY_NEUTRAL, neutralButton)
                args.putString(KEY_NEGATIVE, negativeButton)
                args.putString(KEY_SUPPORT, uriSession)
                args.putString(KEY_SUPPORT_EMAIL, supportEmail)
            }
            if (dialogType == EDialogType.PositiveNegative) {
                args.putString(KEY_POSITIVE, positiveButton)
                args.putString(KEY_NEGATIVE, negativeButton)
            }
            if (dialogType == EDialogType.OK && positiveButton != null) {
                args.putString(KEY_POSITIVE, positiveButton)
            }
            fragment.setArguments(args)
            return fragment
        }

        fun createOkDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            title: String,
            message: String,
            positive: String?
        ): CustomDialogFragment {
            return createCustomDialog(
                activity,
                requestCode,
                EDialogType.OK,
                title,
                message,
                positive,
                null,
                null,
                null,
                null

            )
        }

        fun createOkCancelDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            title: String,
            message: String
        ): CustomDialogFragment {
            return createCustomDialog(
                activity,
                requestCode,
                EDialogType.OKCancel,
                title,
                message,
                null,
                null,
                null,
                null,
                null
            )
        }

        fun createYesNoDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            title: String,
            message: String
        ): CustomDialogFragment {
            return createCustomDialog(
                activity,
                requestCode,
                EDialogType.YesNo,
                title,
                message,
                null,
                null,
                null,
                null,
                null
            )
        }

        fun createPositiveNegativeDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            title: String,
            message: String,
            positiveButton: String,
            negativeButton: String,
            uriSession: String?,
            supportEmail: String?
        ): CustomDialogFragment {
            return createCustomDialog(
                activity,
                requestCode,
                EDialogType.PositiveNegative,
                title,
                message,
                positiveButton,
                null,
                negativeButton,
                uriSession,
                supportEmail
            )
        }

        fun createPositiveSupportDialog(
            activity: AppCompatActivity,
            requestCode: Int,
            title: String,
            message: String,
            positiveButton: String,
            neutralButton: String,
            negativeButton: String,
            uriSession: String?,
            supportEmail: String?
        ): CustomDialogFragment {
            return createCustomDialog(
                activity,
                requestCode,
                EDialogType.PositiveSupport,
                title,
                message,
                positiveButton,
                neutralButton,
                negativeButton,
                uriSession,
                supportEmail
            )
        }

        fun newAccountFinalizedDialog(context: Context) {
            val v = LayoutInflater.from(context).inflate(R.layout.dialog_account_finalized, null)

            val builder = MaterialAlertDialogBuilder(context)
            builder.setView(v)

            v.findViewById<ImageView>(R.id.close_button)?.setOnClickListener {
                dialogAccountFinalized?.dismiss()
                dialogAccountFinalized = null
            }

            v.findViewById<TextView>(R.id.confirm_button)?.setOnClickListener {
                dialogAccountFinalized?.dismiss()
                dialogAccountFinalized = null
                val intent = Intent(context, ExportActivity::class.java)
                context.startActivity(intent)
            }

            dialogAccountFinalized?.dismiss()
            dialogAccountFinalized = builder.show()
            dialogAccountFinalized?.setCanceledOnTouchOutside(false)
            dialogAccountFinalized?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        //endregion
    }

    private var dialogFragmentListener: Dialogs.DialogFragmentListener? = null
    private var requestCode: Int? = 0

    enum class EDialogType {
        OK, OKCancel, YesNo, PositiveNegative, PositiveSupport
    }

    //region Lifecycle
    // ************************************************************

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            dialogFragmentListener = context as Dialogs.DialogFragmentListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + "must implement DialogFragmentListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val resources = App.appContext.getResources()
        val args: Bundle = requireArguments()
        requestCode = args.getInt(KEY_REQUEST_CODE, 0)
        val type = EDialogType.values()[args.getInt(KEY_TYPE, 0)]
        val title = args.getString(KEY_TITLE, "")
        val message = args.getString(KEY_MESSAGE, "")
        var resPositive = args.getString(KEY_POSITIVE, resources.getString(R.string.dialog_ok))
        val resNeutral = args.getString(KEY_NEUTRAL, null)
        var resNegative = args.getString(KEY_NEGATIVE, resources.getString(R.string.dialog_cancel))

        val uriSession = args.getString(KEY_SUPPORT, null)
        val supportEmail = args.getString(KEY_SUPPORT_EMAIL, null)
        var submissionTime = args.getString(KEY_SUPPORT_TIMESTAMP, null)
        if (type == EDialogType.YesNo) {
            resPositive = resources.getString(R.string.dialog_yes)
            resNegative = resources.getString(R.string.dialog_no)
        }

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setCancelable(true) // This have to be set on dialog to have effect
        builder.setTitle(title)
//         builder.setIcon(R.drawable.ic_icon_copy);
        builder.setMessage(message)
        builder.setPositiveButton(resPositive) { _, _ ->
            dialogFragmentListener?.onDialogResult(requestCode!!, Dialogs.POSITIVE, Intent())
        }
        if (type != EDialogType.OK && type != EDialogType.PositiveSupport) {
            builder.setNegativeButton(resNegative) { _, _ ->
                dialogFragmentListener?.onDialogResult(
                    requestCode!!,
                    Dialogs.NEGATIVE,
                    Intent()
                )
            }
        }
        if (type == EDialogType.PositiveSupport) {
            builder.setNeutralButton(resNeutral) { _, _ ->
                context?.let {
                    if (IdentityErrorDialogHelper.canOpenSupportEmail(it)) {
                        IdentityErrorDialogHelper.openSupportEmail(
                            it,
                            resources,
                            supportEmail,
                            uriSession
                        )
                    } else {
                        IdentityErrorDialogHelper.copyToClipboard(
                            it,
                            title.toString(),
                            resources.getString(
                                R.string.dialog_support_text,
                                uriSession,
                                BuildConfig.VERSION_NAME,
                                Build.VERSION.RELEASE
                            )
                        )
                    }
                }
            }
            builder.setNegativeButton(resNegative,
                DialogInterface.OnClickListener { _, _ ->
                    dismiss()
                })
            builder.setPositiveButton(resPositive,
                DialogInterface.OnClickListener { _, _ ->
                    val intent = Intent(activity, IdentityProviderListActivity::class.java)
                    startActivity(intent)
                })
        }

        val dialog = builder.create()
        // dialog.setOwnerActivity(getActivity());
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        dialogFragmentListener?.onDialogResult(requestCode!!, Dialogs.DISMISSED, Intent())
    }

    //endregion
}
