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

import org.hyperledger.besu.evm.tracing.OperationTracer;

import org.web3j.abi.datatypes.Address;
import org.web3j.evm.Configuration;
import org.web3j.evm.EmbeddedEthereum;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

/**
 * Shared helpers for exercising individual EVM opcodes / precompiles against the embedded Besu EVM
 * running on the Osaka (Fusaka) schedule. Tiny contracts are assembled from raw bytecode, deployed,
 * and invoked via {@code eth_call} so each EIP can be checked in isolation.
 */
public final class OsakaEvmTestSupport {

    private OsakaEvmTestSupport() {}

    public static EmbeddedEthereum newEmbeddedEthereum() {
        return new EmbeddedEthereum(
                new Configuration(Address.DEFAULT, 1_000_000), OperationTracer.NO_TRACING);
    }

    /**
     * Wraps the given runtime bytecode in a minimal constructor that returns it verbatim, so the
     * runtime becomes the deployed contract code.
     */
    public static byte[] deployer(final byte[] runtime) {
        final int len = runtime.length;
        if (len > 0xff) {
            throw new IllegalArgumentException("runtime too large for this helper: " + len);
        }
        // PUSH1 len, PUSH1 0x0c, PUSH1 0x00, CODECOPY, PUSH1 len, PUSH1 0x00, RETURN  (12 bytes)
        final byte[] prefix = {
            0x60,
            (byte) len,
            0x60,
            0x0c,
            0x60,
            0x00,
            0x39,
            0x60,
            (byte) len,
            0x60,
            0x00,
            (byte) 0xf3,
        };
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(prefix);
        out.writeBytes(runtime);
        return out.toByteArray();
    }

    /** Deploys the given runtime bytecode and returns the resulting contract address. */
    public static String deployRuntime(final EmbeddedEthereum evm, final byte[] runtime) {
        final String deployHash =
                evm.processTransaction(
                        Transaction.createContractTransaction(
                                Address.DEFAULT.toString(),
                                BigInteger.ZERO,
                                BigInteger.ZERO,
                                Numeric.toHexString(deployer(runtime))));
        final TransactionReceipt receipt = evm.getTransactionReceipt(deployHash);
        return receipt.getContractAddress();
    }

    /**
     * Performs an {@code eth_call} against the contract with the given calldata, returning output.
     */
    public static String call(
            final EmbeddedEthereum evm, final String contractAddress, final byte[] calldata) {
        final Transaction tx =
                Transaction.createEthCallTransaction(
                        Address.DEFAULT.toString(), contractAddress, Numeric.toHexString(calldata));
        return evm.ethCall(tx);
    }

    /** Performs an {@code eth_call} directly against a precompile address. */
    public static String callAddress(
            final EmbeddedEthereum evm, final String address, final byte[] calldata) {
        final Transaction tx =
                Transaction.createEthCallTransaction(
                        Address.DEFAULT.toString(), address, Numeric.toHexString(calldata));
        return evm.ethCall(tx);
    }

    /** Left-pads a value to a 32-byte big-endian word. */
    public static byte[] word(final BigInteger value) {
        return Numeric.toBytesPadded(value, 32);
    }
}
