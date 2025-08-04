package com.komik.komik.service.component

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun AlertDialogCostum(
    context: Context,
    title: String,
    message: String,
    onPositive: (DialogInterface)-> Unit = {},
    onNegative: (DialogInterface)-> Unit = {},
): MaterialAlertDialogBuilder {
    val builder = MaterialAlertDialogBuilder(context)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton("Ya") { dialog, which ->
        onPositive(dialog)
    }
    builder.setNegativeButton("Tidak") { dialog, which ->
        onNegative(dialog)
    }
    return builder
}