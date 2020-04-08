package blockchaincode.fabric;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.UpgradeProposalRequest;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.slf4j.Logger;

import blockchaincode.utils.ByteArryUtil;
import blockchaincode.utils.JsonParseUtil;

import com.google.protobuf.InvalidProtocolBufferException;
/**
 * Fabric客戶端
 * @author WangSong
 *
 */
public class FabricClient {
	private Logger log = org.slf4j.LoggerFactory.getLogger(FabricClient.class);
	private HFClient hfClient;

	//初始化客户端
	public FabricClient(UserContext userContext) throws IllegalAccessException,
			InvocationTargetException, InvalidArgumentException,
			InstantiationException, NoSuchMethodException, CryptoException,
			ClassNotFoundException {
		hfClient = HFClient.createNewInstance();
		CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
		hfClient.setCryptoSuite(cryptoSuite);
		hfClient.setUserContext(userContext);
	}

	public HFClient getHfClient() {
		return hfClient;
	}
	
	/**
	 * @description 创建channel
	 * @param channelName
	 *            channel的名字
	 * @param order
	 *            order的信息
	 * @param txPath
	 *            创建channel所需的tx文件
	 * @return Channel
	 * @throws IOException
	 * @throws InvalidArgumentException
	 * @throws TransactionException
	 */
	public Channel createChannel(String channelName, Orderer order,
			String txPath) throws IOException, InvalidArgumentException,
			TransactionException {
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(
				new File(txPath));
		return hfClient.newChannel(channelName, order, channelConfiguration,
				hfClient.getChannelConfigurationSignature(channelConfiguration,
						hfClient.getUserContext()));
	}

	/**
	 * @description 安装合约
	 * @param lang
	 *            合约开发语言
	 * @param chaincodeName
	 *            合约名称
	 * @param chaincodeVersion
	 *            合约版本
	 * @param chaincodeLocation
	 *            合约的目录路径
	 * @param chaincodePath
	 *            合约的文件夹
	 * @param peers
	 *            安装的peers 节点
	 * @throws InvalidArgumentException
	 * @throws ProposalException
	 */
	public void installChaincode(TransactionRequest.Type lang,
			String chaincodeName, String chaincodeVersion,
			String chaincodeLocation, String chaincodePath, List<Peer> peers)
			throws InvalidArgumentException, ProposalException {
		InstallProposalRequest installProposalRequest = hfClient
				.newInstallProposalRequest();
		ChaincodeID.Builder builder = ChaincodeID.newBuilder()
				.setName(chaincodeName).setVersion(chaincodeVersion);
		installProposalRequest.setChaincodeLanguage(lang);
		installProposalRequest.setChaincodeID(builder.build());
		installProposalRequest.setChaincodeSourceLocation(new File(
				chaincodeLocation));
		installProposalRequest.setChaincodePath(chaincodePath);
		Collection<ProposalResponse> responses = hfClient.sendInstallProposal(
				installProposalRequest, peers);
		for (ProposalResponse response : responses) {
			if (response.getStatus().getStatus() == 200) {
				log.info("{} installed sucess", response.getPeer().getName());
			} else {
			log.error("{} installed fail", response.getMessage());
			}
		}
	}

	/**
	 * @description 合约的实例化
	 * @param channelName
	 * @param lang
	 * @param chaincodeName
	 * @param chaincodeVersion
	 * @param order
	 * @param peer
	 * @param funcName
	 *            合约实例化执行的函数
	 * @param args
	 *            合约实例化执行的参数
	 * @throws TransactionException
	 * @throws ProposalException
	 * @throws InvalidArgumentException
	 */
	public void initChaincode(String channelName, TransactionRequest.Type lang,
			String chaincodeName, String chaincodeVersion, Orderer order,
			Peer peer, String funcName, String args[])
			throws TransactionException, ProposalException,
			InvalidArgumentException {
		Channel channel = getChannel(channelName);
		channel.addPeer(peer);
		channel.addOrderer(order);
		channel.initialize();
		InstantiateProposalRequest instantiateProposalRequest = hfClient
				.newInstantiationProposalRequest();
		instantiateProposalRequest.setArgs(args);
		instantiateProposalRequest.setFcn(funcName);
		instantiateProposalRequest.setChaincodeLanguage(lang);
		ChaincodeID.Builder builder = ChaincodeID.newBuilder()
				.setName(chaincodeName).setVersion(chaincodeVersion);
		instantiateProposalRequest.setChaincodeID(builder.build());
		Collection<ProposalResponse> responses = channel
				.sendInstantiationProposal(instantiateProposalRequest);
		for (ProposalResponse response : responses) {
			if (response.getStatus().getStatus() == 200) {
			log.info("{} init sucess", response.getPeer().getName());
			} else {
				log.error("{} init fail", response.getMessage());
			}
		}
		channel.sendTransaction(responses);
	}

	/**
	 * @description 合约的升级
	 * @param channelName
	 * @param lang
	 * @param chaincodeName
	 * @param chaincodeVersion
	 * @param order
	 * @param peer
	 * @param funcName
	 * @param args
	 * @throws TransactionException
	 * @throws ProposalException
	 * @throws InvalidArgumentException
	 * @throws IOException
	 * @throws ChaincodeEndorsementPolicyParseException
	 */
	public void upgradeChaincode(String channelName,
			TransactionRequest.Type lang, String chaincodeName,
			String chaincodeVersion, Orderer order, Peer peer, String funcName,
			String args[]) throws TransactionException, ProposalException,
			InvalidArgumentException, IOException,
			ChaincodeEndorsementPolicyParseException {
		Channel channel = getChannel(channelName);
		channel.addPeer(peer);
		channel.addOrderer(order);
		channel.initialize();
		UpgradeProposalRequest upgradeProposalRequest = hfClient
				.newUpgradeProposalRequest();
		upgradeProposalRequest.setArgs(args);
		upgradeProposalRequest.setFcn(funcName);
		upgradeProposalRequest.setChaincodeLanguage(lang);
		ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
		chaincodeEndorsementPolicy
				.fromYamlFile(new File(
						"E:\\chaincode\\src\\basicInfo\\chaincodeendorsementpolicy.yaml"));
		upgradeProposalRequest
				.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
		ChaincodeID.Builder builder = ChaincodeID.newBuilder()
				.setName(chaincodeName).setVersion(chaincodeVersion);
		upgradeProposalRequest.setChaincodeID(builder.build());
		Collection<ProposalResponse> responses = channel
				.sendUpgradeProposal(upgradeProposalRequest);
		for (ProposalResponse response : responses) {
			if (response.getStatus().getStatus() == 200) {
				log.info("{} upgrade sucess", response.getPeer().getName());
			} else {
			log.error("{} upgrade fail", response.getMessage());
			}
		}
		channel.sendTransaction(responses);
	}

	/**
	 * @description 合约的调用
	 * @param channelName
	 * @param lang
	 * @param chaincodeName
	 * @param order
	 * @param peers
	 * @param funcName
	 *            合约调用执行的函数名称
	 * @param args
	 *            合约调用执行的参数
	 * @throws TransactionException
	 * @throws ProposalException
	 * @throws InvalidArgumentException
	 */
	public void invoke(String channelName, TransactionRequest.Type lang,
			String chaincodeName, Orderer order, List<Peer> peers,
			String funcName, String args[]) throws TransactionException,
			ProposalException, InvalidArgumentException {
		Channel channel = getChannel(channelName);
		channel.addOrderer(order);
		for (Peer p : peers) {
			channel.addPeer(p);
		}
		channel.initialize();
		TransactionProposalRequest transactionProposalRequest = hfClient
				.newTransactionProposalRequest();
		transactionProposalRequest.setChaincodeLanguage(lang);
		transactionProposalRequest.setArgs(args);
		transactionProposalRequest.setFcn(funcName);
		ChaincodeID.Builder builder = ChaincodeID.newBuilder().setName(
				chaincodeName);
		transactionProposalRequest.setChaincodeID(builder.build());
		Collection<ProposalResponse> responses = channel
				.sendTransactionProposal(transactionProposalRequest, peers);
		for (ProposalResponse response : responses) {
			if (response.getStatus().getStatus() == 200) {
				log.info("{} invoke proposal {} sucess", response.getPeer()
						.getName(), funcName);
				System.out.println("链块操作成功！");
			} else {
				String logArgs[] = { response.getMessage(), funcName,
						response.getPeer().getName() };
				System.out.println("链块操作失败：" + response.getMessage());
				log.error("{} invoke proposal {} fail on {}", logArgs);
				throw new RuntimeException(response.getMessage());
			}
		}
		channel.sendTransaction(responses);
	}

	/**
	 * @description 合约的查询
	 * @param peers
	 * @param channelName
	 * @param lang
	 * @param chaincodeName
	 * @param funcName
	 * @param args
	 * @return
	 * @throws TransactionException
	 * @throws InvalidArgumentException
	 * @throws ProposalException
	 * @throws InvalidProtocolBufferException 
	 */
	public Map<Integer,Object> queryChaincode(List<Peer> peers, String channelName,
			TransactionRequest.Type lang, String chaincodeName,
			String funcName, String args[]) throws TransactionException,
			InvalidArgumentException, ProposalException, InvalidProtocolBufferException {
		Channel channel = getChannel(channelName);
		for (Peer p : peers) {
			channel.addPeer(p);
		}
		channel.initialize();
		Map<Integer,Object> map = new HashMap<Integer,Object>();
		QueryByChaincodeRequest queryByChaincodeRequest = hfClient
				.newQueryProposalRequest();
		ChaincodeID.Builder builder = ChaincodeID.newBuilder().setName(
				chaincodeName);
		queryByChaincodeRequest.setChaincodeID(builder.build());
 		queryByChaincodeRequest.setArgs(args);
		queryByChaincodeRequest.setFcn(funcName);
		queryByChaincodeRequest.setChaincodeLanguage(lang);
	
		Collection<ProposalResponse> responses = channel
				.queryByChaincode(queryByChaincodeRequest);
		
		for (ProposalResponse response : responses) {
			//成功查询到信息
			if (response.getStatus().getStatus() == 200) {
				log.info("data is {}", response.getProposalResponse()
						.getResponse().getPayload());
				map.put(response.getStatus().getStatus(), new String(response
						.getProposalResponse().getResponse().getPayload()
						.toByteArray()));
				return map;
			} else {
				//未查询到信息
				log.error("data get error {}", response.getMessage());
				map.put(response.getStatus().getStatus(), response.getMessage());
				return map;
			}
		}
		map.put(404, "404");
		return map;
	}

	/**
	 * @description 获取orderer节点
	 * @param name
	 * @param grpcUrl
	 * @param tlsFilePath
	 * @return
	 * @throws InvalidArgumentException
	 */
	public Orderer getOrderer(String name, String grpcUrl, String tlsFilePath)
			throws InvalidArgumentException {
		Properties properties = new Properties();
		properties.setProperty("pemFile", tlsFilePath);
		Orderer orderer = hfClient.newOrderer(name, grpcUrl, properties);
		return orderer;
	}

	/**
	 * @description 获取peer节点
	 * @param name
	 * @param grpcUrl
	 * @param tlsFilePath
	 * @return
	 * @throws InvalidArgumentException
	 */
	public Peer getPeer(String name, String grpcUrl, String tlsFilePath)
			throws InvalidArgumentException {
		Properties properties = new Properties();
		properties.setProperty("pemFile", tlsFilePath);
		Peer peer = hfClient.newPeer(name, grpcUrl, properties);
		return peer;
	}

	/**
	 * @description 获取已有的channel
	 * @param channelName
	 * @return
	 * @throws InvalidArgumentException
	 * @throws TransactionException
	 * @throws ProposalException
	 */
	public synchronized Channel getChannel(String channelName)
			throws InvalidArgumentException, TransactionException,
			ProposalException {
		Channel channel = hfClient.getChannel(channelName);
		if (channel == null) {
			channel =  hfClient.newChannel(channelName);
		}
		return channel;
	}
	
	/**
	 * 当前区块信息查询
	 * @param peers					peer网络
	 * @param channelName			通道名·
	 * @param lang						链码语言类型
	 * @param chaincodeName		链码名称
	 * @param funcName				链码中的方法名
	 * @param args						链码中传入方法的参数
	 * @return
	 * @throws Exception
	 */
	public Map<Integer,String> queryBlockInfo(List<Peer> peers, String channelName,
			TransactionRequest.Type lang, String chaincodeName,String funcName, String args[]) throws Exception{
		//当前channel通道
		Channel channel = getChannel(channelName);
		for (Peer p : peers) {
			channel.addPeer(p);
		}
		channel.initialize();
		Map<Integer,String> map = new HashMap<Integer,String>();
		QueryByChaincodeRequest queryByChaincodeRequest = hfClient
				.newQueryProposalRequest();
		ChaincodeID.Builder builder = ChaincodeID.newBuilder().setName(
				chaincodeName);
		queryByChaincodeRequest.setChaincodeID(builder.build());
 		queryByChaincodeRequest.setArgs(args);
		queryByChaincodeRequest.setFcn(funcName);
		queryByChaincodeRequest.setChaincodeLanguage(lang);
		//调用链码方法，返回符合条件的数据
		Collection<ProposalResponse> responses = channel
				.queryByChaincode(queryByChaincodeRequest);
		//循环遍历方法返回值
		for (ProposalResponse response : responses) {
			//成功查询到信息
			if (response.getStatus().getStatus() == 200) {
				log.info("data is {}", response.getProposalResponse()
						.getResponse().getPayload());
				map.put(response.getStatus().getStatus(), new String(response
						.getProposalResponse().getResponse().getPayload()
						.toByteArray()));
				/** 封装区块信息（区块号、当前块hash、前块hash ... ）**/
				String result = String.valueOf(map.get(200));//通过这个得到交易hash
				Map<String, Object> resultMap = JsonParseUtil.json2Map(result);
				Object historys = resultMap.get("Historys");
				List<Map<String,Object>> resultList = new ArrayList<Map<String,Object>>();
				//当前数据没有被修改的情况（单条数据，history为空 ）
				if(null == historys){
					BlockchainInfo chainInfo = channel.queryBlockchainInfo();
					String channelId = channel.getName();
					String dataHash = new String(ByteArryUtil.toHexString(chainInfo.getCurrentBlockHash()));
					String previousHash = new String(ByteArryUtil.toHexString(chainInfo.getPreviousBlockHash()));
					long transactionCount = chainInfo.getHeight();
					long blockNumber = chainInfo.getBlockchainInfo().getHeight();
					//封装到map中
					Map<String,Object> eduMap = new HashMap<String,Object>();
					eduMap.put("blockNumber", blockNumber);
					eduMap.put("dataHash", dataHash);
					eduMap.put("previousHash", previousHash);
					eduMap.put("channelId", channelId);
					eduMap.put("transactionCount", transactionCount);
					eduMap.put("createdDate", resultMap.get("createdDate"));
					//添加map到最终返回的list
					resultList.add(eduMap);
				}
				//当前数据被修改过
				else{
					List<Map<String, Object>> lists = JsonParseUtil.parseJSON2List(historys.toString());
					for(Map<String, Object> eduMap : lists){
						String txId = (String)eduMap.get("TxId");//获取到交易hash
						if(null == txId)	
							continue;
						BlockInfo blockInfo = channel.queryBlockByTransactionID(txId);//通过交易hash得到当前块信息
						long blockNumber = blockInfo.getBlockNumber();//区块号
						String dataHash = new String(ByteArryUtil.toHexString(blockInfo.getDataHash()));//当前区块hash
						String previousHash = new String(ByteArryUtil.toHexString(blockInfo.getPreviousHash()));//前区块hash
						String channelId = blockInfo.getChannelId();//当前channel
						int transactionCount = blockInfo.getTransactionCount();//背书交易数
						//封装到map中
						Map<String,Object> hisMap = new HashMap<String,Object>();
						hisMap.put("blockNumber", blockNumber);
						hisMap.put("dataHash", dataHash);
						hisMap.put("previousHash", previousHash);
						hisMap.put("channelId", channelId);
						hisMap.put("transactionCount", transactionCount);
						//存放交易时间
						Object currentData = eduMap.get("BlockDataEntity");
						Map<String, Object> currentMap = JsonParseUtil.json2Map(String.valueOf(currentData));
						hisMap.put("createdDate", currentMap.get("createdDate"));//交易时间
						//添加map到最终返回的list
						resultList.add(hisMap);
					}
					//把存放最终结果信息的list转换成json格式string
					String blockValue = JsonParseUtil.list2String(resultList);
					//覆盖之前的值
					map.put(response.getStatus().getStatus(), blockValue);
				}
				return map;
			} else {
				//未查询到信息
				log.error("data get error {}", response.getMessage());
				map.put(response.getStatus().getStatus(), response.getMessage());
				return map;
			}
		}
		map.put(404, "404");
		return map;
	}
	
}
