package blockchaincode.service.impl;

import io.netty.util.internal.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blockchaincode.common.BlockChainConstant;
import blockchaincode.fabric.FabricClient;
import blockchaincode.fabric.UserContext;
import blockchaincode.fabric.UserUtils;
import blockchaincode.service.IHKCoinService;

import com.sunsheen.jfids.das.core.annotation.Bean;

/**
 * 构件币操作方法
 * @author WangSong
 *
 */
@Bean("HKCoinService")
public class HKCoinServiceImpl implements IHKCoinService{
	private static Logger log = LoggerFactory.getLogger(HKCoinServiceImpl.class);

	
	//用户权限认证
	private UserContext authCertificat(String path) throws Exception{
		UserContext userContext = new UserContext();
		userContext.setAffiliation("Org1");
		userContext.setMspId("Org1MSP");
		userContext.setAccount("admin");
		userContext.setName("admin");
		Enrollment enrollment = UserUtils.getEnrollment(path+ File.separator +BlockChainConstant.keyFolderPath,
				BlockChainConstant.keyFileName, path+ File.separator +BlockChainConstant.certFoldePath, BlockChainConstant.certFileName);
		userContext.setEnrollment(enrollment);
		return userContext;
	}
	
	@Override
	public void initAccount(String username, String path) throws Exception {
		FabricClient fabricClient = new FabricClient(authCertificat(path));
		Peer peer0 = fabricClient.getPeer("peer0.org1.example.com",
				"grpcs://peer0.org1.example.com:7051", 
				path+ File.separator +BlockChainConstant.tlsPeerFilePath);
		List<Peer> peers = new ArrayList<>();
		peers.add(peer0);
		Orderer order = fabricClient.getOrderer("orderer.example.com",
				"grpcs://orderer.example.com:7050", path+ File.separator +BlockChainConstant.tlsOrderFilePath);
		String initArgs[] = {username};
		fabricClient.invoke("mychannel", TransactionRequest.Type.GO_LANG,
				"hkcoin", order, peers, "createAccount", initArgs);
	}

	@Override
	public void award(String username, String amount, String path)
			throws Exception {
		FabricClient fabricClient = new FabricClient(authCertificat(path));
		Peer peer0 = fabricClient.getPeer("peer0.org1.example.com",
				"grpcs://peer0.org1.example.com:7051", path+ File.separator +BlockChainConstant.tlsPeerFilePath);
		List<Peer> peers = new ArrayList<>();
		peers.add(peer0);
		Orderer order = fabricClient.getOrderer("orderer.example.com",
				"grpcs://orderer.example.com:7050", path+ File.separator +BlockChainConstant.tlsOrderFilePath);
		//(1) 发送人(2) 接收人(3) 代币名(4)发送代币量
		String initArgs[] = {"sunsheen",username,"HKC",amount};
		fabricClient.invoke("mychannel", TransactionRequest.Type.GO_LANG,
				"hkcoin", order, peers, "transferToken", initArgs);
		
	}
	
	@Override
	public void recycle(String username, String amount, String path)
			throws Exception {
		FabricClient fabricClient = new FabricClient(authCertificat(path));
		Peer peer0 = fabricClient.getPeer("peer0.org1.example.com",
				"grpcs://peer0.org1.example.com:7051", path+ File.separator +BlockChainConstant.tlsPeerFilePath);
		List<Peer> peers = new ArrayList<>();
		peers.add(peer0);
		Orderer order = fabricClient.getOrderer("orderer.example.com",
				"grpcs://orderer.example.com:7050", path+ File.separator +BlockChainConstant.tlsOrderFilePath);
		//(1) 发送人(2) 接收人(3) 代币名(4)发送代币量
		String initArgs[] = {username,"sunsheen","HKC",amount};
		fabricClient.invoke("mychannel", TransactionRequest.Type.GO_LANG,
				"hkcoin", order, peers, "transferToken", initArgs);
		
	}

	@Override
	public Map getUserAccount(String username, String path) throws Exception {
		FabricClient fabricClient = new FabricClient(authCertificat(path));
		Peer peer0 = fabricClient.getPeer("peer0.org1.example.com",
				"grpcs://peer0.org1.example.com:7051", path+ File.separator +BlockChainConstant.tlsPeerFilePath);
		List<Peer> peers = new ArrayList<>();
		peers.add(peer0);
		String initArgs[] = {username,"HKC"};
		Map result = fabricClient.queryChaincode(peers, "mychannel",
				TransactionRequest.Type.GO_LANG, "hkcoin", "balance", initArgs);
		return result;
	}
	
	/**
	 * 创建管理员
	 */
	public void admin(String username,String path) throws Exception{
		FabricClient fabricClient = new FabricClient(authCertificat(path));
		Peer peer0 = fabricClient.getPeer("peer0.org1.example.com",
				"grpcs://peer0.org1.example.com:7051", path+ File.separator +BlockChainConstant.tlsPeerFilePath);
		List<Peer> peers = new ArrayList<>();
		peers.add(peer0);
		Orderer order = fabricClient.getOrderer("orderer.example.com",
				"grpcs://orderer.example.com:7050", path+ File.separator +BlockChainConstant.tlsOrderFilePath);
		List<String> params = new ArrayList<String>();
		if(!StringUtil.isNullOrEmpty(username)){
			params.add(username);
		}
		String initArgs[] = params.toArray(new String[params.size()]);
		fabricClient.invoke("mychannel", TransactionRequest.Type.GO_LANG,
				"hkcoin", order, peers, "initLedger", initArgs);
	}
	
	/**
	 * 初始化币池
	 * @param username
	 * @param path
	 * @throws Exception
	 */
	//(1) 代币全称 (2) 代币简称 (3) 代币总量 (4) 代币生成以后持有人 (5) 是否锁仓
	public void initCurrency(String username,String path) throws Exception{
		FabricClient fabricClient = new FabricClient(authCertificat(path));
		Peer peer0 = fabricClient.getPeer("peer0.org1.example.com",
				"grpcs://peer0.org1.example.com:7051", path+ File.separator +BlockChainConstant.tlsPeerFilePath);
		List<Peer> peers = new ArrayList<>();
		peers.add(peer0);
		Orderer order = fabricClient.getOrderer("orderer.example.com",
				"grpcs://orderer.example.com:7050", path+ File.separator +BlockChainConstant.tlsOrderFilePath);
		String initArgs[] = {"HKCoin","HKC","9999999",username,"false"};
		fabricClient.invoke("mychannel", TransactionRequest.Type.GO_LANG,
				"hkcoin", order, peers, "initCurrency", initArgs);
	}

	@Override
	public Map coinHistory(String path) throws Exception {
		FabricClient fabricClient = new FabricClient(authCertificat(path));
		Peer peer0 = fabricClient.getPeer("peer0.org1.example.com",
				"grpcs://peer0.org1.example.com:7051", path+ File.separator +BlockChainConstant.tlsPeerFilePath);
		List<Peer> peers = new ArrayList<>();
		peers.add(peer0);
		String initArgs[] = {"HKC"};
		Map result = fabricClient.queryChaincode(peers, "mychannel",
				TransactionRequest.Type.GO_LANG, "hkcoin", "tokenHistory", initArgs);
		System.out.println(result);
		return result;
	}

	@Override
	public Map userCoinHistory(String username, String path) throws Exception {
		FabricClient fabricClient = new FabricClient(authCertificat(path));
		Peer peer0 = fabricClient.getPeer("peer0.org1.example.com",
				"grpcs://peer0.org1.example.com:7051", path+ File.separator +BlockChainConstant.tlsPeerFilePath);
		List<Peer> peers = new ArrayList<>();
		peers.add(peer0);
		String initArgs[] = {"HKC",username};
		Map result = fabricClient.queryChaincode(peers, "mychannel",
				TransactionRequest.Type.GO_LANG, "hkcoin", "userTokenHistory", initArgs);
		return result;
	}

}
