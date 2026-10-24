package com.salvarez.miboveda.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.salvarez.miboveda.data.crypto.KdfParams
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VaultHeaderStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun newStore() = VaultHeaderStore(
        PreferenceDataStoreFactory.create { File(folder.root, "header.preferences_pb") },
    )

    @Test
    fun `read returns null when no vault was created`() = runTest {
        assertNull(newStore().read())
    }

    @Test
    fun `saved header is read back unchanged`() = runTest {
        val store = newStore()
        val header = VaultHeader(1, KdfParams(3, 65_536, 1), byteArrayOf(1, 2, 3), byteArrayOf(9, 8, 7, 6))
        store.save(header)

        val read = store.read()
        assertNotNull(read)
        assertEquals(header.version, read!!.version)
        assertEquals(header.kdfParams, read.kdfParams)
        assertArrayEquals(header.salt, read.salt)
        assertArrayEquals(header.wrappedDek, read.wrappedDek)
    }

    @Test
    fun `the recovery wrapper is stored, optional and replaceable`() = runTest {
        val store = newStore()
        val withoutRecovery = VaultHeader(1, KdfParams(3, 65_536, 1), byteArrayOf(1), byteArrayOf(2))
        store.save(withoutRecovery)
        assertNull(store.read()!!.recoveryWrappedDek)

        store.saveRecovery(byteArrayOf(7, 7))
        assertArrayEquals(byteArrayOf(7, 7), store.read()!!.recoveryWrappedDek)

        store.save(VaultHeader(1, KdfParams(3, 65_536, 1), byteArrayOf(1), byteArrayOf(2), byteArrayOf(9)))
        assertArrayEquals(byteArrayOf(9), store.read()!!.recoveryWrappedDek)
    }
}
