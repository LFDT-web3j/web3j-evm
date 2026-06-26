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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.web3j.evm.EmbeddedEthereum;
import org.web3j.utils.Numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EIP-7883 (MODEXP gas repricing) and EIP-7823 (MODEXP input size bounds), both activated at Osaka,
 * exercised against the MODEXP precompile at address {@code 0x05}.
 *
 * <ul>
 *   <li>EIP-7823 caps base/exponent/modulus length at 1024 bytes; an over-sized length field makes
 *       the precompile fail (the surrounding {@code STATICCALL} returns 0). This behaviour is
 *       Osaka-specific — a pre-Osaka EVM would accept the larger input.
 *   <li>EIP-7883 raises the minimum MODEXP cost to 500 gas; the measured cost of a tiny MODEXP is
 *       therefore at least that floor, which a pre-Osaka EVM (200 gas floor) would not satisfy.
 * </ul>
 */
public class Eip7883And7823ModExpTest {

    // Returns word0 = STATICCALL success flag, followed by the precompile return data.
    // CALLDATASIZE PUSH1 0 PUSH1 0 CALLDATACOPY
    // PUSH1 0 PUSH1 0 CALLDATASIZE PUSH1 0 PUSH1 5 GAS STATICCALL
    // PUSH1 0 MSTORE
    // RETURNDATASIZE PUSH1 0 PUSH1 0x20 RETURNDATACOPY
    // RETURNDATASIZE PUSH1 0x20 ADD PUSH1 0 RETURN
    private static final byte[] CALL_RUNTIME = {
        0x36,
        0x60,
        0x00,
        0x60,
        0x00,
        0x37,
        0x60,
        0x00,
        0x60,
        0x00,
        0x36,
        0x60,
        0x00,
        0x60,
        0x05,
        0x5a,
        (byte) 0xfa,
        0x60,
        0x00,
        0x52,
        0x3d,
        0x60,
        0x00,
        0x60,
        0x20,
        0x3e,
        0x3d,
        0x60,
        0x20,
        0x01,
        0x60,
        0x00,
        (byte) 0xf3
    };

    // Measures the gas consumed by the MODEXP STATICCALL and returns it as a 32-byte word.
    // CALLDATASIZE PUSH1 0 PUSH1 0 CALLDATACOPY
    // GAS
    // PUSH1 0 PUSH1 0 CALLDATASIZE PUSH1 0 PUSH1 5 GAS STATICCALL POP
    // GAS SWAP1 SUB
    // PUSH1 0 MSTORE PUSH1 0x20 PUSH1 0 RETURN
    private static final byte[] GAS_RUNTIME = {
        0x36,
        0x60,
        0x00,
        0x60,
        0x00,
        0x37,
        0x5a,
        0x60,
        0x00,
        0x60,
        0x00,
        0x36,
        0x60,
        0x00,
        0x60,
        0x05,
        0x5a,
        (byte) 0xfa,
        0x50,
        0x5a,
        (byte) 0x90,
        0x03,
        0x60,
        0x00,
        0x52,
        0x60,
        0x20,
        0x60,
        0x00,
        (byte) 0xf3
    };

    private EmbeddedEthereum evm;

    @BeforeEach
    public void setUp() {
        evm = OsakaEvmTestSupport.newEmbeddedEthereum();
    }

    /** Builds MODEXP calldata: baseLen | expLen | modLen | base | exp | mod. */
    private static byte[] modExpInput(
            final int baseLen,
            final int expLen,
            final int modLen,
            final byte[] base,
            final byte[] exp,
            final byte[] mod) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(OsakaEvmTestSupport.word(BigInteger.valueOf(baseLen)));
        out.writeBytes(OsakaEvmTestSupport.word(BigInteger.valueOf(expLen)));
        out.writeBytes(OsakaEvmTestSupport.word(BigInteger.valueOf(modLen)));
        out.writeBytes(base);
        out.writeBytes(exp);
        out.writeBytes(mod);
        return out.toByteArray();
    }

    @Test
    public void computesModularExponentiation() {
        // 3 ^ 2 mod 5 = 4
        final String contract = OsakaEvmTestSupport.deployRuntime(evm, CALL_RUNTIME);
        final byte[] input =
                modExpInput(1, 1, 1, new byte[] {0x03}, new byte[] {0x02}, new byte[] {0x05});

        final byte[] out =
                Numeric.hexStringToByteArray(OsakaEvmTestSupport.call(evm, contract, input));

        // word0 = success flag, remaining bytes = precompile output (modLen = 1 byte).
        assertEquals(BigInteger.ONE, new BigInteger(1, java.util.Arrays.copyOfRange(out, 0, 32)));
        assertEquals(1, out.length - 32);
        assertEquals(0x04, out[32] & 0xff);
    }

    @Test
    public void inputWithinBoundsSucceeds() {
        // EIP-7823: modulus length of exactly 1024 bytes is allowed.
        final String contract = OsakaEvmTestSupport.deployRuntime(evm, CALL_RUNTIME);
        final byte[] mod = new byte[1024];
        mod[1023] = 0x05;
        final byte[] input = modExpInput(1, 1, 1024, new byte[] {0x03}, new byte[] {0x02}, mod);

        final byte[] out =
                Numeric.hexStringToByteArray(OsakaEvmTestSupport.call(evm, contract, input));

        assertEquals(BigInteger.ONE, new BigInteger(1, java.util.Arrays.copyOfRange(out, 0, 32)));
    }

    @Test
    public void inputExceedingBoundsIsRejected() {
        // EIP-7823: modulus length of 1025 bytes exceeds the 1024-byte cap, so the call fails.
        final String contract = OsakaEvmTestSupport.deployRuntime(evm, CALL_RUNTIME);
        final byte[] input =
                modExpInput(1, 1, 1025, new byte[] {0x03}, new byte[] {0x02}, new byte[0]);

        final byte[] out =
                Numeric.hexStringToByteArray(OsakaEvmTestSupport.call(evm, contract, input));

        // Only the success word is returned, and it must signal failure.
        assertEquals(32, out.length);
        assertEquals(BigInteger.ZERO, new BigInteger(1, out));
    }

    @Test
    public void tinyModExpMeetsOsakaGasFloor() {
        // EIP-7883 raises the minimum MODEXP cost to 500 gas (pre-Osaka it was 200).
        final String contract = OsakaEvmTestSupport.deployRuntime(evm, GAS_RUNTIME);
        final byte[] input =
                modExpInput(1, 1, 1, new byte[] {0x03}, new byte[] {0x02}, new byte[] {0x05});

        final long gasUsed =
                Numeric.toBigInt(OsakaEvmTestSupport.call(evm, contract, input)).longValueExact();

        // gasUsed = STATICCALL overhead (~120) + MODEXP cost. With the 500-gas floor this clears
        // 500; a pre-Osaka 200-gas floor would land well below.
        assertTrue(gasUsed >= 500, "expected MODEXP cost to meet EIP-7883 floor, got " + gasUsed);
        assertTrue(gasUsed < 2000, "unexpectedly large MODEXP cost: " + gasUsed);
    }
}
