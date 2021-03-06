package kpi.ipt.labs.practicalcrypto;

import kpi.ipt.labs.practicalcrypto.elgamal.key.ElGamalKeyPair;
import kpi.ipt.labs.practicalcrypto.elgamal.key.KeyStore;
import kpi.ipt.labs.practicalcrypto.encryption.AsymmetricBlockCipher;
import kpi.ipt.labs.practicalcrypto.encryption.Cipher;
import kpi.ipt.labs.practicalcrypto.encryption.CipherParameters;
import kpi.ipt.labs.practicalcrypto.encryption.elgamal.ElGamalCipher;
import kpi.ipt.labs.practicalcrypto.encryption.elgamal.ElGamalCipherParameters;
import kpi.ipt.labs.practicalcrypto.encryption.padding.OAEPPadding;
import kpi.ipt.labs.practicalcrypto.encryption.rsa.RSACipher;
import kpi.ipt.labs.practicalcrypto.encryption.rsa.RSACipherParameters;
import kpi.ipt.labs.practicalcrypto.rsa.key.RSAKeyGenerator;
import kpi.ipt.labs.practicalcrypto.rsa.key.RSAKeyPair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class FileEncryption {

    private static final Path WORKING_DIR = Paths.get("D:\\work_dir\\pract_crypt");
    private static final Path TEST_FILE = WORKING_DIR.resolve("test_file.bin");

    public static void main(String[] args) throws IOException {
        rsaTest();
    }

    public static void rsaTest() throws IOException {
        RSAKeyPair keyPair = new RSAKeyGenerator().generateKeyPair(513);

        encryptionTest(
                new RSACipher(),
                new RSACipherParameters(keyPair.getPublicKey()),
                new RSACipherParameters(keyPair.getPrivateKey())
        );
    }

    public static void elGamalTest() throws IOException {
        ElGamalKeyPair keyPair = KeyStore.getOrGenerateAndSave(513);

        encryptionTest(
                new ElGamalCipher(),
                new ElGamalCipherParameters(keyPair.getPublicKey()),
                new ElGamalCipherParameters(keyPair.getPrivateKey())
        );
    }

    public static void encryptionTest(AsymmetricBlockCipher baseCipher,
                                      CipherParameters encParams,
                                      CipherParameters decParams) throws IOException {

        String tmpFilePrefix = baseCipher.getClass().getSimpleName();

        Cipher cipher = new Cipher(new OAEPPadding(baseCipher));

        System.out.println("Started at: " + new Date());

        // 1. Encrypt
        cipher.init(true, encParams);

        Path encryptTemp = Files.createTempFile(WORKING_DIR, tmpFilePrefix + "-enc-", ".tmp");

        try (InputStream in = Files.newInputStream(TEST_FILE);
             OutputStream out = Files.newOutputStream(encryptTemp)) {
            process(in, out, cipher);
        }

        System.out.println("Encrypted at: " + new Date());

        // 2. Decrypt
        cipher.init(false, decParams);

        Path decryptTemp = Files.createTempFile(WORKING_DIR, tmpFilePrefix + "-dec-", ".tmp");

        try (InputStream in = Files.newInputStream(encryptTemp);
             OutputStream out = Files.newOutputStream(decryptTemp)) {
            process(in, out, cipher);
        }

        System.out.println("Decrypted at " + new Date());

        // 3. Check

        try (InputStream decrypted = Files.newInputStream(decryptTemp);
             InputStream original = Files.newInputStream(TEST_FILE)) {
            boolean result = checkEquals(decrypted, original);
            System.out.println("Checked at " + new Date() + ", equal = " + result);
        }
    }

    private static boolean checkEquals(InputStream first, InputStream second) throws IOException {
        for (; ; ) {
            int fByte = first.read();
            int sByte = second.read();

            if (fByte != sByte) {
                return false;
            }

            if (fByte == -1) {
                return true;
            }
        }
    }

    private static void process(InputStream in, OutputStream out, Cipher cipher) throws IOException {
        byte[] buff = new byte[8000];
        int read;

        while ((read = in.read(buff)) != -1) {
            out.write(cipher.update(buff, 0, read));
        }

        out.write(cipher.doFinal());
    }
}
