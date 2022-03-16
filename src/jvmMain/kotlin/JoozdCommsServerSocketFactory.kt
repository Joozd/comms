package nl.joozd.comms

import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocketFactory

object JoozdCommsServerSocketFactory {
    /**
     * This will generate an SSLServerSocket using TLS protocol
     * Will trow an exception if things go wrong.
     * @param keyStoreFile: Name of keystore file eg. "keystore.jks"
     * @param keyStorePass: Password of that keystore. protip: Maybe not hardcode that.
     * @param keyStoreAlgorithm: Algorithm used for key, eg. "SunX509"
     * @return an SSLServerSocketFactory
     */
    fun getServerSocketFactory(keyStoreFile: String, keyStorePass: String, keyStoreAlgorithm: String): SSLServerSocketFactory {
        val passPhrase = keyStorePass.toCharArray()
        val ssf: SSLServerSocketFactory
        val ctx: SSLContext = SSLContext.getInstance("TLS")
        val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(keyStoreAlgorithm)
        val ks: KeyStore = KeyStore.getInstance("JKS")
        ks.load(FileInputStream(keyStoreFile), passPhrase)
        kmf.init(ks, passPhrase)
        ctx.init(kmf.keyManagers, null, null)
        ssf = ctx.serverSocketFactory
        return ssf
    }
}