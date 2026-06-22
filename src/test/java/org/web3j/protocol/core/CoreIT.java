/*
 * Copyright 2019 Web3 Labs Ltd.
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
package org.web3j.protocol.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.hyperledger.besu.evm.tracing.OperationTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.evm.Configuration;
import org.web3j.evm.EmbeddedWeb3jService;
import org.web3j.evm.PassthroughTracer;
import org.web3j.generated.Fibonacci;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;

import static org.junit.jupiter.api.Assertions.*;

/** JSON-RPC 2.0 Integration Tests. */
public class CoreIT {

    private Web3j web3j;

    private final IntegrationTestConfig config = new TestnetConfig();
    private Credentials credentials;

    private TransactionReceipt transactionReceipt;

    public CoreIT() {}

    @BeforeEach
    public void setUp() throws Exception {
        credentials = WalletUtils.loadCredentials("Password123", "resources/test-wallet.json");
        final OperationTracer operationTracer = new PassthroughTracer();

        final Configuration configuration =
                new Configuration(new Address(credentials.getAddress()), 10);

        web3j = Web3j.build(new EmbeddedWeb3jService(configuration, operationTracer));

        transactionReceipt =
                Transfer.sendFunds(
                                web3j,
                                credentials,
                                "0x2dfBf35bb7c3c0A466A6C48BEBf3eF7576d3C420",
                                new BigDecimal("1"),
                                Convert.Unit.ETHER)
                        .send();

        if (!transactionReceipt.isStatusOK()) throw new RuntimeException("Failed transaction");
    }

    @Test
    public void testWeb3ClientVersion() throws Exception {
        Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
        String clientVersion = web3ClientVersion.getWeb3ClientVersion();
        System.out.println("Ethereum client version: " + clientVersion);
        assertFalse(clientVersion.isEmpty());
    }

    @Disabled("Missing RPC")
    @Test
    public void testWeb3Sha3() throws Exception {
        Web3Sha3 web3Sha3 = web3j.web3Sha3("0x68656c6c6f20776f726c64").send();
        assertEquals(
                web3Sha3.getResult(),
                ("0x47173285a8d7341e5e972fc677286384f802f8ef42a5ec5f03bbfa254cb01fad"));
    }

    @Test
    public void testNetVersion() throws Exception {
        NetVersion netVersion = web3j.netVersion().send();
        assertFalse(netVersion.getNetVersion().isEmpty());
    }

    @Disabled("Missing RPC")
    @Test
    public void testNetListening() throws Exception {
        NetListening netListening = web3j.netListening().send();
        assertTrue(netListening.isListening());
    }

    @Disabled("Missing RPC")
    @Test
    public void testNetPeerCount() throws Exception {
        NetPeerCount netPeerCount = web3j.netPeerCount().send();
        assertEquals(1, netPeerCount.getQuantity().signum());
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthProtocolVersion() throws Exception {
        EthProtocolVersion ethProtocolVersion = web3j.ethProtocolVersion().send();
        assertFalse(ethProtocolVersion.getProtocolVersion().isEmpty());
    }

    @Test
    public void testEthSyncing() throws Exception {
        EthSyncing ethSyncing = web3j.ethSyncing().send();
        assertNotNull(ethSyncing.getResult());
    }

    @Disabled // This will return null unless mining is enabled. Todo: use admin APIs to check if
    // mining is enabled and enable/disable this test accordingly.
    @Test
    public void testEthCoinbase() throws Exception {
        EthCoinbase ethCoinbase = web3j.ethCoinbase().send();
        assertNotNull(ethCoinbase.getAddress());
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthMining() throws Exception {
        EthMining ethMining = web3j.ethMining().send();
        assertNotNull(ethMining.getResult());
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthHashrate() throws Exception {
        EthHashrate ethHashrate = web3j.ethHashrate().send();
        assertEquals(ethHashrate.getHashrate(), (BigInteger.ZERO));
    }

    @Test
    public void testEthGasPrice() throws Exception {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        assertEquals(1, ethGasPrice.getGasPrice().signum());
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthAccounts() throws Exception {
        EthAccounts ethAccounts = web3j.ethAccounts().send();
        assertNotNull(ethAccounts.getAccounts());
    }

    @Test
    public void testEthBlockNumber() throws Exception {
        EthBlockNumber ethBlockNumber = web3j.ethBlockNumber().send();
        assertTrue(ethBlockNumber.getBlockNumber().longValue() >= 0);
    }

    @Disabled("Missing test setup")
    @Test
    public void testEthGetBalance() throws Exception {
        EthGetBalance ethGetBalance =
                web3j.ethGetBalance(config.validAccount(), DefaultBlockParameter.valueOf("latest"))
                        .send();
        assertEquals(1, ethGetBalance.getBalance().signum());
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthGetStorageAt() throws Exception {
        EthGetStorageAt ethGetStorageAt =
                web3j.ethGetStorageAt(
                                config.validContractAddress(),
                                BigInteger.valueOf(0),
                                DefaultBlockParameter.valueOf("latest"))
                        .send();
        assertEquals(ethGetStorageAt.getData(), (config.validContractAddressPositionZero()));
    }

    @Test
    public void testEthGetTransactionCount() throws Exception {
        EthGetTransactionCount ethGetTransactionCount =
                web3j.ethGetTransactionCount(
                                config.validAccount(), DefaultBlockParameter.valueOf("latest"))
                        .send();
        assertEquals(1, ethGetTransactionCount.getTransactionCount().signum());
    }

    @Test
    public void testEthGetBlockTransactionCountByHash() throws Exception {
        EthGetBlockTransactionCountByHash ethGetBlockTransactionCountByHash =
                web3j.ethGetBlockTransactionCountByHash(config.validBlockHash()).send();
        assertEquals(
                ethGetBlockTransactionCountByHash.getTransactionCount(),
                (config.validBlockTransactionCount()));
    }

    @Test
    public void testEthGetBlockTransactionCountByNumber() throws Exception {
        EthGetBlockTransactionCountByNumber ethGetBlockTransactionCountByNumber =
                web3j.ethGetBlockTransactionCountByNumber(
                                DefaultBlockParameter.valueOf(config.validBlock()))
                        .send();
        assertEquals(
                ethGetBlockTransactionCountByNumber.getTransactionCount(),
                (config.validBlockTransactionCount()));
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthGetUncleCountByBlockHash() throws Exception {
        EthGetUncleCountByBlockHash ethGetUncleCountByBlockHash =
                web3j.ethGetUncleCountByBlockHash(config.validBlockHash()).send();
        assertEquals(ethGetUncleCountByBlockHash.getUncleCount(), (config.validBlockUncleCount()));
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthGetUncleCountByBlockNumber() throws Exception {
        EthGetUncleCountByBlockNumber ethGetUncleCountByBlockNumber =
                web3j.ethGetUncleCountByBlockNumber(DefaultBlockParameter.valueOf("latest")).send();
        assertEquals(
                ethGetUncleCountByBlockNumber.getUncleCount(), (config.validBlockUncleCount()));
    }

    @Test
    public void testEthGetCode() throws Exception {
        final Fibonacci send =
                Fibonacci.deploy(web3j, credentials, new DefaultGasProvider()).send();
        EthGetCode ethGetCode =
                web3j.ethGetCode(send.getContractAddress(), DefaultBlockParameter.valueOf("latest"))
                        .send();
        assertEquals(ethGetCode.getCode(), (config.validContractCode()));
    }

    @Test
    public void testEthGetCodeEmpty() throws Exception {
        EthGetCode ethGetCode =
                web3j.ethGetCode(credentials.getAddress(), DefaultBlockParameter.valueOf("latest"))
                        .send();
        assertEquals(ethGetCode.getCode(), "0x");
    }

    @Disabled // TODO: Once account unlock functionality is available
    @Test
    public void testEthSign() throws Exception {
        // EthSign ethSign = web3j.ethSign();
    }

    @Disabled // TODO: Once account unlock functionality is available
    @Test
    public void testEthSendTransaction() throws Exception {
        EthSendTransaction ethSendTransaction =
                web3j.ethSendTransaction(config.buildTransaction(web3j)).send();
        assertFalse(ethSendTransaction.getTransactionHash().isEmpty());
    }

    @Disabled // TODO: Once account unlock functionality is available
    @Test
    public void testEthSendRawTransaction() throws Exception {}

    @Test
    public void testEthCall() throws Exception {
        EthCall ethCall =
                web3j.ethCall(
                                config.buildTransaction(web3j),
                                DefaultBlockParameter.valueOf("latest"))
                        .send();

        assertEquals(DefaultBlockParameterName.LATEST.getValue(), ("latest"));
        assertEquals(ethCall.getValue(), ("0x"));
    }

    @Test
    public void testEthEstimateGas() throws Exception {
        EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(config.buildTransaction(web3j)).send();
        assertEquals(1, ethEstimateGas.getAmountUsed().signum());
    }

    @Test
    public void testEthGetBlockByHashReturnHashObjects() throws Exception {
        EthBlock ethBlock =
                web3j.ethGetBlockByHash(transactionReceipt.getBlockHash(), false).send();

        EthBlock.Block block = ethBlock.getBlock();
        assertNotNull(ethBlock.getBlock());
        assertEquals(block.getNumber(), transactionReceipt.getBlockNumber());
        assertEquals(block.getTransactions().size(), 1);
    }

    @Test
    public void testEthGetBlockByHashReturnFullTransactionObjects() throws Exception {
        EthBlock ethBlock = web3j.ethGetBlockByHash(transactionReceipt.getBlockHash(), true).send();

        EthBlock.Block block = ethBlock.getBlock();
        assertNotNull(ethBlock.getBlock());
        assertEquals(block.getNumber(), transactionReceipt.getBlockNumber());
        assertEquals(block.getTransactions().size(), 1);
    }

    @Test
    public void testEthGetBlockByNumberReturnHashObjects() throws Exception {
        EthBlock ethBlock =
                web3j.ethGetBlockByNumber(
                                DefaultBlockParameter.valueOf(transactionReceipt.getBlockNumber()),
                                false)
                        .send();

        EthBlock.Block block = ethBlock.getBlock();
        assertNotNull(ethBlock.getBlock());
        assertEquals(block.getNumber(), transactionReceipt.getBlockNumber());
        assertEquals(block.getTransactions().size(), 1);
    }

    @Test
    public void testEthGetBlockByNumberReturnTransactionObjects() throws Exception {
        EthBlock ethBlock =
                web3j.ethGetBlockByNumber(
                                DefaultBlockParameter.valueOf(transactionReceipt.getBlockNumber()),
                                true)
                        .send();

        EthBlock.Block block = ethBlock.getBlock();
        assertNotNull(ethBlock.getBlock());
        assertEquals(block.getNumber(), transactionReceipt.getBlockNumber());
        assertEquals(block.getTransactions().size(), 1);
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthGetTransactionByHash() throws Exception {
        EthTransaction ethTransaction =
                web3j.ethGetTransactionByHash(config.validTransactionHash()).send();
        assertTrue(ethTransaction.getTransaction().isPresent());
        Transaction transaction = ethTransaction.getTransaction().get();
        assertEquals(transaction.getBlockHash(), (config.validBlockHash()));
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthGetTransactionByBlockHashAndIndex() throws Exception {
        BigInteger index = BigInteger.ONE;

        EthTransaction ethTransaction =
                web3j.ethGetTransactionByBlockHashAndIndex(config.validBlockHash(), index).send();
        assertTrue(ethTransaction.getTransaction().isPresent());
        Transaction transaction = ethTransaction.getTransaction().get();
        assertEquals(transaction.getBlockHash(), (config.validBlockHash()));
        assertEquals(transaction.getTransactionIndex(), (index));
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthGetTransactionByBlockNumberAndIndex() throws Exception {
        BigInteger index = BigInteger.ONE;

        EthTransaction ethTransaction =
                web3j.ethGetTransactionByBlockNumberAndIndex(
                                DefaultBlockParameter.valueOf(config.validBlock()), index)
                        .send();
        assertTrue(ethTransaction.getTransaction().isPresent());
        Transaction transaction = ethTransaction.getTransaction().get();
        assertEquals(transaction.getBlockHash(), (config.validBlockHash()));
        assertEquals(transaction.getTransactionIndex(), (index));
    }

    @Disabled("Missing test setup")
    @Test
    public void testEthGetTransactionReceipt() throws Exception {
        EthGetTransactionReceipt ethGetTransactionReceipt =
                web3j.ethGetTransactionReceipt(config.validTransactionHash()).send();
        assertTrue(ethGetTransactionReceipt.getTransactionReceipt().isPresent());
        TransactionReceipt transactionReceipt =
                ethGetTransactionReceipt.getTransactionReceipt().get();
        assertEquals(transactionReceipt.getTransactionHash(), (config.validTransactionHash()));
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthGetUncleByBlockHashAndIndex() throws Exception {
        EthBlock ethBlock =
                web3j.ethGetUncleByBlockHashAndIndex(config.validUncleBlockHash(), BigInteger.ZERO)
                        .send();
        assertNotNull(ethBlock.getBlock());
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthGetUncleByBlockNumberAndIndex() throws Exception {
        EthBlock ethBlock =
                web3j.ethGetUncleByBlockNumberAndIndex(
                                DefaultBlockParameter.valueOf(config.validUncleBlock()),
                                BigInteger.ZERO)
                        .send();
        assertNotNull(ethBlock.getBlock());
    }

    @Disabled // The method eth_GetCompilers does not exist/is not available (it was removed)
    @Test
    public void testEthGetCompilers() throws Exception {
        EthGetCompilers ethGetCompilers = web3j.ethGetCompilers().send();
        assertNotNull(ethGetCompilers.getCompilers());
    }

    @Disabled // The method eth_compileLLL does not exist/is not available (it was removed)
    @Test
    public void testEthCompileLLL() throws Exception {
        EthCompileLLL ethCompileLLL = web3j.ethCompileLLL("(returnlll (suicide (caller)))").send();
        assertFalse(ethCompileLLL.getCompiledSourceCode().isEmpty());
    }

    @Disabled // The method eth_CompileSolidity does not exist/is not available (it was removed)
    @Test
    public void testEthCompileSolidity() throws Exception {
        String sourceCode =
                "pragma solidity ^0.4.0;"
                        + "\ncontract test { function multiply(uint a) returns(uint d) {"
                        + "   return a * 7;   } }"
                        + "\ncontract test2 { function multiply2(uint a) returns(uint d) {"
                        + "   return a * 7;   } }";
        EthCompileSolidity ethCompileSolidity = web3j.ethCompileSolidity(sourceCode).send();
        assertNotNull(ethCompileSolidity.getCompiledSolidity());
        assertEquals(
                ethCompileSolidity.getCompiledSolidity().get("test2").getInfo().getSource(),
                (sourceCode));
    }

    @Disabled // The method eth_compileSerpent does not exist/is not available
    @Test
    public void testEthCompileSerpent() throws Exception {
        EthCompileSerpent ethCompileSerpent = web3j.ethCompileSerpent("/* some serpent */").send();
        assertFalse(ethCompileSerpent.getCompiledSourceCode().isEmpty());
    }

    @Disabled("Missing RPC")
    @Test
    public void testFiltersByFilterId() throws Exception {
        org.web3j.protocol.core.methods.request.EthFilter ethFilter =
                new org.web3j.protocol.core.methods.request.EthFilter(
                        DefaultBlockParameterName.EARLIEST,
                        DefaultBlockParameterName.LATEST,
                        config.validContractAddress());

        String eventSignature = config.encodedEvent();
        ethFilter.addSingleTopic(eventSignature);

        // eth_newFilter
        EthFilter ethNewFilter = web3j.ethNewFilter(ethFilter).send();
        BigInteger filterId = ethNewFilter.getFilterId();

        // eth_getFilterLogs
        EthLog ethFilterLogs = web3j.ethGetFilterLogs(filterId).send();
        List<EthLog.LogResult<?>> filterLogs = ethFilterLogs.getLogs();
        assertFalse(filterLogs.isEmpty());

        // eth_getFilterChanges - nothing will have changed in this interval
        EthLog ethLog = web3j.ethGetFilterChanges(filterId).send();
        assertTrue(ethLog.getLogs().isEmpty());

        // eth_uninstallFilter
        EthUninstallFilter ethUninstallFilter = web3j.ethUninstallFilter(filterId).send();
        assertTrue(ethUninstallFilter.isUninstalled());
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthNewBlockFilter() throws Exception {
        EthFilter ethNewBlockFilter = web3j.ethNewBlockFilter().send();
        assertNotNull(ethNewBlockFilter.getFilterId());
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthNewPendingTransactionFilter() throws Exception {
        EthFilter ethNewPendingTransactionFilter = web3j.ethNewPendingTransactionFilter().send();
        assertNotNull(ethNewPendingTransactionFilter.getFilterId());
    }

    @Disabled("Missing RPC")
    @Test
    public void testEthGetLogs() throws Exception {
        org.web3j.protocol.core.methods.request.EthFilter ethFilter =
                new org.web3j.protocol.core.methods.request.EthFilter(
                        DefaultBlockParameterName.EARLIEST,
                        DefaultBlockParameterName.LATEST,
                        config.validContractAddress());

        ethFilter.addSingleTopic(config.encodedEvent());

        EthLog ethLog = web3j.ethGetLogs(ethFilter).send();
        List<EthLog.LogResult<?>> logs = ethLog.getLogs();
        assertFalse(logs.isEmpty());
    }

    // @Test
    // public void testEthGetWork() throws Exception {
    //     EthGetWork ethGetWork = requestFactory.ethGetWork();
    //     assertNotNull(ethGetWork.getResult());
    // }

    @Test
    public void testEthSubmitWork() throws Exception {}

    @Test
    public void testEthSubmitHashrate() throws Exception {}

    @Test
    public void testDbPutString() throws Exception {}

    @Test
    public void testDbGetString() throws Exception {}

    @Test
    public void testDbPutHex() throws Exception {}

    @Test
    public void testDbGetHex() throws Exception {}

    @Test
    public void testShhPost() throws Exception {}

    @Disabled // The method shh_version does not exist/is not available
    @Test
    public void testShhVersion() throws Exception {
        ShhVersion shhVersion = web3j.shhVersion().send();
        assertNotNull(shhVersion.getVersion());
    }

    @Disabled // The method shh_newIdentity does not exist/is not available
    @Test
    public void testShhNewIdentity() throws Exception {
        ShhNewIdentity shhNewIdentity = web3j.shhNewIdentity().send();
        assertNotNull(shhNewIdentity.getAddress());
    }

    @Test
    public void testShhHasIdentity() throws Exception {}

    @Disabled // The method shh_newIdentity does not exist/is not available
    @Test
    public void testShhNewGroup() throws Exception {
        ShhNewGroup shhNewGroup = web3j.shhNewGroup().send();
        assertNotNull(shhNewGroup.getAddress());
    }

    @Disabled // The method shh_addToGroup does not exist/is not available
    @Test
    public void testShhAddToGroup() throws Exception {}

    @Test
    public void testShhNewFilter() throws Exception {}

    @Test
    public void testShhUninstallFilter() throws Exception {}

    @Test
    public void testShhGetFilterChanges() throws Exception {}

    @Test
    public void testShhGetMessages() throws Exception {}
}
