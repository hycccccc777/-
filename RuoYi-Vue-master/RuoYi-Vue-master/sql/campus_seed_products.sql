-- ==============================================================
-- 校园二手交易系统 演示种子数据（商品 + 卖家）
-- 依赖：先执行 ry_20260417.sql、campus_trade.sql
-- 说明：4 个演示卖家密码均为 admin123（与 admin 相同的 BCrypt 散列）。
--       仅用于本地联调演示，正式环境请勿导入。
-- ==============================================================

-- 演示卖家
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark)
values (1001, 100, 'xiaolin', '小林同学', '00', '', '13800001001', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '演示卖家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark)
values (1002, 100, 'aze', '阿泽', '00', '', '13800001002', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '演示卖家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark)
values (1003, 100, 'xiaocheng', '一颗小橙', '00', '', '13800001003', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '演示卖家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark)
values (1004, 100, 'qiche', '骑车去看海', '00', '', '13800001004', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '演示卖家');

-- 演示商品（status=1 已上架）
insert into ct_product (product_id, title, category_id, seller_id, seller_type, price, original_price, stock, condition_code, school_id, location, cover_url, tag, description, status, view_count, favorite_count, del_flag, create_by, create_time) values
(1, '高等数学 · 同济第七版（上下册）', 100, 1001, '0', 25.00, 96.00, 1, 'nine', 100, '图书馆南门', '', '开学必备', '上下两册一起出，书页完整，少量铅笔笔记。适合新学期提前预习，可在图书馆南门交接。', '1', 128, 6, '0', 'admin', date_sub(sysdate(), interval 8 minute));
insert into ct_product (product_id, title, category_id, seller_id, seller_type, price, original_price, stock, condition_code, school_id, location, cover_url, tag, description, status, view_count, favorite_count, del_flag, create_by, create_time) values
(2, 'Sony WH-1000XM4 无线降噪耳机', 101, 1002, '0', 680.00, 1899.00, 1, 'nine', 100, '东区学生公寓', '', '', '自用耳机，降噪正常，附收纳盒和充电线。耳罩已清洁，外壳有轻微使用痕迹，支持当面试用。', '1', 342, 21, '0', 'admin', date_sub(sysdate(), interval 12 minute));
insert into ct_product (product_id, title, category_id, seller_id, seller_type, price, original_price, stock, condition_code, school_id, location, cover_url, tag, description, status, view_count, favorite_count, del_flag, create_by, create_time) values
(3, '复古奶油色台灯 · 三档调光', 102, 1003, '0', 39.00, 129.00, 1, 'almost_new', 100, '西区宿舍楼下', '', '好物推荐', '暖光很适合晚上看书，三档调光功能正常。毕业整理宿舍转让，不含桌面其他物品。', '1', 76, 4, '0', 'admin', date_sub(sysdate(), interval 18 minute));
insert into ct_product (product_id, title, category_id, seller_id, seller_type, price, original_price, stock, condition_code, school_id, location, cover_url, tag, description, status, view_count, favorite_count, del_flag, create_by, create_time) values
(4, '迪卡侬城市自行车 · 轻松骑去上课', 103, 1004, '0', 260.00, 799.00, 1, 'eight', 101, '体育馆自行车棚', '', '', '刹车和变速正常，轮胎上月换新。车架有使用划痕，建议现场试骑后决定。附一把车锁。', '1', 203, 12, '0', 'admin', date_sub(sysdate(), interval 24 minute));
insert into ct_product (product_id, title, category_id, seller_id, seller_type, price, original_price, stock, condition_code, school_id, location, cover_url, tag, description, status, view_count, favorite_count, del_flag, create_by, create_time) values
(5, 'Fujifilm mini 11 拍立得相机', 101, 1001, '0', 280.00, 599.00, 1, 'nine', 100, '大学生活动中心', '', '', '淡紫色机身，功能正常，附肩带。相纸需自行准备，喜欢记录生活的同学可以看看。', '1', 158, 9, '0', 'admin', date_sub(sysdate(), interval 35 minute));
insert into ct_product (product_id, title, category_id, seller_id, seller_type, price, original_price, stock, condition_code, school_id, location, cover_url, tag, description, status, view_count, favorite_count, del_flag, create_by, create_time) values
(6, '帆布托特包 · 装得下整个周末', 104, 1003, '0', 18.00, 59.00, 1, 'almost_new', 100, '第二食堂门口', '', '', '厚实帆布，内有小口袋。只背过两次，洗净后转让，可放下教材和平板。', '1', 61, 3, '0', 'admin', date_sub(sysdate(), interval 43 minute));
insert into ct_product (product_id, title, category_id, seller_id, seller_type, price, original_price, stock, condition_code, school_id, location, cover_url, tag, description, status, view_count, favorite_count, del_flag, create_by, create_time) values
(7, 'Python 编程：从入门到实践', 100, 1004, '0', 32.00, 89.00, 1, 'nine', 101, '北门快递站', '', '学习搭子', '适合编程入门，主要是前几章有笔记，其余页面干净。纸质书，不附赠账号或电子资源。', '1', 94, 5, '0', 'admin', date_sub(sysdate(), interval 56 minute));
insert into ct_product (product_id, title, category_id, seller_id, seller_type, price, original_price, stock, condition_code, school_id, location, cover_url, tag, description, status, view_count, favorite_count, del_flag, create_by, create_time) values
(8, '保温随行杯 500ml · 燕麦白', 102, 1002, '0', 28.00, 99.00, 1, 'almost_new', 100, '教学楼 A 座', '', '', '闲置备用杯，仅试装过水。杯盖密封正常，已清洗，建议收到后自行再次消毒。', '1', 47, 2, '0', 'admin', date_sub(sysdate(), interval 65 minute));
