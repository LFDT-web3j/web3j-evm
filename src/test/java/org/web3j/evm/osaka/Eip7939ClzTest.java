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

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.web3j.evm.EmbeddedEthereum;
import org.web3j.utils.Numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * EIP-7939: the {@code CLZ} (count leading zeros) opcode {@code 0x1e}, activated at Osaka.
 *
 * <p>A pre-Osaka EVM would treat {@code 0x1e} as an invalid opcode; the fact that these calls
 * return the correct leading-zero counts demonstrates the embedded EVM is running the Osaka
 * schedule.
 */
public class Eip7939ClzTest {

    // Runtime: PUSH1 0; CALLDATALOAD; CLZ; PUSH1 0; MSTORE; PUSH1 32; PUSH1 0; RETURN
    private static final byte[] CLZ_RUNTIME = {
        0x60, 0x00, 0x35, 0x1e, 0x60, 0x00, 0x52, 0x60, 0x20, 0x60, 0x00, (byte) 0xf3
    };

    private EmbeddedEthereum evm;
    private String contract;

    @BeforeEach
    public void setUp() {
        evm = OsakaEvmTestSupport.newEmbeddedEthereum();
        contract = OsakaEvmTestSupport.deployRuntime(evm, CLZ_RUNTIME);
    }

    private long clz(final BigInteger input) {
        final String out = OsakaEvmTestSupport.call(evm, contract, OsakaEvmTestSupport.word(input));
        return Numeric.toBigInt(out).longValueExact();
    }

    @Test
    public void clzOfZeroIs256() {
        assertEquals(256, clz(BigInteger.ZERO));
    }

    @Test
    public void clzOfOneIs255() {
        assertEquals(255, clz(BigInteger.ONE));
    }

    @Test
    public void clzOf256Is247() {
        assertEquals(247, clz(BigInteger.valueOf(256)));
    }

    @Test
    public void clzOfHighestBitIsZero() {
        assertEquals(0, clz(BigInteger.ONE.shiftLeft(255)));
    }

    @Test
    public void clzOfAllOnesIsZero() {
        assertEquals(0, clz(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)));
    }
}
