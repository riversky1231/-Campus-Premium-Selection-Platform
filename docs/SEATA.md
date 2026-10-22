# Seata AT 接入与验收

完整Docker构建使用-Pseata,monitor，商品运行prod,seata，用户和订单运行prod。三个库均有undo_log。TC使用Seata 2.0.0，文件注册/配置及持久事务存储；客户端事务组campus_tx_group映射default，TC地址seata:8091。

TradingFacade的create/pay/cancel使用@GlobalTransactional，内部OrderService处理本地事务。Feign拦截器转发TX_XID，商品和用户的Alibaba Seata starter接收XID并自动代理数据源。不得重复手工代理DataSource。

验收需在实际MySQL/Seata环境完成：

1. 成功下单后，订单与stock_operation存在，库存准确减少。
2. 选择两个商品，第二个库存不足；让第一项已扣库存后失败，检查第一项库存恢复，订单与操作记录回滚。
3. 支付时在用户扣款成功、订单更新前注入异常，检查余额与钱包流水回滚，订单仍PENDING。可用调试器断点抛出异常或专用测试构建；不要在生产暴露故障注入接口。
4. 取消订单中途失败，验证已恢复的库存和订单状态一起回滚。
5. 并发支付与关单，只允许一种状态转换；重复消息不重复变更余额和库存。
6. 停止TC，交易应失败而不是静默以非事务方式继续。恢复TC后核对待恢复分支。
7. 重启应用及TC，确认未完成事务可恢复；查询数据库和TC日志，不以HTTP返回码代替数据验证。

全局AT回滚成功后不要额外调用库存恢复；恢复接口是正常取消订单的业务操作。demo模式关闭Seata，用尽力补偿实现演示，进程崩溃与网络不确定性不等价于AT保证。

参考：[Alibaba Seata配置说明](https://sca.aliyun.com/docs/2023/user-guide/seata/advanced-guide/)、[Seata 2.0.0服务器配置](https://github.com/apache/incubator-seata/blob/v2.0.0/server/src/main/resources/application.yml)。
