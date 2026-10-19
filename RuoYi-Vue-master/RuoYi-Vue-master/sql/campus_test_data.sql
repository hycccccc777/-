-- ==============================================================
-- 校园二手交易系统 自动化测试数据
-- 目标库：shop_test（由 scripts/setup-test-db.ps1 创建）
-- 用途：并发下单（超卖/重复下单）、全链路回归
-- 注意：仅供测试，绝不可导入生产库
-- ==============================================================

-- --------------------------------------------------------------
-- 测试卖家
-- --------------------------------------------------------------
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark)
values (2100, 100, 'test_seller', '测试卖家', '00', '', '13900009000', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试卖家');

-- --------------------------------------------------------------
-- 20 个测试买家（手机号 13900000001 ~ 13900000020），密码均为 admin123
-- 每个买家彼此独立，可并发下单同一商品以复现超卖
-- --------------------------------------------------------------
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2001, 100, 'test_buyer_01', '测试买家01', '00', '', '13900000001', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2002, 100, 'test_buyer_02', '测试买家02', '00', '', '13900000002', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2003, 100, 'test_buyer_03', '测试买家03', '00', '', '13900000003', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2004, 100, 'test_buyer_04', '测试买家04', '00', '', '13900000004', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2005, 100, 'test_buyer_05', '测试买家05', '00', '', '13900000005', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2006, 100, 'test_buyer_06', '测试买家06', '00', '', '13900000006', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2007, 100, 'test_buyer_07', '测试买家07', '00', '', '13900000007', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2008, 100, 'test_buyer_08', '测试买家08', '00', '', '13900000008', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2009, 100, 'test_buyer_09', '测试买家09', '00', '', '13900000009', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2010, 100, 'test_buyer_10', '测试买家10', '00', '', '13900000010', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2011, 100, 'test_buyer_11', '测试买家11', '00', '', '13900000011', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2012, 100, 'test_buyer_12', '测试买家12', '00', '', '13900000012', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2013, 100, 'test_buyer_13', '测试买家13', '00', '', '13900000013', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2014, 100, 'test_buyer_14', '测试买家14', '00', '', '13900000014', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2015, 100, 'test_buyer_15', '测试买家15', '00', '', '13900000015', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2016, 100, 'test_buyer_16', '测试买家16', '00', '', '13900000016', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2017, 100, 'test_buyer_17', '测试买家17', '00', '', '13900000017', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2018, 100, 'test_buyer_18', '测试买家18', '00', '', '13900000018', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2019, 100, 'test_buyer_19', '测试买家19', '00', '', '13900000019', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');
insert into sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, remark) values (2020, 100, 'test_buyer_20', '测试买家20', '00', '', '13900000020', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate(), '自动化测试买家');

-- --------------------------------------------------------------
-- 测试商品
-- 9001：库存 1 的限量商品，用于复现「超卖」
-- 9002~9006：库存 10 的普通商品，用于全链路回归
-- --------------------------------------------------------------
insert into ct_product (product_id, title, category_id, seller_id, seller_type, price, original_price, stock, condition_code, school_id, location, cover_url, tag, description, status, view_count, favorite_count, del_flag, create_by, create_time) values
(9001, '并发测试用限量商品（库存 1）', 100, 2100, '0', 99.00, 199.00, 1, 'nine', 100, '测试交接点', '', '限量', '仅用于自动化测试复现超卖，不对外展示。', '1', 0, 0, '0', 'admin', sysdate()),
(9002, '测试商品-教材', 100, 2100, '0', 25.00, 96.00, 10, 'nine', 100, '图书馆南门', '', '测试', '用于全链路回归测试的教材类商品。', '1', 0, 0, '0', 'admin', sysdate()),
(9003, '测试商品-数码', 101, 2100, '0', 599.00, 1299.00, 10, 'almost_new', 100, '东区宿舍', '', '测试', '用于全链路回归测试的数码类商品。', '1', 0, 0, '0', 'admin', sysdate()),
(9004, '测试商品-生活', 102, 2100, '0', 39.00, 129.00, 10, 'eight', 101, '西区楼下', '', '测试', '用于全链路回归测试的生活类商品。', '1', 0, 0, '0', 'admin', sysdate()),
(9005, '测试商品-运动', 103, 2100, '0', 260.00, 799.00, 10, 'eight', 101, '体育馆', '', '测试', '用于全链路回归测试的运动类商品。', '1', 0, 0, '0', 'admin', sysdate()),
(9006, '测试商品-服饰', 104, 2100, '0', 18.00, 59.00, 10, 'almost_new', 100, '第二食堂', '', '测试', '用于全链路回归测试的服饰类商品。', '1', 0, 0, '0', 'admin', sysdate());
