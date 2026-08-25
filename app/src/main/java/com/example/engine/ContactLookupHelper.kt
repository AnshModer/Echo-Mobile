package com.example.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.util.Locale

data class ContactMatch(
    val id: String,
    val name: String,
    val number: String,
    val typeLabel: String = "Mobile",
    val photoThumbnailUri: String? = null,
    val matchScore: Int = 0
)

object ContactLookupHelper {

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isDirectPhoneNumber(input: String): Boolean {
        val trimmed = input.trim()
        val digitsAndSymbols = trimmed.replace(Regex("[0-9+\\-().\\s]"), "")
        val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
        
        // If there are no alphabetical characters and at least 3 digits, treat as direct phone number
        return digitsAndSymbols.isEmpty() && digitsOnly.length >= 3
    }

    fun cleanContactQuery(rawQuery: String): String {
        return rawQuery.trim()
            .replace(Regex("^(?i)(to|the|my|contact|for|call|dial)\\s+"), "")
            .replace(Regex("^(?i)(to|the|my)\\s+"), "")
            .replace(Regex("(?i)\\s+(on\\s+)?(mobile|phone|cell|cellphone)$"), "")
            .trim()
    }

    fun findBestContactMatch(context: Context, query: String): ContactMatch? {
        val matches = searchContacts(context, query, limit = 5)
        return matches.firstOrNull()
    }

    fun searchContacts(context: Context, query: String, limit: Int = 10): List<ContactMatch> {
        if (!hasContactsPermission(context)) {
            return emptyList()
        }

        val cleanQuery = cleanContactQuery(query)
        if (cleanQuery.isBlank()) return emptyList()

        val normalizedQuery = cleanQuery.lowercase(Locale.ROOT)
        val matchesList = mutableListOf<ContactMatch>()
        val seenNumbers = mutableSetOf<String>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        var cursor: Cursor? = null
        try {
            // First, query contacts with selection filter for efficiency
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$cleanQuery%")

            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            // If strict selection yielded nothing, fetch all phone contacts and score in-memory
            val allRows = mutableListOf<RawContactRow>()

            if (cursor != null && cursor.moveToFirst()) {
                val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                val photoCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                do {
                    val id = if (idCol >= 0) cursor.getString(idCol) ?: "" else ""
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                    val number = if (numberCol >= 0) cursor.getString(numberCol) ?: "" else ""
                    val type = if (typeCol >= 0) cursor.getInt(typeCol) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val label = if (labelCol >= 0) cursor.getString(labelCol) else null
                    val photo = if (photoCol >= 0) cursor.getString(photoCol) else null

                    if (name.isNotBlank() && number.isNotBlank()) {
                        allRows.add(RawContactRow(id, name, number, type, label, photo))
                    }
                } while (cursor.moveToNext())
            }

            cursor?.close()

            // If empty, query all contacts to allow fuzzy/phonetic match
            if (allRows.isEmpty()) {
                cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val typeCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                    val labelCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                    val photoCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                    do {
                        val id = if (idCol >= 0) cursor.getString(idCol) ?: "" else ""
                        val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                        val number = if (numberCol >= 0) cursor.getString(numberCol) ?: "" else ""
                        val type = if (typeCol >= 0) cursor.getInt(typeCol) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        val label = if (labelCol >= 0) cursor.getString(labelCol) else null
                        val photo = if (photoCol >= 0) cursor.getString(photoCol) else null

                        if (name.isNotBlank() && number.isNotBlank()) {
                            allRows.add(RawContactRow(id, name, number, type, label, photo))
                        }
                    } while (cursor.moveToNext())
                }
            }

            for (row in allRows) {
                val score = calculateMatchScore(row.name, normalizedQuery)
                if (score > 0) {
                    val cleanNum = row.number.replace(Regex("[^0-9+]"), "")
                    if (cleanNum.isNotBlank() && !seenNumbers.contains(cleanNum)) {
                        seenNumbers.add(cleanNum)
                        val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                            context.resources,
                            row.type,
                            row.label
                        ).toString()

                        matchesList.add(
                            ContactMatch(
                                id = row.id,
                                name = row.name,
                                number = row.number,
                                typeLabel = typeLabel,
                                photoThumbnailUri = row.photo,
                                matchScore = score
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        // Sort descending by score, then name length
        return matchesList
            .sortedWith(compareByDescending<ContactMatch> { it.matchScore }.thenBy { it.name.length })
            .take(limit)
    }

    private fun calculateMatchScore(contactName: String, query: String): Int {
        val nameLower = contactName.lowercase(Locale.ROOT).trim()
        val queryLower = query.lowercase(Locale.ROOT).trim()

        if (nameLower == queryLower) return 100

        // Exact match of first word (e.g. "Mom" in "Mom Work")
        val words = nameLower.split(Regex("[\\s,._-]+")).filter { it.isNotBlank() }
        if (words.contains(queryLower)) return 95

        // Starts with query
        if (nameLower.startsWith(queryLower)) return 90

        // Any word in contact starts with query (e.g. "John" in "Uncle John Doe")
        if (words.any { it.startsWith(queryLower) }) return 85

        // Contains query substring
        if (nameLower.contains(queryLower)) return 70

        // Word initials match (e.g. "JD" for "John Doe")
        val initials = words.mapNotNull { it.firstOrNull() }.joinToString("")
        if (initials.equals(queryLower, ignoreCase = true)) return 65

        // Levenshtein / fuzzy distance check for short queries with minor typo
        val distance = computeLevenshteinDistance(nameLower, queryLower)
        if (queryLower.length >= 4 && distance <= 1) return 60
        if (queryLower.length >= 6 && distance <= 2) return 50

        return 0
    }

    private fun computeLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private data class RawContactRow(
        val id: String,
        val name: String,
        val number: String,
        val type: Int,
        val label: String?,
        val photo: String?
    )
}
