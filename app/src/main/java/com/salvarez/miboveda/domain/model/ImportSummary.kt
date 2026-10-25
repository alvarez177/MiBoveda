package com.salvarez.miboveda.domain.model

/** What importing a backup into an existing vault did: new entries added, and those already there. */
data class ImportSummary(val added: Int, val skipped: Int)
