-- ==============================================================
-- 校园二手交易系统 菜单与权限脚本
-- 依赖：先执行 sql/campus_trade.sql 建表
-- 说明：菜单ID从 2000 起，避免与若依默认菜单（1~1000+）冲突
--       权限标识格式：campus:{模块}:{操作}
-- ==============================================================

-- 一级目录
insert into sys_menu values('2000', '校园二手', '0', '4', 'campus', null, '', '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), '', null, '校园二手交易目录');

-- 学校管理
insert into sys_menu values('2001', '学校管理', '2000', '1', 'school', 'campus/school/index', '', '', 1, 0, 'C', '0', '0', 'campus:school:list', 'build', 'admin', sysdate(), '', null, '学校管理菜单');
insert into sys_menu values('2002', '学校查询', '2001', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:school:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2003', '学校新增', '2001', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:school:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2004', '学校修改', '2001', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:school:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2005', '学校删除', '2001', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:school:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2006', '学校导出', '2001', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:school:export', '#', 'admin', sysdate(), '', null, '');

-- 商品类目
insert into sys_menu values('2008', '商品类目', '2000', '2', 'category', 'campus/category/index', '', '', 1, 0, 'C', '0', '0', 'campus:category:list', 'tree', 'admin', sysdate(), '', null, '商品类目菜单');
insert into sys_menu values('2009', '类目查询', '2008', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:category:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2010', '类目新增', '2008', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:category:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2011', '类目修改', '2008', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:category:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2012', '类目删除', '2008', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:category:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2013', '类目导出', '2008', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:category:export', '#', 'admin', sysdate(), '', null, '');

-- 用户校园档案
insert into sys_menu values('2015', '校园档案', '2000', '3', 'profile', 'campus/profile/index', '', '', 1, 0, 'C', '0', '0', 'campus:profile:list', 'peoples', 'admin', sysdate(), '', null, '用户校园档案菜单');
insert into sys_menu values('2016', '档案查询', '2015', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:profile:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2017', '档案新增', '2015', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:profile:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2018', '档案修改', '2015', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:profile:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2019', '档案删除', '2015', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:profile:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2020', '档案导出', '2015', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:profile:export', '#', 'admin', sysdate(), '', null, '');

-- 商品管理
insert into sys_menu values('2022', '商品管理', '2000', '4', 'product', 'campus/product/index', '', '', 1, 0, 'C', '0', '0', 'campus:product:list', 'goods', 'admin', sysdate(), '', null, '商品管理菜单');
insert into sys_menu values('2023', '商品查询', '2022', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:product:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2024', '商品新增', '2022', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:product:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2025', '商品修改', '2022', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:product:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2026', '商品删除', '2022', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:product:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2027', '商品导出', '2022', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:product:export', '#', 'admin', sysdate(), '', null, '');

-- 商品图片
insert into sys_menu values('2029', '商品图片', '2000', '5', 'productImage', 'campus/productImage/index', '', '', 1, 0, 'C', '0', '0', 'campus:productImage:list', 'picture', 'admin', sysdate(), '', null, '商品图片菜单');
insert into sys_menu values('2030', '图片查询', '2029', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:productImage:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2031', '图片新增', '2029', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:productImage:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2032', '图片修改', '2029', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:productImage:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2033', '图片删除', '2029', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:productImage:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2034', '图片导出', '2029', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:productImage:export', '#', 'admin', sysdate(), '', null, '');

-- 商品收藏
insert into sys_menu values('2036', '商品收藏', '2000', '6', 'favorite', 'campus/favorite/index', '', '', 1, 0, 'C', '0', '0', 'campus:favorite:list', 'star', 'admin', sysdate(), '', null, '商品收藏菜单');
insert into sys_menu values('2037', '收藏查询', '2036', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:favorite:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2038', '收藏新增', '2036', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:favorite:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2039', '收藏删除', '2036', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:favorite:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2040', '收藏导出', '2036', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:favorite:export', '#', 'admin', sysdate(), '', null, '');

-- 购物车
insert into sys_menu values('2043', '购物车', '2000', '7', 'cartItem', 'campus/cartItem/index', '', '', 1, 0, 'C', '0', '0', 'campus:cartItem:list', 'shopping', 'admin', sysdate(), '', null, '购物车菜单');
insert into sys_menu values('2044', '购物车查询', '2043', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:cartItem:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2045', '购物车新增', '2043', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:cartItem:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2046', '购物车修改', '2043', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:cartItem:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2047', '购物车删除', '2043', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:cartItem:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2048', '购物车导出', '2043', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:cartItem:export', '#', 'admin', sysdate(), '', null, '');

-- 收货地址
insert into sys_menu values('2050', '收货地址', '2000', '8', 'address', 'campus/address/index', '', '', 1, 0, 'C', '0', '0', 'campus:address:list', 'location', 'admin', sysdate(), '', null, '收货地址菜单');
insert into sys_menu values('2051', '地址查询', '2050', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:address:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2052', '地址新增', '2050', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:address:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2053', '地址修改', '2050', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:address:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2054', '地址删除', '2050', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:address:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2055', '地址导出', '2050', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:address:export', '#', 'admin', sysdate(), '', null, '');

-- 订单管理
insert into sys_menu values('2057', '订单管理', '2000', '9', 'order', 'campus/order/index', '', '', 1, 0, 'C', '0', '0', 'campus:order:list', 'form', 'admin', sysdate(), '', null, '订单管理菜单');
insert into sys_menu values('2058', '订单查询', '2057', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:order:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2059', '订单新增', '2057', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:order:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2060', '订单修改', '2057', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:order:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2061', '订单删除', '2057', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:order:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2062', '订单导出', '2057', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:order:export', '#', 'admin', sysdate(), '', null, '');

-- 订单明细
insert into sys_menu values('2064', '订单明细', '2000', '10', 'orderItem', 'campus/orderItem/index', '', '', 1, 0, 'C', '0', '0', 'campus:orderItem:list', 'list', 'admin', sysdate(), '', null, '订单明细菜单');
insert into sys_menu values('2065', '明细查询', '2064', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:orderItem:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2066', '明细新增', '2064', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:orderItem:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2067', '明细修改', '2064', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:orderItem:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2068', '明细删除', '2064', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:orderItem:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2069', '明细导出', '2064', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:orderItem:export', '#', 'admin', sysdate(), '', null, '');

-- 支付单
insert into sys_menu values('2071', '支付单', '2000', '11', 'payment', 'campus/payment/index', '', '', 1, 0, 'C', '0', '0', 'campus:payment:list', 'money', 'admin', sysdate(), '', null, '支付单菜单');
insert into sys_menu values('2072', '支付查询', '2071', '1', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:payment:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2073', '支付新增', '2071', '2', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:payment:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2074', '支付修改', '2071', '3', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:payment:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2075', '支付删除', '2071', '4', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:payment:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2076', '支付导出', '2071', '5', '', null, '', '', 1, 0, 'F', '0', '0', 'campus:payment:export', '#', 'admin', sysdate(), '', null, '');
