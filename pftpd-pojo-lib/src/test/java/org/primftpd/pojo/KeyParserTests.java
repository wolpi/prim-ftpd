package org.primftpd.pojo;

import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

public class KeyParserTests {

    @Test
    public void noSpaceInKey() throws Exception {
        String key = "no-space";
        InputStream is = new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8));

        List<String> errors = new ArrayList<>();
        List<PublicKey> keys = KeyParser.parsePublicKeys(is, errors);

        Assert.assertTrue(keys.isEmpty());
        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    public void parsePubKeyRsa() throws Exception {
        InputStream is = getClass().getResourceAsStream("/keys/rsa.key.pub");

        List<String> errors = new ArrayList<>();
        List<PublicKey> keys = KeyParser.parsePublicKeys(is, errors);

        assertsRsaKey((RSAPublicKey)keys.get(0));
        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    public void parsePubKeyEcdsa() throws Exception {
        InputStream is = getClass().getResourceAsStream("/keys/ecdsa.key.pub");

        List<String> errors = new ArrayList<>();
        List<PublicKey> keys = KeyParser.parsePublicKeys(is, errors);

        assertsEcdsaKey((ECPublicKey)keys.get(0));
        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    public void parsePubKeyEcdsa384() throws Exception {
        InputStream is = getClass().getResourceAsStream("/keys/ecdsa.key.pub.384");

        List<String> errors = new ArrayList<>();
        List<PublicKey> keys = KeyParser.parsePublicKeys(is, errors);

        assertsEcdsaKey384((ECPublicKey)keys.get(0));
        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    public void parsePubKeyEcdsa521() throws Exception {
        InputStream is = getClass().getResourceAsStream("/keys/ecdsa.key.pub.521");

        List<String> errors = new ArrayList<>();
        List<PublicKey> keys = KeyParser.parsePublicKeys(is, errors);

        assertsEcdsaKey521((ECPublicKey)keys.get(0));
        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    public void parsePubKeyEd25519() throws Exception {
        InputStream is = getClass().getResourceAsStream("/keys/ed25519.key.pub");

        List<String> errors = new ArrayList<>();
        List<PublicKey> keys = KeyParser.parsePublicKeys(is, errors);

        for (PublicKey key : keys) {
            System.out.println("key type: " + key.getClass().getName());
        }
        assertsEd25519((BCEdDSAPublicKey)keys.get(0));
        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    public void parseAuthorizedKeys() throws Exception {
        InputStream is = getClass().getResourceAsStream("/keys/authorized_keys");

        List<String> errors = new ArrayList<>();
        List<PublicKey> keys = KeyParser.parsePublicKeys(is, errors);

        Assert.assertEquals(5, keys.size());
        assertsRsaKey((RSAPublicKey)keys.get(0));
        assertsEcdsaKey((ECPublicKey)keys.get(1));
        assertsEcdsaKey384((ECPublicKey)keys.get(2));
        assertsEcdsaKey521((ECPublicKey)keys.get(3));
        assertsEd25519((BCEdDSAPublicKey)keys.get(4));

        Assert.assertEquals(1, errors.size());
        System.out.println(errors.get(0));
    }

    protected void assertsRsaKey(RSAPublicKey pubKey) {
        Assert.assertEquals(new BigInteger("65537"), pubKey.getPublicExponent());

        final String modulus = "22840028388110743583131987675136887114153126223124011317437832666" +
                "25854781992306722377061897219550740787245366580892823047038154958086308817575397" +
                "86207323511183698710582016112357060923053840856777454937186677760616034425665662" +
                "82261472797239839649294119764258671908502475664743909024714305171394796349355615" +
                "01410512875834037603865586850446929492793894140130256172372280205701912961974382" +
                "44718040286649900869581969011709834002741504113088991590355018061303753262915348" +
                "56911333402703872012358714368938812147820774134682975669390306870781321673316754" +
                "378035200080485404740444851779733064858474545694849794752210968120764651";
        Assert.assertEquals(new BigInteger(modulus), pubKey.getModulus());
    }

    protected void assertsEcdsaKey(ECPublicKey pubKey) {
        final BigInteger val1 = new BigInteger(
                "48439561293906451759052585252797914202762949526041747995844080717082404635286");
        final BigInteger val2 = new BigInteger(
                "36134250956749795798585127919587881956611106672985015071877198253568414405109");

        final BigInteger keyX = pubKey.getParams().getGenerator().getAffineX();
        final BigInteger keyY = pubKey.getParams().getGenerator().getAffineY();

        Assert.assertEquals(val1, keyX);
        Assert.assertEquals(val2, keyY);
    }

    protected void assertsEcdsaKey384(ECPublicKey pubKey) {
        final BigInteger val1 = new BigInteger("26247035095799689268623156744566981891852923491" +
                "109213387815615900925518854738050089022388053975719786650872476732087");
        final BigInteger val2 = new BigInteger("83257109614890299855467512895201081792878530488" +
                "61315594709205902480503199884419224438643760392947333078086511627871");

        final BigInteger keyX = pubKey.getParams().getGenerator().getAffineX();
        final BigInteger keyY = pubKey.getParams().getGenerator().getAffineY();

        Assert.assertEquals(val1, keyX);
        Assert.assertEquals(val2, keyY);
    }

    protected void assertsEcdsaKey521(ECPublicKey pubKey) {
        final String x = "2661740802050217063228768716723360960729859168756973147706671368418802944" +
                "996427808491545080627771902352094241225065558662157113545570916814161637315895999846";
        final String y = "3757180025770020463545507224491183603594455134769762486694567779615544477" +
                "440556316691234405012945539562144444537289428522585666729196580810124344277578376784";

        Assert.assertEquals(new BigInteger(x), pubKey.getParams().getGenerator().getAffineX());
        Assert.assertEquals(new BigInteger(y), pubKey.getParams().getGenerator().getAffineY());
    }

    protected void assertsEd25519(BCEdDSAPublicKey pubKey) {
        final byte[] expectedKey = new byte[] {
                48, 42, 48, 5, 6, 3, 43, 101, 112, 3, 33, 0, -32, -9, -50, -49, -58, -103, -34, -12,
                -45, 16, -112, -11, -12, 122, -48, 77, 113, -56, -128, 63, -17, -94, -56, -49, -104,
                77, -29, 64, -12, -78, -113, 4,
        };

        byte[] encoded = pubKey.getEncoded();

        Assert.assertArrayEquals(expectedKey, encoded);
    }
}
