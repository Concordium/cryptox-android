package com.concordium.wallet.data.util

import android.content.Context
import com.concordium.wallet.R
import com.concordium.wallet.util.DateTimeUtil
import java.util.Locale

object IdentityAttributeConverterUtil {

    fun convertAttribute(
        context: Context,
        attribute: Pair<String, String>
    ): Pair<String, String> {
        return Pair(
            convertAttributeTag(context, attribute.first),
            convertAttributeValue(context, attribute.first, attribute.second)
        )
    }

    fun convertAttributeValue(
        context: Context,
        attributeTag: String,
        attributeValue: String
    ): String {
        return when (attributeTag) {
            "sex" ->
                convertSex(context, attributeValue)

            "dob" ->
                DateTimeUtil.convertLongDate(attributeValue)

            "countryOfResidence" ->
                getCountryName(attributeValue)

            "nationality" ->
                getCountryName(attributeValue)

            "idDocType" ->
                getDocType(context, attributeValue)

            "idDocIssuer" ->
                getCountryName(attributeValue)

            "idDocIssuedAt" ->
                DateTimeUtil.convertLongDate(attributeValue)

            "idDocExpiresAt" ->
                DateTimeUtil.convertLongDate(attributeValue)

            "legalCountry" ->
                getCountryName(attributeValue)

            else ->
                attributeValue
                    .ifEmpty {
                        context.getString(R.string.identity_attribute_unavailable)
                    }
        }
    }

    fun convertAttributeTag(context: Context, tag: String): String {
        return when (tag) {
            // Person:

            "firstName" ->
                context.getString(R.string.identity_attribute_first_name)

            "lastName" ->
                context.getString(R.string.identity_attribute_last_name)

            "sex" ->
                context.getString(R.string.identity_attribute_sex)

            "dob" ->
                context.getString(R.string.identity_attribute_birth_date)

            "countryOfResidence" ->
                context.getString(R.string.identity_attribute_country_residence)

            "nationality" ->
                context.getString(R.string.identity_attribute_nationality)

            "idDocType" ->
                context.getString(R.string.identity_attribute_doc_type)

            "idDocNo" ->
                context.getString(R.string.identity_attribute_doc_no)

            "idDocIssuer" ->
                context.getString(R.string.identity_attribute_doc_issuer)

            "idDocIssuedAt" ->
                context.getString(R.string.identity_attribute_doc_issued_at)

            "idDocExpiresAt" ->
                context.getString(R.string.identity_attribute_doc_expires_at)

            "nationalIdNo" ->
                context.getString(R.string.identity_attribute_national_id_no)

            "taxIdNo" ->
                context.getString(R.string.identity_attribute_tax_id_no)

            // Company:

            "lei" ->
                context.getString(R.string.identity_attribute_lei)

            "legalName" ->
                context.getString(R.string.identity_attribute_legal_name)

            "legalCountry" ->
                context.getString(R.string.identity_attribute_legal_country)

            "businessNumber" ->
                context.getString(R.string.identity_attribute_business_no)

            "registrationAuth" ->
                context.getString(R.string.identity_attribute_registration_auth)

            else -> tag
        }
    }

    private fun convertSex(context: Context, value: String): String {
        return when (value) {
            "0" -> context.getString(R.string.identity_attribute_sex_not_known)
            "1" -> context.getString(R.string.identity_attribute_sex_male)
            "2" -> context.getString(R.string.identity_attribute_sex_female)
            "9" -> context.getString(R.string.identity_attribute_na)
            else -> context.getString(R.string.identity_attribute_unavailable)
        }
    }

    private fun getCountryName(code: String): String {
        val local = Locale("", code)
        return local.displayCountry
    }

    private fun getDocType(context: Context, value: String): String {
        return when (value) {
            "0" -> context.getString(R.string.identity_attribute_na)
            "1" -> context.getString(R.string.identity_attribute_doc_type_passport)
            "2" -> context.getString(R.string.identity_attribute_doc_type_national_id)
            "3" -> context.getString(R.string.identity_attribute_doc_type_driving_license)
            "4" -> context.getString(R.string.identity_attribute_doc_type_immigration_card)
            else -> context.getString(R.string.identity_attribute_unavailable)
        }
    }
}
