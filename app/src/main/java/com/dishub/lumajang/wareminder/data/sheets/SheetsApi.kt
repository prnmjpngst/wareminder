package com.dishub.lumajang.wareminder.data.sheets

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SheetsApi @Inject constructor(
    private val sheets: Sheets?
) {
    companion object {
        private const val RANGE = "Sheet1!A:M"
        private const val SPREADSHEET_ID_KEY = "spreadsheet_id"
    }

    fun isAvailable(): Boolean = sheets != null

    fun fetchAllVehicles(spreadsheetId: String): List<Vehicle> {
        if (sheets == null) return emptyList()
        val response: ValueRange = sheets.spreadsheets().values()
            .get(spreadsheetId, RANGE)
            .execute()
        val rows = response.getValues() ?: return emptyList()
        return rows.mapIndexedNotNull { index, row ->
            if (index == 0) null // skip header
            Vehicle.fromRow(row, index + 1)
        }
    }

    fun markDone(spreadsheetId: String, rowIndex: Int) {
        if (sheets == null) return
        sheets.spreadsheets().values()
            .update(spreadsheetId, "M$rowIndex", ValueRange().setValues(listOf(listOf("done"))))
            .setValueInputOption("USER_ENTERED")
            .execute()
    }
}
