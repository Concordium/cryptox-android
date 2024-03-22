package com.concordium.wallet.ui.walletconnect

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.concordium.sdk.crypto.wallet.identityobject.AttributeList
import com.concordium.sdk.crypto.wallet.identityobject.IdentityObject
import com.concordium.sdk.crypto.wallet.web3Id.CredentialAttribute
import com.concordium.sdk.crypto.wallet.web3Id.Statement.AtomicStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.MembershipStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.NonMembershipStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RangeStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RequestStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.RevealStatement
import com.concordium.sdk.crypto.wallet.web3Id.Statement.SetStatement
import com.concordium.sdk.responses.accountinfo.credential.AttributeType
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.data.util.IdentityAttributeConverterUtil
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofSecretStatementBinding
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofStatementCardBinding
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofStatementsBinding
import com.concordium.wallet.databinding.IdentityProofStatementLineBinding
import com.concordium.wallet.util.DateTimeUtil
import java.time.LocalDateTime
import java.time.ZoneId

class DisplayStatements(context: Context, attrs: AttributeSet): LinearLayout(context, attrs) {
    var binding: FragmentWalletConnectIdentityProofStatementsBinding;

    val MIN_DATE = "18000101"
    val MAX_DATE = "99990101";
    val EU_MEMBERS = listOf("AT", "BE", "BG", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "HR")

    init {
        binding = FragmentWalletConnectIdentityProofStatementsBinding.inflate(LayoutInflater.from(context))
        binding.secretStatements.statementHeader.setText(R.string.zero_knowledge_proof)
        binding.secretStatements.revealDescription.visibility = GONE
        binding.secretStatements.revealLines.visibility = GONE
        binding.revealStatements.statementHeader.setText(R.string.information_to_reveal)
        binding.revealStatements.secretLines.visibility = GONE
        addView(binding.root)
    }

     fun setStatement(request: RequestStatement, identity: Identity) {
         binding.revealStatements.revealLines.removeAllViews()
         binding.secretStatements.secretLines.removeAllViews()

        val secretStatements = request.statement.filterNot { it is RevealStatement }
        val revealStatements = request.statement.filterIsInstance<RevealStatement>()

         if (secretStatements.isEmpty()) {
             // If there are no reveal statements, then don't show the reveal box
             binding.secretStatements.root.visibility = GONE
         } else {
             binding.secretStatements.root.visibility = VISIBLE
             secretStatements.forEach {
                 binding.secretStatements.secretLines.addView(getSecretStatement(it, it.canBeProvedBy((getIdentityObject(identity)))))
             }
         }

         if (revealStatements.isEmpty()) {
             // If there are no reveal statements, then don't show the reveal box
             binding.revealStatements.root.visibility = GONE
         } else {
             val attributes = identity.identityObject?.let { it.attributeList.chosenAttributes.mapValues { attribute ->
                 CredentialAttribute.builder().value(attribute.value)
                     .type(CredentialAttribute.CredentialAttributeType.STRING).build()
             }} ?: emptyMap()

             binding.revealStatements.root.visibility = VISIBLE
             revealStatements.forEach {
                 binding.revealStatements.revealLines.addView(getRevealLine(it, attributes))
             }
         }
    }


    private fun setStatementLine(line: IdentityProofStatementLineBinding, tag: String, value: String, provable: Boolean) {
        line.attributeTag.text = tag
        line.attributeValue.text = value

        if (!provable) {
            line.checkMark.setImageResource(R.drawable.ccx_ico_declined)
        }
    }

    private fun createStatementLine(): IdentityProofStatementLineBinding {
        return IdentityProofStatementLineBinding.inflate(
            LayoutInflater.from(
                context
            ), binding.root, false
        )
    }

    private fun getRevealLine(statement: RevealStatement, attributes: Map<String, CredentialAttribute>): View {
        val revealBinding = createStatementLine()
        val rawAttribute = attributes[statement.attributeTag]?.value ?: "???"
        // Assuming this is identity attributes
        val pair = IdentityAttributeConverterUtil.convertAttribute(context, Pair(statement.attributeTag,rawAttribute))
        revealBinding.attributeTag.text = pair.first
        revealBinding.attributeValue.text = pair.second
        setStatementLine(revealBinding, pair.first, pair.second, true)

        return revealBinding.root
    }

    private fun getSecretStatement(statement: AtomicStatement, provable: Boolean): View {
        val secretBinding =
            FragmentWalletConnectIdentityProofSecretStatementBinding.inflate(
                LayoutInflater.from(
                    context
                ), binding.root, false
            )

        setStatementLine(secretBinding.secretStatement, getPropertyTitle(statement), getStatementValue(statement), provable)
        secretBinding.statementDescription.text = getStatementDescription(statement)

        return secretBinding.root
    }

    private fun getStatementDescription(atomicStatement: AtomicStatement): String {
        val name = IdentityAttributeConverterUtil.convertAttributeTag(context, atomicStatement.attributeTag)
        return when (atomicStatement) {
            is RangeStatement -> context.getString(R.string.identity_proofs_range_statement_description, name, getPropertyValue(atomicStatement.attributeTag, atomicStatement.lower.value), getPropertyValue(atomicStatement.attributeTag, atomicStatement.upper.value))
            is MembershipStatement -> context.getString(R.string.identity_proofs_membership_statement_description, name, atomicStatement.set.joinToString { getPropertyValue(atomicStatement.attributeTag, it.value) })
            is NonMembershipStatement -> context.getString(R.string.identity_proofs_non_membership_statement_description, name, atomicStatement.set.joinToString { getPropertyValue(atomicStatement.attributeTag, it.value) })
            else -> throw IllegalArgumentException("Unknown statement type")
        }
    }

    private fun getPropertyTitle(statement: AtomicStatement): String {
        if (isAgeStatement(statement) && AttributeType.fromJSON(statement.attributeTag) == AttributeType.DOB) {
            return context.getString(R.string.identity_proofs_age)
        }
        return IdentityAttributeConverterUtil.convertAttributeTag(context, statement.attributeTag)
    }

    private fun getPropertyValue(attributeTag: String, attributeValue: String): String {
        return IdentityAttributeConverterUtil.convertAttributeValue(context, attributeTag, attributeValue)
    }

    /**
     * @param date, the date string which days are added to.
     * It is assumed to be on the form "YYYYMMDD"
     * @returns A new date string on the form "YYYYMMDD"
     */
    private fun addDays(date: String, days: Long): String {
        val dateObject = DateTimeUtil.parseLongDate(date)
            ?: throw RuntimeException("Failed to parse date: $date")
        val dateWithDaysAdded = LocalDateTime.ofInstant(dateObject.toInstant(), ZoneId.systemDefault()).plusDays(days)
        return dateToDateString(dateWithDaysAdded)
    }

    /**
     * Change a date to a string on the form "YYYYMMDD"
     */
    private fun dateToDateString(date: LocalDateTime): String {
        val day = date.dayOfMonth.toString().padStart(2, '0')
        val month = date.monthValue.toString().padStart(2, '0')
        val year = date.year.toString()
        return year + month + day
    }

    private fun isAgeStatement(statement: AtomicStatement): Boolean {
        return when (statement) {
            is RevealStatement -> false
            is MembershipStatement -> false
            is NonMembershipStatement -> false
            is RangeStatement -> {
                val today = getPastDate(0)
                val isYearOffsetUpper = addDays(statement.upper.value, -1).substring(4) == today.substring(4)
                if (statement.lower.value == MIN_DATE) {
                    return isYearOffsetUpper
                }

                val isYearOffsetLower = addDays(statement.lower.value, -1).substring(4) == today.substring(4)
                if (statement.upper.value > today) {
                    return isYearOffsetLower
                }

                return isYearOffsetUpper && isYearOffsetLower
            }
            else -> false
        }
    }

    /**
     * @param date, the date string from which the year should be extracted from.
     * It is assumed to be on the form "YYYYMMDD"
     * @returns the year as an integer
     */
    private fun getYearFromDateString(date: String): Int {
        return Integer.parseInt(date.substring(0, 4))
    }

    /**
     * @param date, the date string that should be formatted for display.
     * It is assumed to be on the form "YYYYMMDD"
     */
    private fun formatDateString(date: String): String {
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6)
    }

    /**
     * returns a date string for the specified years ago with the specified day offset.
     * @return A string on the form "YYYYMMDD"
     */
    private fun getPastDate(yearsAgo: Long, daysOffset: Long = 0): String {
        val now = LocalDateTime.now().minusYears(yearsAgo).minusDays(daysOffset)
        return dateToDateString(now)
    }

    private fun getRangeStatementValue(rangeStatement: RangeStatement): String {
        val lowerValue = rangeStatement.lower.value
        val upperValue = rangeStatement.upper.value

        when (AttributeType.fromJSON(rangeStatement.attributeTag)) {
            AttributeType.DOB -> {
                val today = getPastDate(0)

                if (isAgeStatement(rangeStatement)) {
                    val ageMin = getYearFromDateString(today) - getYearFromDateString(addDays(upperValue, -1))
                    val ageMax = getYearFromDateString(today) - getYearFromDateString(addDays(lowerValue, -1)) - 1

                    if (lowerValue == MIN_DATE) {
                        return context.resources.getQuantityString(R.plurals.identity_proofs_age_min, ageMin, ageMin)
                    }

                    if (upperValue > today) {
                        return context.resources.getQuantityString(R.plurals.identity_proofs_age_max, ageMax, ageMax)
                    }

                    if (ageMin == ageMax) {
                        return context.resources.getQuantityString(R.plurals.identity_proofs_age_exact, ageMin, ageMin)
                    }

                    return context.resources.getQuantityString(R.plurals.identity_proofs_age_between, ageMax, ageMin, ageMax)
                }

                val minDateString = formatDateString(lowerValue)

                val upper = if (today < upperValue) { today } else { upperValue }
                val maxDateString = formatDateString(upper)

                if (lowerValue == MIN_DATE) {
                    return context.getString(R.string.identity_proofs_date_before, maxDateString)
                }

                if (upperValue > today) {
                    return context.getString(R.string.identity_proofs_date_after_inclusive, minDateString)
                }

                return context.getString(R.string.identity_proofs_date_between, minDateString, maxDateString)
            }
            AttributeType.ID_DOC_ISSUED_AT, AttributeType.ID_DOC_EXPIRES_AT -> {
                val minDateString = formatDateString(lowerValue)
                val maxDateString = formatDateString(upperValue)

                if (lowerValue == MIN_DATE) {
                    return context.getString(R.string.identity_proofs_date_before, maxDateString)
                }

                if (upperValue == MAX_DATE) {
                    return context.getString(R.string.identity_proofs_date_after_inclusive, minDateString)
                }

                return context.getString(R.string.identity_proofs_date_between, minDateString, maxDateString)
            }
            else -> {
                return context.getString(R.string.identity_proofs_generic_between, lowerValue, upperValue)
            }
        }
    }

    private fun isEuCountrySet(countries: List<String>): Boolean {
        return countries.size == EU_MEMBERS.size && countries.containsAll(EU_MEMBERS)
    }

    private fun getSetStatementValue(setStatement: SetStatement): String {
        val setSize = setStatement.set.size
        val isMembershipStatement: Boolean = when (setStatement) {
            is MembershipStatement -> true
            is NonMembershipStatement -> false
            else -> throw IllegalArgumentException("An unsupported set statement was provided")
        }

        when (AttributeType.fromJSON(setStatement.attributeTag)) {
            AttributeType.NATIONALITY, AttributeType.COUNTRY_OF_RESIDENCE -> {
                if (isEuCountrySet(setStatement.set.map { it.value })) {
                    return if (isMembershipStatement) context.getString(R.string.identity_proofs_eu_membership) else context.getString(R.string.identity_proofs_non_eu_membership)
                }
                return if (isMembershipStatement) context.resources.getQuantityString(R.plurals.identity_proofs_countries, setSize, setSize) else context.resources.getQuantityString(R.plurals.identity_proofs_none_countries, setSize, setSize)
            }
            AttributeType.ID_DOC_TYPE -> {
                return if (isMembershipStatement) context.resources.getQuantityString(R.plurals.identity_proofs_id_doc_types, setSize, setSize) else context.resources.getQuantityString(R.plurals.identity_proofs_none_id_doc_types, setSize, setSize)
            }
            AttributeType.ID_DOC_ISSUER -> {
                return if (isMembershipStatement) context.resources.getQuantityString(R.plurals.identity_proofs_id_doc_issuer, setSize, setSize) else context.resources.getQuantityString(R.plurals.identity_proofs_none_id_doc_issuer, setSize, setSize)
            }
            else -> {
                return if (isMembershipStatement) context.resources.getQuantityString(R.plurals.identity_proofs_set_fallback, setSize, setSize) else context.resources.getQuantityString(R.plurals.identity_proofs_set_non_fallback, setSize, setSize)
            }
        }
    }

    private fun getStatementValue(atomicStatement: AtomicStatement): String {
        return when (atomicStatement) {
            is RangeStatement -> {
                getRangeStatementValue(atomicStatement)
            }

            is MembershipStatement -> {
                getSetStatementValue(atomicStatement)
            }

            is NonMembershipStatement -> {
                getSetStatementValue(atomicStatement)
            }

            else -> throw IllegalArgumentException("Unknown statement type")
        }
    }
}


/**
 * Get an IdentityObject compatible with the Concordium SDK methods.
 * N.B. Only the attributeList is populated, the remaining fields are null
  */
fun getIdentityObject(identity: Identity): IdentityObject {
    val identityObject = identity.identityObject!!
    val attributes = AttributeList.builder().chosenAttributes(identityObject.attributeList.chosenAttributes.mapKeys { AttributeType.fromJSON(it.key) }).build()
    return IdentityObject.builder().attributeList(attributes).build()
}
