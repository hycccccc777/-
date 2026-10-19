package com.ruoyi.campus.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.domain.CtAddress;
import com.ruoyi.campus.domain.CtCartItem;
import com.ruoyi.campus.domain.CtIdempotency;
import com.ruoyi.campus.domain.CtOrder;
import com.ruoyi.campus.domain.CtOrderCreateBody;
import com.ruoyi.campus.domain.CtOrderItem;
import com.ruoyi.campus.domain.vo.CtProductVo;
import com.ruoyi.campus.mapper.CtCartItemMapper;
import com.ruoyi.campus.mapper.CtIdempotencyMapper;
import com.ruoyi.campus.mapper.CtOrderItemMapper;
import com.ruoyi.campus.mapper.CtOrderMapper;
import com.ruoyi.campus.service.ICtAddressService;
import com.ruoyi.campus.service.ICtOrderService;
import com.ruoyi.campus.service.ICtProductService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.service.ISysUserService;

/**
 * 订单 服务层实现
 * 
 * @author campus
 */
@Service
public class CtOrderServiceImpl implements ICtOrderService
{
    private static final Logger log = LoggerFactory.getLogger(CtOrderServiceImpl.class);

    private static final Map<String, String> ORDER_STATUS = new HashMap<>();

    static
    {
        ORDER_STATUS.put("0", "待付款");
        ORDER_STATUS.put("1", "待发货");
        ORDER_STATUS.put("2", "待收货");
        ORDER_STATUS.put("3", "已完成");
        ORDER_STATUS.put("4", "已取消");
        ORDER_STATUS.put("5", "退款中");
        ORDER_STATUS.put("6", "已退款");
    }

    @Autowired
    private CtOrderMapper ctOrderMapper;

    @Autowired
    private CtIdempotencyMapper ctIdempotencyMapper;

    @Autowired
    private CtOrderItemMapper ctOrderItemMapper;

    @Autowired
    private CtCartItemMapper ctCartItemMapper;

    @Autowired
    private ICtProductService productService;

    @Autowired
    private ICtAddressService addressService;

    @Autowired
    private ISysUserService userService;

    @Override
    public List<CtOrder> selectCtOrderList(CtOrder ctOrder)
    {
        return ctOrderMapper.selectCtOrderList(ctOrder);
    }

    @Override
    public CtOrder selectCtOrderById(Long orderId)
    {
        return ctOrderMapper.selectCtOrderById(orderId);
    }

    @Override
    public int insertCtOrder(CtOrder ctOrder)
    {
        return ctOrderMapper.insertCtOrder(ctOrder);
    }

    @Override
    public int updateCtOrder(CtOrder ctOrder)
    {
        return ctOrderMapper.updateCtOrder(ctOrder);
    }

    @Override
    public int deleteCtOrderByIds(Long[] orderIds)
    {
        for (Long orderId : orderIds)
        {
            ctOrderMapper.deleteCtOrderById(orderId);
        }
        return orderIds.length;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOrder(CtOrderCreateBody body)
    {
        Long userId = SecurityUtils.getUserId();
        String key = body == null || body.getIdempotencyKey() == null ? "" : body.getIdempotencyKey().trim();
        if (key.length() > 64)
        {
            // group_no 列长度为 64，超长直接截断
            key = key.substring(0, 64);
        }

        // 幂等：先抢占幂等键。并发场景下由 DB 唯一键 uk_ct_idem_key 保证只有第一个请求能进入下单流程，
        // 其余请求要么拿到原批次（重放），要么被明确拒绝，不会重复扣减库存。
        if (!key.isEmpty() && !acquireIdempotency(key))
        {
            return replayOrReject(key);
        }

        if (body == null || body.getCartItemIds() == null || body.getCartItemIds().isEmpty())
        {
            throw new ApiException(400, "请选择要结算的商品");
        }
        Set<Long> wanted = new HashSet<>(body.getCartItemIds());

        CtCartItem query = new CtCartItem();
        query.setUserId(userId);
        List<CtCartItem> owned = ctCartItemMapper.selectCtCartItemList(query);
        Map<Long, CtCartItem> ownedMap = new HashMap<>();
        for (CtCartItem item : owned)
        {
            ownedMap.put(item.getCartItemId(), item);
        }
        if (!ownedMap.keySet().containsAll(wanted))
        {
            throw new ApiException(400, "购物车条目已变化，请刷新后重试");
        }

        String[] contact = resolveContact(body, userId);
        String deliveryType = StringUtils.isEmpty(body.getDeliveryType()) ? "0" : body.getDeliveryType();
        String remark = body.getRemark() == null ? "" : body.getRemark();
        String groupNo = key.isEmpty() ? "G" + IdUtils.fastSimpleUUID() : key;

        // 按卖家分组
        Map<Long, List<Map.Entry<CtCartItem, CtProductVo>>> groups = new java.util.LinkedHashMap<>();
        for (Long cartItemId : wanted)
        {
            CtCartItem item = ownedMap.get(cartItemId);
            CtProductVo product = productService.selectCtProductVoById(item.getProductId());
            if (product == null || !"1".equals(product.getStatus()))
            {
                throw new ApiException(409, "商品「" + item.getProductId() + "」已下架，请移出购物车");
            }
            if (userId.equals(product.getSellerId()))
            {
                throw new ApiException(400, "不能购买自己发布的商品");
            }
            int qty = item.getQuantity() == null ? 1 : item.getQuantity();
            // 库存不做预检：预检是非锁定快照读，并发下会读到过期数据而误拒本应成功的请求。
            // 库存是否充足一律由 deductStock 的原子条件更新判定（唯一权威）。
            groups.computeIfAbsent(product.getSellerId(), k -> new ArrayList<>()).add(Map.entry(item, product));
        }

        List<CtOrder> created = new ArrayList<>();
        for (Map.Entry<Long, List<Map.Entry<CtCartItem, CtProductVo>>> group : groups.entrySet())
        {
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<CtCartItem, CtProductVo> e : group.getValue())
            {
                int qty = e.getKey().getQuantity() == null ? 1 : e.getKey().getQuantity();
                total = total.add(e.getValue().getPrice().multiply(BigDecimal.valueOf(qty)));
            }
            total = total.setScale(2, RoundingMode.HALF_UP);
            BigDecimal deliveryFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            // 先原子扣减库存：任一商品扣减失败立即抛错，事务整体回滚，不会出现部分建单
            for (Map.Entry<CtCartItem, CtProductVo> e : group.getValue())
            {
                CtCartItem item = e.getKey();
                CtProductVo product = e.getValue();
                int qty = item.getQuantity() == null ? 1 : item.getQuantity();
                if (!productService.deductStock(product.getProductId(), qty))
                {
                    throw new ApiException(409, "商品「" + product.getTitle() + "」库存不足或已下架");
                }
            }

            CtOrder order = new CtOrder();
            order.setOrderNo(generateOrderNo());
            order.setBuyerId(userId);
            order.setSellerId(group.getKey());
            order.setGroupNo(groupNo);
            order.setTotalAmount(total);
            order.setDeliveryFee(deliveryFee);
            order.setPayAmount(total.add(deliveryFee));
            order.setStatus("0");
            order.setDeliveryType(deliveryType);
            order.setReceiver(contact[0]);
            order.setReceiverPhone(contact[1]);
            order.setAddressDetail(contact[2]);
            order.setRemark(remark);
            order.setCreateBy(SecurityUtils.getUsername());
            ctOrderMapper.insertCtOrder(order);

            for (Map.Entry<CtCartItem, CtProductVo> e : group.getValue())
            {
                CtCartItem item = e.getKey();
                CtProductVo product = e.getValue();
                int qty = item.getQuantity() == null ? 1 : item.getQuantity();
                BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

                CtOrderItem line = new CtOrderItem();
                line.setOrderId(order.getOrderId());
                line.setOrderNo(order.getOrderNo());
                line.setProductId(product.getProductId());
                line.setTitle(product.getTitle());
                line.setCoverUrl(product.getCoverUrl());
                line.setPrice(product.getPrice());
                line.setQuantity(qty);
                line.setTotalAmount(subtotal);
                ctOrderItemMapper.insertCtOrderItem(line);

                ctCartItemMapper.deleteCtCartItemById(item.getCartItemId());
            }
            created.add(order);
        }
        if (!key.isEmpty())
        {
            finishIdempotency(key, groupNo);
        }
        return buildResult(groupNo, created);
    }

    /**
     * 抢占幂等键：插入成功即代表本请求拥有该键。
     *
     * @return true=抢占成功；false=该键已被其他请求占用
     */
    private boolean acquireIdempotency(String key)
    {
        try
        {
            CtIdempotency record = new CtIdempotency();
            record.setIdemKey(key);
            record.setStatus("0");
            ctIdempotencyMapper.insertCtIdempotency(record);
            return true;
        }
        catch (DuplicateKeyException e)
        {
            log.info("幂等键[{}]已被占用，当前请求不进入下单流程", key);
            return false;
        }
    }

    /**
     * 幂等键已被占用时的处理：已完成则重放原批次，处理中则拒绝。
     */
    private Map<String, Object> replayOrReject(String key)
    {
        CtIdempotency record = ctIdempotencyMapper.selectCtIdempotencyByKey(key);
        if (record != null && "1".equals(record.getStatus()) && StringUtils.isNotEmpty(record.getGroupNo()))
        {
            CtOrder probe = new CtOrder();
            probe.setGroupNo(record.getGroupNo());
            List<CtOrder> orders = ctOrderMapper.selectCtOrderList(probe);
            if (!orders.isEmpty())
            {
                log.info("幂等键[{}]重放原批次[{}]，共{}笔订单", key, record.getGroupNo(), orders.size());
                return buildResult(record.getGroupNo(), orders);
            }
        }
        throw new ApiException(409, "订单正在创建中，请勿重复提交");
    }

    private void finishIdempotency(String key, String groupNo)
    {
        CtIdempotency record = new CtIdempotency();
        record.setIdemKey(key);
        record.setGroupNo(groupNo);
        record.setStatus("1");
        ctIdempotencyMapper.updateCtIdempotency(record);
    }

    @Override
    public List<Map<String, Object>> listMyOrders(Long userId)
    {
        CtOrder query = new CtOrder();
        query.setBuyerId(userId);
        return toMaps(ctOrderMapper.selectCtOrderList(query));
    }

    @Override
    public List<Map<String, Object>> listMySales(Long userId)
    {
        CtOrder query = new CtOrder();
        query.setSellerId(userId);
        return toMaps(ctOrderMapper.selectCtOrderList(query));
    }

    @Override
    public Map<String, Object> orderDetail(Long userId, String orderNo)
    {
        CtOrder order = requireVisibleOrder(userId, orderNo);
        return toOrderMap(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, String orderNo, String reason)
    {
        CtOrder order = ctOrderMapper.selectCtOrderByOrderNo(orderNo);
        if (order == null || !userId.equals(order.getBuyerId()))
        {
            throw new ApiException(404, "订单不存在");
        }
        if (!"0".equals(order.getStatus()))
        {
            throw new ApiException(409, "订单当前状态（" + ORDER_STATUS.get(order.getStatus()) + "）不可取消");
        }
        order.setStatus("4");
        order.setCancelTime(new Date());
        order.setCancelReason(StringUtils.isEmpty(reason) ? "买家取消" : reason);
        order.setUpdateBy(SecurityUtils.getUsername());
        ctOrderMapper.updateCtOrder(order);
        restoreStock(order);
    }

    @Override
    public void confirmOrder(Long userId, String orderNo)
    {
        CtOrder order = ctOrderMapper.selectCtOrderByOrderNo(orderNo);
        if (order == null || !userId.equals(order.getBuyerId()))
        {
            throw new ApiException(404, "订单不存在");
        }
        if (!"2".equals(order.getStatus()))
        {
            throw new ApiException(409, "订单当前状态（" + ORDER_STATUS.get(order.getStatus()) + "）不可确认收货");
        }
        order.setStatus("3");
        order.setFinishTime(new Date());
        order.setUpdateBy(SecurityUtils.getUsername());
        ctOrderMapper.updateCtOrder(order);
    }

    @Override
    public void shipOrder(Long userId, String orderNo)
    {
        CtOrder order = ctOrderMapper.selectCtOrderByOrderNo(orderNo);
        if (order == null || !userId.equals(order.getSellerId()))
        {
            throw new ApiException(404, "订单不存在");
        }
        if (!"1".equals(order.getStatus()))
        {
            throw new ApiException(409, "订单当前状态（" + ORDER_STATUS.get(order.getStatus()) + "）不可发货");
        }
        order.setStatus("2");
        order.setShipTime(new Date());
        order.setUpdateBy(SecurityUtils.getUsername());
        ctOrderMapper.updateCtOrder(order);
    }

    private String[] resolveContact(CtOrderCreateBody body, Long userId)
    {
        if (body.getAddressId() != null)
        {
            CtAddress address = addressService.selectCtAddressById(body.getAddressId());
            if (address == null || !userId.equals(address.getUserId()))
            {
                throw new ApiException(400, "收货地址不存在");
            }
            return new String[] { address.getReceiver(), address.getPhone(), address.getDetail() };
        }
        String receiver = body.getReceiver() == null ? "" : body.getReceiver().trim();
        String phone = body.getPhone() == null ? "" : body.getPhone().trim();
        String detail = body.getAddressDetail() == null ? "" : body.getAddressDetail().trim();
        if (receiver.isEmpty() || phone.isEmpty() || detail.isEmpty())
        {
            throw new ApiException(400, "请填写收货人、联系电话与收货地址");
        }
        if (!phone.matches("^1[3-9]\\d{9}$"))
        {
            throw new ApiException(400, "请输入有效的 11 位手机号");
        }
        return new String[] { receiver, phone, detail };
    }

    private void restoreStock(CtOrder order)
    {
        if (order.getOrderItems() == null)
        {
            return;
        }
        for (CtOrderItem line : order.getOrderItems())
        {
            // 原子加回；商品若因售罄下架则同时恢复上架
            productService.restoreStock(line.getProductId(), line.getQuantity() == null ? 1 : line.getQuantity());
        }
    }

    private CtOrder requireVisibleOrder(Long userId, String orderNo)
    {
        CtOrder order = ctOrderMapper.selectCtOrderByOrderNo(orderNo);
        if (order == null || (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())))
        {
            throw new ApiException(404, "订单不存在");
        }
        return order;
    }

    private Map<String, Object> buildResult(String groupNo, List<CtOrder> orders)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CtOrder order : orders)
        {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderNo", order.getOrderNo());
            m.put("sellerId", String.valueOf(order.getSellerId()));
            m.put("sellerName", sellerName(order.getSellerId()));
            m.put("status", order.getStatus());
            m.put("statusText", ORDER_STATUS.get(order.getStatus()));
            m.put("totalAmount", order.getTotalAmount());
            m.put("payAmount", order.getPayAmount());
            list.add(m);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("groupNo", groupNo);
        data.put("orders", list);
        return data;
    }

    private List<Map<String, Object>> toMaps(List<CtOrder> orders)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CtOrder order : orders)
        {
            list.add(toOrderMap(order));
        }
        return list;
    }

    private Map<String, Object> toOrderMap(CtOrder order)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderNo", order.getOrderNo());
        m.put("status", order.getStatus());
        m.put("statusText", ORDER_STATUS.get(order.getStatus()));
        m.put("deliveryType", order.getDeliveryType());
        m.put("totalAmount", order.getTotalAmount());
        m.put("deliveryFee", order.getDeliveryFee());
        m.put("payAmount", order.getPayAmount());
        m.put("receiver", order.getReceiver());
        m.put("receiverPhone", order.getReceiverPhone());
        m.put("addressDetail", order.getAddressDetail());
        m.put("remark", order.getRemark());
        m.put("sellerId", String.valueOf(order.getSellerId()));
        m.put("sellerName", sellerName(order.getSellerId()));
        m.put("createTime", order.getCreateTime());
        List<Map<String, Object>> items = new ArrayList<>();
        if (order.getOrderItems() != null)
        {
            for (CtOrderItem line : order.getOrderItems())
            {
                Map<String, Object> li = new LinkedHashMap<>();
                li.put("productId", String.valueOf(line.getProductId()));
                li.put("title", line.getTitle());
                li.put("coverUrl", line.getCoverUrl());
                li.put("price", line.getPrice());
                li.put("quantity", line.getQuantity());
                li.put("totalAmount", line.getTotalAmount());
                items.add(li);
            }
        }
        m.put("items", items);
        return m;
    }

    private String sellerName(Long sellerId)
    {
        SysUser user = userService.selectUserById(sellerId);
        return user == null ? "" : user.getNickName();
    }

    private String generateOrderNo()
    {
        return "CT" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + IdUtils.fastSimpleUUID().substring(0, 6).toUpperCase();
    }
}