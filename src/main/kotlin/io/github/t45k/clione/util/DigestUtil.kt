package io.github.t45k.clione.util

import org.apache.commons.codec.binary.Hex
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class DigestUtil {
    companion object {
        /**
         * Perform HMAC SHA1 HEX Digest
         *
         * @param algorithm hashing algorithm. Ordinarily, only SHA1 is given.
         * @param contents string that you want to hash
         *
         * @return hex hash value
         */
        fun digest(algorithm: String, secret: String, contents: String): String =
            Mac.getInstance(algorithm)
                .apply {
                    val secretKeySpec = SecretKeySpec(secret.toByteArray(), algorithm)
                    init(secretKeySpec)
                }
                .doFinal(contents.toByteArray())
                .let(Hex::encodeHex)
                .let { String(it) }

        /**
         * get RSAPrivateKey from contents of a pem file
         *
         * @param pemFileContents contents of a target pem file
         *
         * @return RSA private key
         */
        fun getRSAPrivateKeyFromPEMFileContents(pemFileContents: String): RSAPrivateKey =
            PemReader(StringReader(pemFileContents))
                .readPemObject()
                .let { PKCS8EncodedKeySpec(it.content) }
                .let { KeyFactory.getInstance("RSA").generatePrivate(it) as RSAPrivateKey }
    }
}
