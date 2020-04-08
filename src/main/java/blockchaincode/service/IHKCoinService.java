package blockchaincode.service;

import java.util.Map;

/**
 * 构件币服务接口
 * @author WangSong
 *
 */
public interface IHKCoinService {

	/**
	 * 初始币账户 --- 构件用户与币账户绑定
	 * @param username	被操作者
	 * @param path		证书文件根目录
	 */
	void initAccount(String username,String path) throws Exception;
	
	/**
	 * 币操作转出 --- 奖励
	 * @param username	被操作者
	 * @param amount	转移数量
	 * @param path		证书文件根目录
	 */
	void award(String username,String amount,String path) throws Exception;
	
	/**
	 * 币操作收回 --- 惩罚
	 * @param username	被操作者
	 * @param amount	转移数量
	 * @param path		证书文件根目录
	 */
	void recycle(String username,String amount,String path) throws Exception;
	
	/**
	 * 查询用户币账户
	 * @param username
	 * @param path
	 * @return
	 */
	Map getUserAccount(String username,String path) throws Exception;
	
	/**
	 * 查询构件币的所有记录
	 * @return
	 * @throws Exception
	 */
	Map coinHistory(String path) throws Exception;
	
	/**
	 * 查询指定用户构件币的操作
	 * @param username
	 * @param path
	 * @return
	 * @throws Exception
	 */
	Map userCoinHistory(String username,String path) throws Exception;
}
