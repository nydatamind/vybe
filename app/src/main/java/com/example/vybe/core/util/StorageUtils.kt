package com.example.vybe.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

object StorageUtils {

    fun getFriendlyFolderName(context: Context, uri: Uri): String {
        return try {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            val name = docFile?.name
            if (!name.isNullOrBlank()) {
                name
            } else {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                val parts = docId.split(":")
                if (parts.size > 1 && parts[1].isNotBlank()) {
                    parts[1]
                } else {
                    docId
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment ?: "Selected Folder"
        }
    }

    fun takePersistableUriPermission(context: Context, uri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
