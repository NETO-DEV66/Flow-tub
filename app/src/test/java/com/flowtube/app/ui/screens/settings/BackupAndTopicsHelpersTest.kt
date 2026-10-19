package com.flowtube.app.ui.screens.settings

import com.flowtube.app.data.backup.ExportKind
import com.flowtube.app.data.local.LocalDataManager.AutoBackupFrequency
import com.flowtube.app.ui.screens.settings.backup.folderDisplayName
import com.flowtube.app.ui.screens.settings.backup.periodDays
import com.flowtube.app.ui.screens.settings.topics.unblockedSuggestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.Locale

class BackupAndTopicsHelpersTest {
    @Test
    fun `schedule periods match the old worker intervals`() {
        assertNull(AutoBackupFrequency.NONE.periodDays())
        assertEquals(1L, AutoBackupFrequency.DAILY.periodDays())
        assertEquals(7L, AutoBackupFrequency.WEEKLY.periodDays())
        assertEquals(30L, AutoBackupFrequency.MONTHLY.periodDays())
    }

    @Test
    fun `folder names drop the storage volume`() {
        assertEquals("Documents/Backups", folderDisplayName("primary:Documents/Backups"))
        assertEquals("NeuroTube", folderDisplayName("1A2B-3C4D:NeuroTube/"))
        assertNull(folderDisplayName("primary:"))
        assertNull(folderDisplayName(null))
    }

    @Test
    fun `export file names keep their historical shapes`() {
        val now = Instant.ofEpochMilli(1_700_000_000_000)
        assertEquals("flow_master_backup_1700000000000.zip", ExportKind.MASTER.fileName(now))
        assertEquals("flow-watch-history.json", ExportKind.WATCH_HISTORY.fileName(now))
    }

    @Test
    fun `blocked suggestions hide whatever the case they were stored in`() {
        val remaining = unblockedSuggestions(listOf("ASMR", "News", "drama"), blocked = setOf("asmr", "drama"), locale = Locale.ROOT)
        assertEquals(listOf("News"), remaining)
    }
}
