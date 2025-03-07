package NoMathExpectation.NMEBoot.util

import java.security.MessageDigest

private val sha1Digest = MessageDigest.getInstance("SHA-1")

fun ByteArray.sha1(): ByteArray = sha1Digest.digest(this).also { sha1Digest.reset() }

@OptIn(ExperimentalStdlibApi::class)
fun ByteArray.sha1String() = sha1().toHexString()