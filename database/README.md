# 数据库说明

MySQL 8入口init.sql（UTF-8），由Docker MySQL官方镜像在新数据卷首次启动时执行。不会DROP数据库或表。先由镜像创建campus账户，再由init.sql授予三个业务库的SELECT/INSERT/UPDATE/DELETE权限；运行账号不需要DDL权限。

| 数据库 | 表 | 用途 |
|---|---|---|
| campus_product | category | 商品分类，名称唯一 |
| campus_product | product | 价格、库存和上下架状态 |
| campus_product | stock_operation | 请求号唯一的库存操作，防重复和取消先到 |
| campus_user | app_user | 账号、BCrypt密码、角色、余额、状态、微信openid |
| campus_user | user_session | 登录令牌摘要与过期时间 |
| campus_user | user_address | 用户所属地址 |
| campus_user | wallet_operation | 充值/扣款/退款流水，操作号+类型联合主键 |
| campus_order | market_order | 用户订单、状态、金额、地址快照、提货码 |
| campus_order | order_item | 商品名称/价格/数量快照 |
| campus_order | event_outbox | 事务内写入的待投递事件 |
| campus_order | user_notice | 消息ID幂等的站内通知 |
| 每个业务库 | undo_log | Seata AT分支回滚日志 |

金额使用DECIMAL，不使用浮点数。库存有非负约束，钱包余额有非负约束。提货码唯一；同用户同下单请求号唯一。三个库不设跨服务外键，仅库内用外键保护引用。

下单状态：PENDING → PAID → COMPLETED；PENDING → CANCELLED。已支付订单不能走取消接口，本项目未包含售后退款。用户地址保存为订单快照，删除原地址不破坏订单。

库存操作：DEDUCTED、RESTORED、CANCELLED、ADDED。同号不同参数返回409；恢复先到保存CANCELLED，阻止迟到扣减。钱包REFUND同样可先留下未实际退款记录，阻止迟到扣款。

时间：订单过期按UTC计算；容器TZ=UTC。API的时间字段为不带时区的UTC本地时间，前端展示时需按UTC解释。

demo模式使用各模块src/main/resources/db/schema.sql创建独立H2内存库，生产模式不自动运行这些脚本。MySQL初始化数据只带商品示例，管理员由用户服务根据MARKET_ADMIN_PASSWORD首次创建，普通用户余额初始0。

如已有数据库，请先备份并审核迁移；init.sql不是增量迁移脚本。
