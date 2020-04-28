package main

import (
	//"math"
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"
	"time"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	"github.com/hyperledger/fabric/protos/peer"
)

// -----------
const TokenKey = "token"
const Admin = "admin"

// Define the Smart Contract structure
type HearkenCoinContract struct {
}

type Msg struct {
	Status  bool   `json:"Status"`
	Code    int    `json:"Code"`
	Message string `json:"Message"`
}

type Currency struct {
	Lock        bool                `json:"Lock"`
	TokenName   string              `json:"TokenName"`
	TokenSymbol string              `json:"TokenSymbol"`
	TotalSupply float64             `json:"TotalSupply"`
	User        map[string]float64  `json:"User"`

	Record      []TransactionRecord `json:"Record"`	//历史信息
}

type TransactionRecord struct {
	From   string  `json:"From"`
	To     string  `json:"To"`
	Amount float64 `json:"Amount"`
	TxId   string  `json:"TxId"`
	CreatedDate string `json:"CreatedDate"`	//创建时间
	Description   string  `json:"Description"` //交易说明
}

type Token struct {
	Currency map[string]Currency `json:"Currency"`
}

type Account struct {
	Name      string             `json:"Name"`
	Frozen    bool               `json:"Frozen"`
	BalanceOf map[string]float64 `json:"BalanceOf"`
}

/**
转账
 */
func (token *Token) transfer(_from *Account, _to *Account, _currency string, _value float64) []byte {
	var rev []byte
	if token.Currency[_currency].Lock {
		msg := &Msg{Status: false, Code: 0, Message: "锁仓状态，停止一切转账活动"}
		rev, _ = json.Marshal(msg)
		return rev
	}
	if _from.Frozen {
		msg := &Msg{Status: false, Code: 0, Message: "From 账号冻结"}
		rev, _ = json.Marshal(msg)
		return rev
	}
	if _to.Frozen {
		msg := &Msg{Status: false, Code: 0, Message: "To 账号冻结"}
		rev, _ = json.Marshal(msg)
		return rev
	}
	if !token.isCurrency(_currency) {
		msg := &Msg{Status: false, Code: 0, Message: "货币符号不存在"}
		rev, _ = json.Marshal(msg)
		return rev
	}
	if _from.BalanceOf[_currency] >= _value {
		_from.BalanceOf[_currency] -= _value
		_to.BalanceOf[_currency] += _value

		token.Currency[_currency].User[_from.Name] = _from.BalanceOf[_currency]
		token.Currency[_currency].User[_to.Name] = _to.BalanceOf[_currency]

		msg := &Msg{Status: true, Code: 0, Message: "转账成功！"}
		rev, _ = json.Marshal(msg)
		return rev
	} else {
		msg := &Msg{Status: false, Code: 0, Message: "余额不足"}
		rev, _ = json.Marshal(msg)
		return rev
	}

}

/**
初始化代币
 */
func (token *Token) initialSupply(_name string, _symbol string, _supply float64, _account *Account, lock bool) []byte {
	if _, ok := token.Currency[_symbol]; ok {
		msg := &Msg{Status: false, Code: 0, Message: "代币已经存在"}
		rev, _ := json.Marshal(msg)
		return rev
	}

	if _account.BalanceOf[_symbol] > 0 {
		msg := &Msg{Status: false, Code: 0, Message: "账号中存在代币"}
		rev, _ := json.Marshal(msg)
		return rev
	} else {
		user := make(map[string]float64)
		user[_account.Name] = _supply
		token.Currency[_symbol] = Currency{TokenName: _name, TokenSymbol: _symbol, TotalSupply: _supply, Lock: lock, User: user}
		_account.BalanceOf[_symbol] = _supply

		msg := &Msg{Status: true, Code: 0, Message: "代币初始化成功"}
		rev, _ := json.Marshal(msg)
		return rev
	}

}

//添加转账记录
func (token *Token) installRecord(_from string, _to string, _currency string, _value float64, txId string,description string) []byte {
	var curr Currency

	record := TransactionRecord{_from, _to, _value, txId,time.Now().Format("2006-01-02 15:04:05"),description}
	recordList := make([]TransactionRecord, 0)

	recordList = append(token.Currency[_currency].Record, record)
	curr.Record = recordList
	curr.User = token.Currency[_currency].User
	curr.Lock = token.Currency[_currency].Lock
	curr.TokenName = token.Currency[_currency].TokenName
	curr.TokenSymbol = token.Currency[_currency].TokenSymbol
	curr.TotalSupply = token.Currency[_currency].TotalSupply

	token.Currency[_currency] = curr

	msg := &Msg{Status: true, Code: 0, Message: "添加转账记录成功"}
	rev, _ := json.Marshal(msg)
	return rev
}

//代币增发
func (token *Token) mint(_currency string, _amount float64, _account *Account) []byte {
	if !token.isCurrency(_currency) {
		msg := &Msg{Status: false, Code: 0, Message: "货币符号不存在"}
		rev, _ := json.Marshal(msg)
		return rev
	}
	cur := token.Currency[_currency]
	cur.TotalSupply += _amount
	cur.User[_account.Name] += _amount
	token.Currency[_currency] = cur
	_account.BalanceOf[_currency] += _amount

	msg := &Msg{Status: true, Code: 0, Message: "代币增发成功"}
	rev, _ := json.Marshal(msg)
	return rev

}

//代币回收
func (token *Token) burn(_currency string, _amount float64, _account *Account) []byte {
	if !token.isCurrency(_currency) {
		msg := &Msg{Status: false, Code: 0, Message: "货币符号不存在"}
		rev, _ := json.Marshal(msg)
		return rev
	}
	if _account.BalanceOf[_currency] >= _amount {
		cur := token.Currency[_currency]
		cur.TotalSupply -= _amount
		cur.User[_account.Name] -= _amount
		token.Currency[_currency] = cur
		_account.BalanceOf[_currency] -= _amount

		msg := &Msg{Status: false, Code: 0, Message: "代币回收成功"}
		rev, _ := json.Marshal(msg)
		return rev
	} else {
		msg := &Msg{Status: false, Code: 0, Message: "代币回收失败，回收额度不足"}
		rev, _ := json.Marshal(msg)
		return rev
	}

}

func (token *Token) isCurrency(_currency string) bool {
	if _, ok := token.Currency[_currency]; ok {
		return true
	} else {
		return false
	}
}
//锁仓
func (token *Token) setLock(_currency string, _look bool) bool {
	cur := token.Currency[_currency]
	cur.Lock = _look
	token.Currency[_currency] = cur
	return token.Currency[_currency].Lock
}

func (account *Account) balance(_currency string) map[string]float64 {
	bal := map[string]float64{_currency: account.BalanceOf[_currency]}
	return bal
}

func (account *Account) balanceAll() map[string]float64 {
	return account.BalanceOf
}

/**
token记录初始化
 */
func (s *HearkenCoinContract) Init(stub shim.ChaincodeStubInterface) peer.Response {

	token := &Token{Currency: map[string]Currency{}}

	tokenAsBytes, err := json.Marshal(token)
	err = stub.PutState(TokenKey, tokenAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	} else {
		fmt.Printf("Init Token %s \n", string(tokenAsBytes))
	}
	err = stub.SetEvent("tokenInit", []byte{})
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(nil)
}

func (s *HearkenCoinContract) Query(stub shim.ChaincodeStubInterface) peer.Response {
	function, args := stub.GetFunctionAndParameters()
	if function == "balance" {
		return s.balance(stub, args)
	} else if function == "balanceAll" {
		return s.balanceAll(stub, args)
	} else if function == "showAccount" {
		return s.showAccount(stub, args)
	}
	return shim.Error("Invalid Smart Contract function name.")
}

/**
链码调用
 */
func (s *HearkenCoinContract) Invoke(stub shim.ChaincodeStubInterface) peer.Response {
	// 获取链码方法跟参数
	function, args := stub.GetFunctionAndParameters()
	if function == "initLedger" {
		// 注册管理员账户
		//peer chaincode invoke -C myc -n token -c '{"function":"initLedger","Args":[]}'
		return s.initLedger(stub, args)
	} else if function == "createAccount" {
		//创建账户 参数 ： (1)账户名
		//peer chaincode invoke -C myc -n token -c '{"function":"createAccount","Args":["123"]}'
		return s.createAccount(stub, args)
	} else if function == "initCurrency" {
		//创建代币 (1) 代币全称 (2) 代币简称 (3) 代币总量 (4) 代币生成以后持有人 (5) 是否锁仓
		// peer chaincode invoke -C myc -n token -c '{"function":"initCurrency","Args":["Netkiller Token","NKC","1000000","skyhuihui","false"]}'
		return s.initCurrency(stub, args)
	} else if function == "setLock" {
		//锁仓某个代币 (1) 代币简称 (2) 是否锁仓 (3) 操作人
		//peer chaincode invoke -C myc -n token -c '{"function":"setLock","Args":["NKC","true","skyhuihui"]}'
		return s.setLock(stub, args)
	} else if function == "transferToken" {
		//转账 (1) 发送人(2) 接收人(3) 代币名(4)发送代币量
		//peer chaincode invoke -C myc -n token -c '{"function":"transferToken","Args":["skyhuihui","123","ada","12.584"]}'
		return s.transferToken(stub, args)
	} else if function == "frozenAccount" {
		//冻结账户 (1) 要冻结的账户 (2) 是否冻结 (3) 操作人
		//peer chaincode invoke -C myc -n token -c '{"function":"frozenAccount","Args":["netkiller","true","skyhuihui"]}'
		return s.frozenAccount(stub, args)
	} else if function == "mintToken" {
		//代币增发 (1)代币名称(2)增发数量(3)操作人，也是代币增发接收人
		//peer chaincode invoke -C myc -n token -c '{"function":"mintToken","Args":["NKC","5000","skyhuihui"]}'
		return s.mintToken(stub, args)
	} else if function == "burnToken" {
		//代币销毁 (1)代币名称(2)回收数量(3)回收的账户（回收谁的代币）(4)操作人
		//peer chaincode invoke -C myc -n token -c '{"function":"burnToken","Args":["NKC","5000","123","skyhuihui"]}'
		return s.burnToken(stub, args)
	} else if function == "balance" {
		//查询指定账户指定代币 (1)查询账户 （2） 代币名称
		//peer chaincode invoke -C myc -n token -c '{"function":"balance","Args":["skyhuihui","NKC"]}'
		return s.balance(stub, args)
	} else if function == "tokenHistory" {
		//查询指定代币交易记录 (1)代币名称
		//peer chaincode invoke -C myc -n token -c '{"function":"tokenHistory","Args":["NKC"]}'
		return s.tokenHistory(stub, args)
	} else if function == "userTokenHistory" {
		//查询指定用户指定代币交易记录 (1)代币名称(2)用户名
		//peer chaincode invoke -C myc -n token -c '{"function":"userTokenHistory","Args":["NKC","skyhuihui"]}'
		return s.userTokenHistory(stub, args)
	} else if function == "getHistoryKey" {
		//查询某个key 历史交易 (1)代币名称
		//peer chaincode invoke -C myc -n token -c '{"function":"tokenHistory","Args":["NKC"]}'
		return s.getHistoryForKey(stub, args)
	} else if function == "balanceAll" {
		//查询某个用户所有资金 (1)账户名
		//peer chaincode invoke -C myc -n token -c '{"function":"balanceAll","Args":["skyhuihui"]}'
		return s.balanceAll(stub, args)
	} else if function == "showAccount" {
		//查看某个账户(1)账户名
		//peer chaincode invoke -C myc -n token -c '{"function":"showAccount","Args":["skyhuihui"]}'
		return s.showAccount(stub, args)
	} else if function == "showToken" {
		//查看所有代币
		//peer chaincode invoke -C myc -n token -c '{"function":"showToken","Args":[]}'
		return s.showToken(stub, args)
	} else if function == "showTokenUser" {
		//查看代币的所有持有用户 （1）代币名
		//peer chaincode invoke -C myc -n token -c '{"function":"showTokenUser","Args":["ada"]}'
		return s.showTokenUser(stub, args)
	}
	return shim.Error("Invalid Smart Contract function name.")
}

//查看代币的所有持有用户
func (s *HearkenCoinContract) showTokenUser(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}
	_token := args[0]
	token := Token{}
	existAsBytes, err := stub.GetState(TokenKey)
	if err != nil {
		return shim.Error(err.Error())
	} else {
		fmt.Printf("GetState(%s)) %s \n", TokenKey, string(existAsBytes))
	}
	json.Unmarshal(existAsBytes, &token)
	reToekn, err := json.Marshal(token.Currency[_token])
	if err != nil {
		return shim.Error(err.Error())
	} else {
		fmt.Printf("Account balance %s \n", string(reToekn))
	}
	return shim.Success(reToekn)
}

/**
创建账户
 */
func (s *HearkenCoinContract) createAccount(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	if len(args) != 1 {
		return shim.Error("一个账户名参数！")
	}
	key := args[0]
	name := args[0]
	existAsBytes, err := stub.GetState(key)
	if string(existAsBytes) != "" {
		return shim.Error("当前用户账户已经存在！")
	}
	//创建当前用户的账户
	account := Account{
		Name:      name,
		Frozen:    false,
		BalanceOf: map[string]float64{}}

	accountAsBytes, _ := json.Marshal(account)
	err = stub.PutState(key, accountAsBytes)
	if err != nil {
		return shim.Error("初始化用户账户钱包失败："+err.Error())
	}
	//初始化
	err = stub.SetEvent("addUser", []byte{})
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(accountAsBytes)
}

/**
注册管理员
 */
func (s *HearkenCoinContract) initLedger(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	var key string
	var name string
	if len(args)==0 {
	    key = "成都淞幸科技有限责任公司"
	    name = "成都淞幸科技有限责任公司"
	}else if len(args)>1 {
		return shim.Error("只能有一个参数：管理员名！")
	}else{
		key = args[0]
		name = args[0]
	}
	exist, err := stub.GetState(Admin)
	if nil != exist {
		return shim.Error("已经存在管理员账户！")
	}
	//设置管理员
	b, err := json.Marshal(key)//格式化成byte数组
	err = stub.PutState(Admin, b)
	if err != nil {
		return shim.Error(err.Error())
	}
	//给管理员一个币账户
	account := Account{
		Name:      name,
		Frozen:    false,
		BalanceOf: map[string]float64{}}
	accountAsBytes, _ := json.Marshal(account)
	err = stub.PutState(key, accountAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	//初始化token
	//HearkenCoinContract.Init()
	//事件
	err = stub.SetEvent("addAdmin", []byte{})
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(accountAsBytes)
}

func (s *HearkenCoinContract) showToken(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	tokenAsBytes, err := stub.GetState(TokenKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(tokenAsBytes)
}

/**
初始化币池（创建代币）只能是管理员账户
 */
func (s *HearkenCoinContract) initCurrency(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	if len(args) != 5 {
		return shim.Error("需要5个参数：(1) 代币全称 (2) 代币简称 (3) 代币总量 (4) 代币生成以后持有人 (5) 是否锁仓")
	}
	admin, err := stub.GetState(Admin)
	if nil == admin {
		return shim.Error("未设置管理员账户！" )
	}
	account, _ := json.Marshal(args[3])
	if string(account) != string(admin) {
		return shim.Error("只有管理员才能初始化币池!")
	}
	//初始化管理员钱包
	_name := args[0]
	_symbol := args[1]
	_supply, _ := strconv.ParseFloat(args[2], 64)
	_account := args[3]
	lock := args[4]
	coinbaseAsBytes, err := stub.GetState(_account)
	if err != nil {
		return shim.Error(err.Error())
	}
	coinbase := &Account{}
	json.Unmarshal(coinbaseAsBytes, &coinbase) //赋值
	token := Token{}
	existAsBytes, err := stub.GetState(TokenKey)//得到token
	if err != nil {
		return shim.Error(err.Error())
	}
	json.Unmarshal(existAsBytes, &token) //复制
	var blog bool
	if lock == "false" {
		blog = false
	} else {
		blog = true
	}
	result := token.initialSupply(_name, _symbol, _supply, coinbase, blog)
	tokenAsBytes, _ := json.Marshal(token)
	err = stub.PutState(TokenKey, tokenAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	coinbaseAsBytes, _ = json.Marshal(coinbase)
	err = stub.PutState(_account, coinbaseAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.SetEvent("initCoin", []byte{})
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(result)
}

/**
转账
 */
func (s *HearkenCoinContract) transferToken(stub shim.ChaincodeStubInterface, args []string) peer.Response {

	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 4：(1) 发送人(2) 接收人(3) 代币名(4)发送代币量 (5)交易说明")
	}
	_from := args[0]
	_to := args[1]
	_currency := args[2]
	_amount, _ := strconv.ParseFloat(args[3], 32)   //32 改成了64
	_description := args[4]

	if _amount <= 0 {
		return shim.Error("交易的币数量必须大于零！")
	}
	//发送人账户
	fromAsBytes, err := stub.GetState(_from)
	if err != nil {
		return shim.Error(err.Error())
	}
	if nil == fromAsBytes {
		return shim.Error("没有"+_from+"的账户！")
	}
	fromAccount := &Account{}
	json.Unmarshal(fromAsBytes, &fromAccount)
	//发送人账户币是否足够转账
	left := fromAccount.BalanceOf[_currency] //用户持有的当前代币数量---_currency：代币类型
	if left < _amount {
		return shim.Error("当前用户余额不足！")
	}

	//接收人账户
	toAsBytes, err := stub.GetState(_to)
	if err != nil {
		return shim.Error(err.Error())
	}
	if nil == toAsBytes {
		return shim.Error("没有"+_to+"的账户！")
	}
	toAccount := &Account{}
	json.Unmarshal(toAsBytes, &toAccount)

	//原来的记录信息
	tokenAsBytes, err := stub.GetState(TokenKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	token := Token{Currency: map[string]Currency{}}
	json.Unmarshal(tokenAsBytes, &token)

	//转账
	//result := token.transfer(fromAccount, toAccount, _currency, _amount)
	fromAccount.BalanceOf[_currency] -= _amount
	toAccount.BalanceOf[_currency] += _amount
	token.Currency[_currency].User[_from] = fromAccount.BalanceOf[_currency]
	token.Currency[_currency].User[_to] = toAccount.BalanceOf[_currency]
	tokenAsBytes, err = json.Marshal(token)
	if err != nil {
		return shim.Error(err.Error())
	}
	//保存转账后之后的账户信息
	err = stub.PutState(TokenKey, tokenAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	//验证是否正常保存
	existAsBytes, err := stub.GetState(TokenKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	json.Unmarshal(existAsBytes, &token)

	//添加到历史记录
	token.installRecord(fromAccount.Name, toAccount.Name, _currency, _amount, stub.GetTxID(),_description)
	//记录信息到token
	tokenAsBytes, _ = json.Marshal(token)
	err = stub.PutState(TokenKey, tokenAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	//重新设置付款人跟收款人的账户
	fromAsBytes, err = json.Marshal(fromAccount)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(_from, fromAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	toAsBytes, err = json.Marshal(toAccount)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(_to, toAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	err = stub.SetEvent("transferRecord", []byte{})
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success([]byte("转账成功"))
}

/**
代币增发 (1)代币名称(2)增发数量(3)代币增发接收人
 */
func (s *HearkenCoinContract) mintToken(stub shim.ChaincodeStubInterface, args []string) peer.Response {

	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 3")
	}
	//拿到管理员账户
	admin, err := stub.GetState(Admin)
	if admin == nil {
		return shim.Error("未设置管理员账户")
	}
	account, _ := json.Marshal(args[2])
	if string(account) != string(admin) {
		return shim.Error("当前账户不是管理员账户")
	}

	_currency := args[0]
	_amount, _ := strconv.ParseFloat(args[1], 32)
	_account := args[2]
	//得到当前账户
	coinbaseAsBytes, err := stub.GetState(_account)
	if err != nil {
		return shim.Error(err.Error())
	}
	//总帐户
	coinbase := &Account{}
	json.Unmarshal(coinbaseAsBytes, &coinbase)
	tokenAsBytes, err := stub.GetState(TokenKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	token := Token{}
	json.Unmarshal(tokenAsBytes, &token)
	//增发操作
	result := token.mint(_currency, _amount, coinbase)

	tokenAsBytes, err = json.Marshal(token)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(TokenKey, tokenAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	coinbaseAsBytes, _ = json.Marshal(coinbase)
	err = stub.PutState(_account, coinbaseAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	//触发事件
	err = stub.SetEvent("mintToken", []byte{})
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(result)
}

/**
代币销毁 (1)代币名称(2)回收数量(3)回收的账户（回收谁的代币）(4)操作人
 */
func (s *HearkenCoinContract) burnToken(stub shim.ChaincodeStubInterface, args []string) peer.Response {

	if len(args) != 4 {
		return shim.Error("Incorrect number of arguments. Expecting 4：(1)代币名称(2)回收数量(3)回收的账户（回收谁的代币）(4)操作人")
	}
	admin, err := stub.GetState(Admin)
	if admin == nil {
		return shim.Error("管理员账户为空！")
	}
	account, _ := json.Marshal(args[3])
	if string(account) != string(admin) {
		return shim.Error("当前账户不是管理员账户！")
	}

	_currency := args[0]
	_amount, _ := strconv.ParseFloat(args[1], 32)
	_account := args[2]
	coinbaseAsBytes, err := stub.GetState(_account)
	if err != nil {
		return shim.Error(err.Error())
	}
	coinbase := &Account{}
	json.Unmarshal(coinbaseAsBytes, &coinbase)

	tokenAsBytes, err := stub.GetState(TokenKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	token := Token{}
	json.Unmarshal(tokenAsBytes, &token)

	result := token.burn(_currency, _amount, coinbase)
	tokenAsBytes, err = json.Marshal(token)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(TokenKey, tokenAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	coinbaseAsBytes, _ = json.Marshal(coinbase)
	err = stub.PutState(_account, coinbaseAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}

	err = stub.SetEvent("burnToken", []byte{})
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(result)
}

//锁仓
func (s *HearkenCoinContract) setLock(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 2")
	}
	admin, err := stub.GetState(Admin)
	if admin == nil {
		return shim.Error("The administrator account is empty")
	}
	account, _ := json.Marshal(args[2])
	if string(account) != string(admin) {
		return shim.Error("Current account is not an admin account")
	}
	_currency := args[0]
	_look := args[1]

	tokenAsBytes, err := stub.GetState(TokenKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	// fmt.Printf("setLock - begin %s \n", string(tokenAsBytes))

	token := Token{}

	json.Unmarshal(tokenAsBytes, &token)

	if _look == "true" {
		token.setLock(_currency, true)
	} else {
		token.setLock(_currency, false)
	}

	tokenAsBytes, err = json.Marshal(token)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(TokenKey, tokenAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	}
	fmt.Printf("setLock - end %s \n", string(tokenAsBytes))
	err = stub.SetEvent("setLock", []byte{})
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(nil)
}

//冻结账户
func (s *HearkenCoinContract) frozenAccount(stub shim.ChaincodeStubInterface, args []string) peer.Response {

	if len(args) != 3 {
		return shim.Error("Incorrect number of arguments. Expecting 2")
	}
	admin, err := stub.GetState(Admin)
	if admin == nil {
		return shim.Error("The administrator account is empty")
	}
	acc, _ := json.Marshal(args[2])
	if string(acc) != string(admin) {
		return shim.Error("Current account is not an admin account")
	}

	_account := args[0]
	_status := args[1]

	accountAsBytes, err := stub.GetState(_account)
	if err != nil {
		return shim.Error(err.Error())
	}
	// fmt.Printf("setLock - begin %s \n", string(tokenAsBytes))

	account := Account{}

	json.Unmarshal(accountAsBytes, &account)

	var status bool
	if _status == "true" {
		status = true
	} else {
		status = false
	}

	account.Frozen = status

	accountAsBytes, err = json.Marshal(account)
	if err != nil {
		return shim.Error(err.Error())
	}
	err = stub.PutState(_account, accountAsBytes)
	if err != nil {
		return shim.Error(err.Error())
	} else {
		fmt.Printf("frozenAccount - end %s \n", string(accountAsBytes))
	}
	err = stub.SetEvent("frozenAccount", []byte{})
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(nil)
}

//获取代币交易记录
func (s *HearkenCoinContract) tokenHistory(stub shim.ChaincodeStubInterface, args []string) peer.Response {

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}
	_currency := args[0]

	tokenAsBytes, err := stub.GetState(TokenKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	token := Token{}
	json.Unmarshal(tokenAsBytes, &token)
	resultAsBytes, _ := json.Marshal(token.Currency[_currency].Record)

	return shim.Success(resultAsBytes)
}

//获取某个用户某个代币交易记录
func (s *HearkenCoinContract) userTokenHistory(stub shim.ChaincodeStubInterface, args []string) peer.Response {

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2")
	}
	_currency := args[0]  //代币名
	_account := args[1]	//用户名

	//是否有该用户
	userAccount, err := stub.GetState(_account)
	if nil==userAccount {
		return shim.Error("当前用户还没有构件币账户！")
	}

	tokenAsBytes, err := stub.GetState(TokenKey)
	if err != nil {
		return shim.Error(err.Error())
	}
	token := Token{}
	json.Unmarshal(tokenAsBytes, &token)

	var userRecord []TransactionRecord
	index := 0
	for k, v := range token.Currency[_currency].Record {
		if token.Currency[_currency].Record[k].From == _account || token.Currency[_currency].Record[k].To == _account {
			userRecord = append(userRecord, v)
			index++
		}
	}
	resultAsBytes, _ := json.Marshal(userRecord)
	return shim.Success(resultAsBytes)
}

func (s *HearkenCoinContract) getHistoryForKey(stub shim.ChaincodeStubInterface, args []string) peer.Response {
	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}

	marbleId := args[0]

	// 返回某个键的所有历史值
	resultsIterator, err := stub.GetHistoryForKey(marbleId)
	if err != nil {
		return shim.Error(err.Error())
	}

	defer resultsIterator.Close()

	var buffer bytes.Buffer
	buffer.WriteString("[")

	bArrayMemberAlreadyWritten := false
	for resultsIterator.HasNext() {
		queryResult, err := resultsIterator.Next()
		if err != nil {
			return shim.Error(err.Error())
		}

		if bArrayMemberAlreadyWritten == true {
			buffer.WriteString(",")
		}

		buffer.WriteString("{\"TxId\":")
		buffer.WriteString("\"")
		buffer.WriteString(queryResult.TxId)
		buffer.WriteString("\"")

		buffer.WriteString(", \"Timestamp\":")
		buffer.WriteString("\"")
		buffer.WriteString(time.Unix(queryResult.Timestamp.Seconds, int64(queryResult.Timestamp.Nanos)).String())
		buffer.WriteString("\"")

		buffer.WriteString("{\"Value\":")
		buffer.WriteString("\"")
		buffer.WriteString(string(queryResult.Value))
		buffer.WriteString("\"")

		buffer.WriteString("{\"IsDelete\":")
		buffer.WriteString("\"")
		buffer.WriteString(strconv.FormatBool(queryResult.IsDelete))
		buffer.WriteString("\"")

		bArrayMemberAlreadyWritten = true
	}
	buffer.WriteString("]")
	fmt.Printf("- getMarblesByRange queryResult:\n%s\n", buffer.String())
	return shim.Success(buffer.Bytes())
}

func (s *HearkenCoinContract) showAccount(stub shim.ChaincodeStubInterface, args []string) peer.Response {

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}
	_account := args[0]

	accountAsBytes, err := stub.GetState(_account)
	if err != nil {
		return shim.Error(err.Error())
	}
	return shim.Success(accountAsBytes)
}

/**
查询指定账户指定代币 (1)查询账户 （2） 代币名称
 */
func (s *HearkenCoinContract) balance(stub shim.ChaincodeStubInterface, args []string) peer.Response {

	if len(args) != 2 {
		return shim.Error("Incorrect number of arguments. Expecting 2： (1)查询账户 （2） 代币名称")
	}
	_account := args[0]
	_currency := args[1]

	accountAsBytes, err := stub.GetState(_account)
	if err != nil {
		return shim.Error(err.Error())
	}
	//是否有该用户
	if nil==accountAsBytes {
		return shim.Error("当前用户未创建账户！")
	}

	account := Account{}
	json.Unmarshal(accountAsBytes, &account)
	result := account.balance(_currency)
	resultAsBytes, _ := json.Marshal(result)

	return shim.Success(resultAsBytes)
}

func (s *HearkenCoinContract) balanceAll(stub shim.ChaincodeStubInterface, args []string) peer.Response {

	if len(args) != 1 {
		return shim.Error("Incorrect number of arguments. Expecting 1")
	}
	_account := args[0]

	accountAsBytes, err := stub.GetState(_account)
	if err != nil {
		return shim.Error(err.Error())
	} else {
		fmt.Printf("Account balance %s \n", string(accountAsBytes))
	}

	account := Account{}
	json.Unmarshal(accountAsBytes, &account)
	result := account.balanceAll()
	resultAsBytes, _ := json.Marshal(result)
	fmt.Printf("%s balance is %s \n", _account, string(resultAsBytes))

	return shim.Success(resultAsBytes)
}

// The main function is only relevant in unit test mode. Only included here for completeness.
func main() {
	// Create a new Smart Contract
	err := shim.Start(new(HearkenCoinContract))
	if err != nil {
		fmt.Printf("Error creating new Smart Contract: %s", err)
	}
}

