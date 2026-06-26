/*
 * Copyright 2026 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.evm.osaka;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.web3j.evm.EmbeddedEthereum;
import org.web3j.utils.Numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EIP-7951: the secp256r1 (P-256) signature verification precompile {@code P256VERIFY} at address
 * {@code 0x0000000000000000000000000000000000000100}, activated at Osaka.
 *
 * <p>The 160-byte input is {@code hash || r || s || qx || qy}. A valid signature returns the
 * 32-byte word {@code 1}; an invalid one returns empty output. A pre-Osaka EVM has no code at
 * {@code 0x100}, so a "valid" call would return empty too — the {@code 1} word is the proof the
 * precompile is live.
 *
 * <p>Signatures are generated with the JDK's SunEC provider rather than hard-coded vectors.
 */
public class Eip7951P256VerifyTest {

    private static final String P256VERIFY = "0x0000000000000000000000000000000000000100";

    private EmbeddedEthereum evm;

    @BeforeEach
    public void setUp() {
        evm = OsakaEvmTestSupport.newEmbeddedEthereum();
    }

    private static byte[] concat(final byte[]... parts) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (final byte[] p : parts) {
            out.writeBytes(p);
        }
        return out.toByteArray();
    }

    /** Generates a valid P-256 signature over {@code hash}, returning {hash, r, s, qx, qy}. */
    private static byte[][] sign(final byte[] hash) throws Exception {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        final KeyPair keyPair = kpg.generateKeyPair();

        // NONEwithECDSAinP1363Format signs the raw 32-byte digest and returns r||s (64 bytes).
        final Signature signer = Signature.getInstance("NONEwithECDSAinP1363Format");
        signer.initSign(keyPair.getPrivate());
        signer.update(hash);
        final byte[] rs = signer.sign();

        final ECPublicKey pub = (ECPublicKey) keyPair.getPublic();
        final byte[] r = Arrays.copyOfRange(rs, 0, 32);
        final byte[] s = Arrays.copyOfRange(rs, 32, 64);
        final byte[] qx = OsakaEvmTestSupport.word(pub.getW().getAffineX());
        final byte[] qy = OsakaEvmTestSupport.word(pub.getW().getAffineY());
        return new byte[][] {hash, r, s, qx, qy};
    }

    @Test
    public void validSignatureReturnsOne() throws Exception {
        final byte[] hash = OsakaEvmTestSupport.word(BigInteger.valueOf(0x1234_5678L));
        final byte[][] p = sign(hash);
        final byte[] input = concat(p[0], p[1], p[2], p[3], p[4]);

        final String out = OsakaEvmTestSupport.callAddress(evm, P256VERIFY, input);

        assertEquals(BigInteger.ONE, Numeric.toBigInt(out), "valid P-256 signature should verify");
        assertEquals(32, Numeric.hexStringToByteArray(out).length);
    }

    @Test
    public void invalidSignatureReturnsEmpty() throws Exception {
        final byte[] hash = OsakaEvmTestSupport.word(BigInteger.valueOf(0x1234_5678L));
        final byte[][] p = sign(hash);
        // Corrupt the message hash so the signature no longer matches.
        final byte[] badHash = p[0].clone();
        badHash[0] ^= 0x01;
        final byte[] input = concat(badHash, p[1], p[2], p[3], p[4]);

        final String out = OsakaEvmTestSupport.callAddress(evm, P256VERIFY, input);

        final byte[] outBytes = Numeric.hexStringToByteArray(out);
        assertTrue(
                outBytes.length == 0 || BigInteger.ZERO.equals(Numeric.toBigInt(out)),
                "invalid signature should not verify, got " + out);
    }
}
