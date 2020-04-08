package blockchaincode.common;

import java.io.File;

/**
 * 区块链常量
 * @author WangSong
 *
 */
public class BlockChainConstant {
	/**
	 * 密钥文件位置
	 */
	public static final String keyFolderPath = "crypto-config"+File.separator+"peerOrganizations"+File.separator+"org1.example.com"+File.separator+"users"+File.separator+"Admin@org1.example.com"+File.separator+"msp"+File.separator+"keystore";
	
	/**
	 * 密钥文件名
	 */
	public static final String keyFileName = "62490a66d8d5ffa05dc57712c2793b546c929a2fd9ae521cf8d3cde59b807986_sk";
	
	/**
	 * cert文件位置
	 */
	public static final String certFoldePath = "crypto-config"+File.separator+"peerOrganizations"+File.separator+"org1.example.com"+File.separator+"users"+File.separator+"Admin@org1.example.com"+File.separator+"msp"+File.separator+"admincerts";
	
	/**
	 * cert文件名
	 */
	public static final String certFileName = "Admin@org1.example.com-cert.pem";
	
	/**
	 * tlsOrder文件位置
	 */
	public static final String tlsOrderFilePath = "crypto-config"+File.separator+"ordererOrganizations"+File.separator+"example.com"+File.separator+"tlsca"+File.separator+"tlsca.example.com-cert.pem";
	
	/**
	 * tlsOrder名
	 */
	public static final String txfilePath = "test1.tx";
	
	/**
	 * tlsPeer文件位置
	 */
	public static final String tlsPeerFilePath = "crypto-config"+File.separator+"peerOrganizations"+File.separator+"org1.example.com"+File.separator+"peers"+File.separator+"peer0.org1.example.com"+File.separator+"msp"+File.separator+"tlscacerts"+File.separator+"tlsca.org1.example.com-cert.pem";
	
	/**
	 * tlspeer
	 */
	public static final String tlsPeerFilePathAddtion = "crypto-config"+File.separator+"peerOrganizations"+File.separator+"org1.example.com"+File.separator+"tlsca"+File.separator+"tlsca.org1.example.com-cert.pem";
	
	
}
