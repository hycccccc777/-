-- ==============================================================
-- 校易淘 · 校园二手交易系统 业务库脚本
-- --------------------------------------------------------------
-- 说明：
--   1. 复用若依 sys_* 系统表，本脚本只新增 ct_ 前缀的业务表。
--   2. 用户主账号沿用 sys_user（user_id 作为所有业务表的用户外键）。
--      - 登录按 phonenumber + password 校验（不新增独立用户表，避免与鉴权重复）。
--      - 昵称取 sys_user.nick_name，头像取 sys_user.avatar。
--   3. 校园专属信息（学校、学号、认证状态）放在 ct_user_profile。
--   4. 所有状态/类型字段使用 char(1) 或短码，并与前端字段映射文档对齐：
--      - 成色 condition_code 用短码，中文由 sys_dict_data('ct_condition') 提供。
--      - Java Long 主键对外序列化为字符串，避免浏览器整数精度丢失。
--   5. 金额统一 decimal(10,2)，单位元；前端展示金额不可作为后端计价依据。
--   6. 统一响应成功码 = 200（已与前端冻结；非本脚本内容，仅作记录）。
--   7. 字符集：utf8mb4 / utf8mb4_general_ci。
-- ==============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1、学校表
-- ----------------------------
drop table if exists ct_school;
create table ct_school (
  school_id       bigint(20)      not null auto_increment    comment '学校ID',
  school_name     varchar(64)     not null                   comment '学校全称',
  short_name      varchar(32)     default ''                 comment '学校简称',
  city            varchar(32)     default ''                 comment '所在城市',
  order_num       int(4)          default 0                  comment '显示顺序',
  status          char(1)         default '0'                comment '状态（0正常 1停用）',
  del_flag        char(1)         default '0'                comment '删除标志（0存在 2删除）',
  create_by       varchar(64)     default ''                 comment '创建者',
  create_time     datetime                                   comment '创建时间',
  update_by       varchar(64)     default ''                 comment '更新者',
  update_time     datetime                                   comment '更新时间',
  remark          varchar(255)    default null               comment '备注',
  primary key (school_id)
) engine=innodb auto_increment=100 comment = '学校表';

-- ----------------------------
-- 2、商品类目表
-- ----------------------------
drop table if exists ct_category;
create table ct_category (
  category_id     bigint(20)      not null auto_increment    comment '类目ID',
  parent_id       bigint(20)      default 0                  comment '父类目ID（0为根）',
  category_name   varchar(64)     not null                   comment '类目名称',
  icon            varchar(64)     default ''                 comment '前端图标名',
  order_num       int(4)          default 0                  comment '显示顺序',
  status          char(1)         default '0'                comment '状态（0正常 1停用）',
  del_flag        char(1)         default '0'                comment '删除标志（0存在 2删除）',
  create_by       varchar(64)     default ''                 comment '创建者',
  create_time     datetime                                   comment '创建时间',
  update_by       varchar(64)     default ''                 comment '更新者',
  update_time     datetime                                   comment '更新时间',
  remark          varchar(255)    default null               comment '备注',
  primary key (category_id)
) engine=innodb auto_increment=100 comment = '商品类目表';

-- ----------------------------
-- 3、用户校园档案表（扩展 sys_user）
-- ----------------------------
drop table if exists ct_user_profile;
create table ct_user_profile (
  profile_id      bigint(20)      not null auto_increment    comment '档案ID',
  user_id         bigint(20)      not null                   comment '用户ID（sys_user.user_id）',
  real_name       varchar(64)     default ''                 comment '真实姓名',
  student_no      varchar(32)     default ''                 comment '学号',
  school_id       bigint(20)      default null               comment '所属学校ID',
  verify_status   char(1)         default '0'                comment '学号认证状态（0未认证 1待审核 2已认证 3已驳回）',
  verify_remark   varchar(255)    default ''                 comment '认证备注/驳回原因',
  verify_time     datetime                                   comment '认证通过时间',
  contact_phone   varchar(20)     default ''                 comment '常用联系方式',
  create_by       varchar(64)     default ''                 comment '创建者',
  create_time     datetime                                   comment '创建时间',
  update_by       varchar(64)     default ''                 comment '更新者',
  update_time     datetime                                   comment '更新时间',
  primary key (profile_id),
  unique key uk_ct_profile_user (user_id),
  key idx_ct_profile_school (school_id)
) engine=innodb comment = '用户校园档案表';

-- ----------------------------
-- 4、商品表
-- ----------------------------
drop table if exists ct_product;
create table ct_product (
  product_id      bigint(20)      not null auto_increment    comment '商品ID',
  title           varchar(128)    not null                   comment '商品标题',
  category_id     bigint(20)      not null                   comment '类目ID',
  seller_id       bigint(20)      not null                   comment '卖家用户ID',
  seller_type     char(1)         default '0'                comment '卖家类型（0个人 1商家）',
  price           decimal(10,2)   not null                   comment '售价（元）',
  original_price  decimal(10,2)   default null               comment '原价（元，可空）',
  stock           int(11)         default 1                  comment '库存',
  condition_code  varchar(16)     default ''                 comment '成色（字典 ct_condition）',
  school_id       bigint(20)      default null               comment '所属学校ID',
  location        varchar(128)    default ''                 comment '交接地点',
  cover_url       varchar(255)    default ''                 comment '封面图URL',
  tag             varchar(32)     default ''                 comment '标签',
  description     varchar(2000)   default ''                 comment '商品描述',
  status          char(1)         default '0'                comment '状态（0待审核 1已上架 2已下架 3审核驳回 4已售出）',
  audit_remark    varchar(255)    default ''                 comment '审核意见',
  view_count      int(11)         default 0                  comment '浏览量',
  favorite_count  int(11)         default 0                  comment '收藏数',
  del_flag        char(1)         default '0'                comment '删除标志（0存在 2删除）',
  create_by       varchar(64)     default ''                 comment '创建者',
  create_time     datetime                                   comment '创建时间',
  update_by       varchar(64)     default ''                 comment '更新者',
  update_time     datetime                                   comment '更新时间',
  primary key (product_id),
  key idx_ct_product_category (category_id),
  key idx_ct_product_seller (seller_id),
  key idx_ct_product_school (school_id),
  key idx_ct_product_status (status),
  key idx_ct_product_price (price),
  key idx_ct_product_create_time (create_time)
) engine=innodb comment = '商品表';

-- ----------------------------
-- 5、商品图片表
-- ----------------------------
drop table if exists ct_product_image;
create table ct_product_image (
  image_id        bigint(20)      not null auto_increment    comment '图片ID',
  product_id      bigint(20)      not null                   comment '商品ID',
  image_url       varchar(255)    not null                   comment '图片URL',
  order_num       int(4)          default 0                  comment '排序',
  create_time     datetime                                   comment '创建时间',
  primary key (image_id),
  key idx_ct_image_product (product_id)
) engine=innodb comment = '商品图片表';

-- ----------------------------
-- 6、收藏表
-- ----------------------------
drop table if exists ct_favorite;
create table ct_favorite (
  favorite_id     bigint(20)      not null auto_increment    comment '收藏ID',
  user_id         bigint(20)      not null                   comment '用户ID',
  product_id      bigint(20)      not null                   comment '商品ID',
  create_time     datetime                                   comment '收藏时间',
  primary key (favorite_id),
  unique key uk_ct_favorite (user_id, product_id),
  key idx_ct_favorite_product (product_id)
) engine=innodb comment = '商品收藏表（跨设备同步用）';

-- ----------------------------
-- 7、购物车表
-- ----------------------------
drop table if exists ct_cart_item;
create table ct_cart_item (
  cart_item_id    bigint(20)      not null auto_increment    comment '购物车条目ID（非商品ID）',
  user_id         bigint(20)      not null                   comment '用户ID',
  product_id      bigint(20)      not null                   comment '商品ID',
  quantity        int(11)         default 1                  comment '数量',
  selected        char(1)         default '1'                comment '是否选中（0否 1是）',
  create_time     datetime                                   comment '创建时间',
  update_time     datetime                                   comment '更新时间',
  primary key (cart_item_id),
  unique key uk_ct_cart (user_id, product_id),
  key idx_ct_cart_user (user_id)
) engine=innodb comment = '购物车表';

-- ----------------------------
-- 8、收货地址表
-- ----------------------------
drop table if exists ct_address;
create table ct_address (
  address_id      bigint(20)      not null auto_increment    comment '地址ID',
  user_id         bigint(20)      not null                   comment '用户ID',
  receiver        varchar(32)     not null                   comment '收货人',
  phone           varchar(20)     not null                   comment '联系电话',
  school_id       bigint(20)      default null               comment '学校ID',
  detail          varchar(255)    not null                   comment '详细地址',
  is_default      char(1)         default '0'                comment '是否默认（0否 1是）',
  del_flag        char(1)         default '0'                comment '删除标志（0存在 2删除）',
  create_by       varchar(64)     default ''                 comment '创建者',
  create_time     datetime                                   comment '创建时间',
  update_by       varchar(64)     default ''                 comment '更新者',
  update_time     datetime                                   comment '更新时间',
  primary key (address_id),
  key idx_ct_address_user (user_id)
) engine=innodb comment = '收货地址表';

-- ----------------------------
-- 9、订单主表（按卖家拆单，一条记录 = 一个卖家子订单）
-- ----------------------------
drop table if exists ct_order;
create table ct_order (
  order_id        bigint(20)      not null auto_increment    comment '订单ID',
  order_no        varchar(32)     not null                   comment '订单号（业务唯一）',
  buyer_id        bigint(20)      not null                   comment '买家用户ID',
  seller_id       bigint(20)      not null                   comment '卖家用户ID（拆单后每单一个卖家）',
  group_no        varchar(64)     default null               comment '下单批次号/幂等键（一次结算的多个子订单共用）',
  total_amount    decimal(10,2)   not null                   comment '商品总额（元）',
  delivery_fee    decimal(10,2)   default 0.00               comment '配送费（元）',
  pay_amount      decimal(10,2)   not null                   comment '应付金额（元，服务端重算）',
  status          char(1)         default '0'                comment '状态（0待付款 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款）',
  delivery_type   char(1)         default '0'                comment '配送方式（0自提 1配送）',
  receiver        varchar(32)     default ''                 comment '收货人快照',
  receiver_phone  varchar(20)     default ''                 comment '收货电话快照',
  address_detail  varchar(255)    default ''                 comment '收货地址快照',
  remark          varchar(255)    default ''                 comment '买家备注',
  idempotency_key varchar(64)     default null               comment '下单幂等键',
  pay_time        datetime                                   comment '支付时间',
  ship_time       datetime                                   comment '发货时间',
  finish_time     datetime                                   comment '完成时间',
  cancel_time     datetime                                   comment '取消时间',
  cancel_reason   varchar(255)    default ''                 comment '取消原因',
  del_flag        char(1)         default '0'                comment '删除标志（0存在 2删除）',
  create_by       varchar(64)     default ''                 comment '创建者',
  create_time     datetime                                   comment '创建时间',
  update_by       varchar(64)     default ''                 comment '更新者',
  update_time     datetime                                   comment '更新时间',
  primary key (order_id),
  unique key uk_ct_order_no (order_no),
  unique key uk_ct_order_idem (idempotency_key),
  -- 同一次结算（group_no）内，每个卖家只能有一条子订单：防止并发下重复建单
  unique key uk_ct_order_group_seller (group_no, seller_id),
  key idx_ct_order_buyer (buyer_id),
  key idx_ct_order_seller (seller_id),
  key idx_ct_order_status (status),
  key idx_ct_order_group (group_no)
) engine=innodb comment = '订单主表';

-- ----------------------------
-- 10、订单明细表
-- ----------------------------
drop table if exists ct_order_item;
create table ct_order_item (
  item_id         bigint(20)      not null auto_increment    comment '明细ID',
  order_id        bigint(20)      not null                   comment '订单ID',
  order_no        varchar(32)     not null                   comment '订单号',
  product_id      bigint(20)      not null                   comment '商品ID',
  title           varchar(128)    default ''                 comment '商品标题快照',
  cover_url       varchar(255)    default ''                 comment '商品封面快照',
  price           decimal(10,2)   not null                   comment '成交单价快照（元）',
  quantity        int(11)         default 1                  comment '数量',
  total_amount    decimal(10,2)   not null                   comment '小计（元）',
  create_time     datetime                                   comment '创建时间',
  primary key (item_id),
  key idx_ct_order_item_order (order_id),
  key idx_ct_order_item_product (product_id)
) engine=innodb comment = '订单明细表';

-- ----------------------------
-- 11、支付单表
-- ----------------------------
drop table if exists ct_payment;
create table ct_payment (
  payment_id      bigint(20)      not null auto_increment    comment '支付ID',
  pay_no          varchar(32)     not null                   comment '支付单号（业务唯一）',
  order_no        varchar(32)     not null                   comment '订单号',
  user_id         bigint(20)      not null                   comment '支付用户ID',
  amount          decimal(10,2)   not null                   comment '支付金额（元）',
  pay_method      char(1)         default '0'                comment '支付方式（0沙箱 1支付宝 2微信 3余额）',
  status          char(1)         default '0'                comment '状态（0待支付 1支付成功 2支付失败 3已退款）',
  third_party_no  varchar(64)     default ''                 comment '第三方交易号',
  callback_time   datetime                                   comment '回调时间',
  create_time     datetime                                   comment '创建时间',
  update_time     datetime                                   comment '更新时间',
  primary key (payment_id),
  unique key uk_ct_payment_no (pay_no),
  key idx_ct_payment_order (order_no),
  key idx_ct_payment_user (user_id)
) engine=innodb comment = '支付单表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 学校（示例；真实学校名单以业务录入为准）
insert into ct_school values(100, '示范大学', '本校', '本市', 1, '0', '0', 'admin', sysdate(), '', null, '演示数据');
insert into ct_school values(101, '邻校学院', '邻校', '本市', 2, '0', '0', 'admin', sysdate(), '', null, '演示数据');

-- 商品类目（对应前端 products.js 的分类）
insert into ct_category values(100, 0, '教材书籍', 'book',    1, '0', '0', 'admin', sysdate(), '', null, null);
insert into ct_category values(101, 0, '数码电子', 'monitor', 2, '0', '0', 'admin', sysdate(), '', null, null);
insert into ct_category values(102, 0, '宿舍生活', 'coffee',  3, '0', '0', 'admin', sysdate(), '', null, null);
insert into ct_category values(103, 0, '运动出行', 'bike',    4, '0', '0', 'admin', sysdate(), '', null, null);
insert into ct_category values(104, 0, '服饰好物', 'bag',     5, '0', '0', 'admin', sysdate(), '', null, null);

-- ============================================================
-- 字典（若依 sys_dict_type / sys_dict_data）
-- 说明：如需在若依后台维护枚举，直接导入以下字典；不需要可整段删除。
-- ============================================================

insert into sys_dict_type values(100, '商品成色', 'ct_condition', '0', 'admin', sysdate(), '', null, '商品成色');
insert into sys_dict_type values(101, '商品状态', 'ct_product_status', '0', 'admin', sysdate(), '', null, '商品审核与上架状态');
insert into sys_dict_type values(102, '订单状态', 'ct_order_status', '0', 'admin', sysdate(), '', null, '订单状态');
insert into sys_dict_type values(103, '配送方式', 'ct_delivery_type', '0', 'admin', sysdate(), '', null, '配送方式');
insert into sys_dict_type values(104, '支付方式', 'ct_pay_method', '0', 'admin', sysdate(), '', null, '支付方式');
insert into sys_dict_type values(105, '认证状态', 'ct_verify_status', '0', 'admin', sysdate(), '', null, '学号认证状态');

insert into sys_dict_data values(100, 1, '几乎全新', 'almost_new', 'ct_condition', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '几乎全新');
insert into sys_dict_data values(101, 2, '九成新',   'nine',       'ct_condition', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '九成新');
insert into sys_dict_data values(102, 3, '八成新',   'eight',      'ct_condition', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '八成新');

insert into sys_dict_data values(110, 1, '待审核', '0', 'ct_product_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '待审核');
insert into sys_dict_data values(111, 2, '已上架', '1', 'ct_product_status', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '已上架');
insert into sys_dict_data values(112, 3, '已下架', '2', 'ct_product_status', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '已下架');
insert into sys_dict_data values(113, 4, '审核驳回', '3', 'ct_product_status', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '审核驳回');
insert into sys_dict_data values(114, 5, '已售出', '4', 'ct_product_status', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '已售出');

insert into sys_dict_data values(120, 1, '待付款', '0', 'ct_order_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '待付款');
insert into sys_dict_data values(121, 2, '待发货', '1', 'ct_order_status', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '待发货');
insert into sys_dict_data values(122, 3, '待收货', '2', 'ct_order_status', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '待收货');
insert into sys_dict_data values(123, 4, '已完成', '3', 'ct_order_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已完成');
insert into sys_dict_data values(124, 5, '已取消', '4', 'ct_order_status', '', 'info',    'N', '0', 'admin', sysdate(), '', null, '已取消');
insert into sys_dict_data values(125, 6, '退款中', '5', 'ct_order_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '退款中');
insert into sys_dict_data values(126, 7, '已退款', '6', 'ct_order_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已退款');

insert into sys_dict_data values(130, 1, '自提', '0', 'ct_delivery_type', '', 'default', 'Y', '0', 'admin', sysdate(), '', null, '自提');
insert into sys_dict_data values(131, 2, '配送', '1', 'ct_delivery_type', '', 'default', 'N', '0', 'admin', sysdate(), '', null, '配送');

insert into sys_dict_data values(140, 1, '沙箱支付', '0', 'ct_pay_method', '', 'default', 'Y', '0', 'admin', sysdate(), '', null, '沙箱支付');
insert into sys_dict_data values(141, 2, '支付宝',   '1', 'ct_pay_method', '', 'default', 'N', '0', 'admin', sysdate(), '', null, '支付宝');
insert into sys_dict_data values(142, 3, '微信',     '2', 'ct_pay_method', '', 'default', 'N', '0', 'admin', sysdate(), '', null, '微信');
insert into sys_dict_data values(143, 4, '余额',     '3', 'ct_pay_method', '', 'default', 'N', '0', 'admin', sysdate(), '', null, '余额');

insert into sys_dict_data values(150, 1, '未认证', '0', 'ct_verify_status', '', 'info',    'Y', '0', 'admin', sysdate(), '', null, '未认证');
insert into sys_dict_data values(151, 2, '待审核', '1', 'ct_verify_status', '', 'warning', 'N', '0', 'admin', sysdate(), '', null, '待审核');
insert into sys_dict_data values(152, 3, '已认证', '2', 'ct_verify_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已认证');
insert into sys_dict_data values(153, 4, '已驳回', '3', 'ct_verify_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已驳回');

-- --------------------------------------------------------------
-- 12、退款/售后表
-- status: 0待审核 1已同意 2已拒绝 3已退款完成
-- order_status_before 保存申请前的订单状态，拒绝时回滚
create table ct_refund (
  refund_id           bigint(20)      not null auto_increment    comment '退款ID',
  refund_no           varchar(32)     not null                   comment '退款单号（业务唯一）',
  order_id            bigint(20)      not null                   comment '订单ID',
  order_no            varchar(32)     not null                   comment '订单号',
  user_id             bigint(20)      not null                   comment '申请人（买家）',
  seller_id           bigint(20)      not null                   comment '卖家用户ID',
  reason              varchar(500)    default ''                 comment '退款原因',
  refund_amount       decimal(10,2)   not null                   comment '退款金额（元）',
  status              char(1)         default '0'                comment '状态（0待审核 1已同意 2已拒绝 3已退款完成）',
  order_status_before char(1)         default '0'                comment '申请前订单状态（拒绝时回滚用）',
  handle_by           varchar(64)     default ''                 comment '处理人（卖家）',
  handle_time         datetime                                   comment '处理时间',
  handle_remark       varchar(255)    default ''                 comment '处理说明/拒绝原因',
  del_flag            char(1)         default '0'                comment '删除标志（0存在 2删除）',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime                                   comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime                                   comment '更新时间',
  primary key (refund_id),
  unique key uk_ct_refund_no (refund_no),
  key idx_ct_refund_order (order_no),
  key idx_ct_refund_user (user_id),
  key idx_ct_refund_seller (seller_id)
) engine=innodb comment = '退款/售后表';

insert into sys_dict_type values(107, '退款状态', 'ct_refund_status', '0', 'admin', sysdate(), '', null, '退款状态');

insert into sys_dict_data values(170, 1, '待审核', '0', 'ct_refund_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '', null, '待审核');
insert into sys_dict_data values(171, 2, '已同意', '1', 'ct_refund_status', '', 'primary', 'N', '0', 'admin', sysdate(), '', null, '已同意');
insert into sys_dict_data values(172, 3, '已拒绝', '2', 'ct_refund_status', '', 'danger',  'N', '0', 'admin', sysdate(), '', null, '已拒绝');
insert into sys_dict_data values(173, 4, '已退款完成', '3', 'ct_refund_status', '', 'success', 'N', '0', 'admin', sysdate(), '', null, '已退款完成');

-- --------------------------------------------------------------
-- 13、下单幂等表
-- 由数据库唯一键保证「同一幂等键只会产生一批订单」，避免并发重复下单。
-- status: 0处理中（订单尚未创建完成） 1已完成
-- 说明：过期键可由定时任务按 create_time 清理，当前保留记录不做清理。
create table ct_idempotency (
  idem_id     bigint(20)   not null auto_increment    comment '幂等ID',
  idem_key    varchar(128) not null                   comment '客户端幂等键',
  group_no    varchar(64)  default null               comment '下单批次号（创建完成后回写）',
  status      char(1)      default '0'                comment '状态（0处理中 1已完成）',
  create_time datetime                                comment '创建时间',
  primary key (idem_id),
  unique key uk_ct_idem_key (idem_key)
) engine=innodb comment = '下单幂等表';

-- --------------------------------------------------------------
-- 升级脚本：已建过旧版 ct_order 的环境执行一次
-- group_no 需要容纳 "G" + 32 位 UUID（下单批次号/幂等键）
-- ALTER TABLE ct_order MODIFY COLUMN group_no varchar(64) DEFAULT NULL COMMENT '下单批次号/幂等键（一次结算的多个子订单共用）';
