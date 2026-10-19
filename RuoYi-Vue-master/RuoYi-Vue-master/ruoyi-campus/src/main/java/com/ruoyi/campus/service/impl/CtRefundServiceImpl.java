package com.ruoyi.campus.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.domain.CtOrder;
import com.ruoyi.campus.domain.CtOrderItem;
import com.ruoyi.campus.domain.CtRefund;
import com.ruoyi.campus.mapper.CtOrderMapper;
import com.ruoyi.campus.mapper.CtRefundMapper;
import com.ruoyi.campus.service.ICtProductService;
import com.ruoyi.campus.service.ICtRefundService;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * ???/??? ????????
 * 
 * @author campus
 */
@Service
public class CtRefundServiceImpl implements ICtRefundService
{
    /** ????????????????? */
    private static final List<String> REFUNDABLE = Arrays.asList("0", "1", "2");

    private static final Map<String, String> REFUND_STATUS = new HashMap<>();
    private static final Map<String, String> ORDER_STATUS = new HashMap<>();

    static
    {
        REFUND_STATUS.put("0", "?????");
        REFUND_STATUS.put("1", "?????");
        REFUND_STATUS.put("2", "????");
        REFUND_STATUS.put("3", "????????");
        ORDER_STATUS.put("0", "??????");
        ORDER_STATUS.put("1", "??????");
        ORDER_STATUS.put("2", "?????");
        ORDER_STATUS.put("3", "?????");
        ORDER_STATUS.put("4", "?????");
        ORDER_STATUS.put("5", "?????");
        ORDER_STATUS.put("6", "?????");
    }

    @Autowired
    private CtRefundMapper ctRefundMapper;

    @Autowired
    private CtOrderMapper ctOrderMapper;

    @Autowired
    private ICtProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apply(Long userId, String orderNo, String reason)
    {
        CtOrder order = ctOrderMapper.selectCtOrderByOrderNo(orderNo);
        if (order == null || !userId.equals(order.getBuyerId()))
        {
            throw new ApiException(404, "??????????");
        }
        if (!REFUNDABLE.contains(order.getStatus()))
        {
            throw new ApiException(409, "???????????" + ORDER_STATUS.get(order.getStatus()) + "??????????????");
        }
        if (hasActiveRefund(orderNo))
        {
            throw new ApiException(409, "????????н????е????????");
        }

        CtRefund refund = new CtRefund();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderId(order.getOrderId());
        refund.setOrderNo(orderNo);
        refund.setUserId(userId);
        refund.setSellerId(order.getSellerId());
        refund.setReason(StringUtils.isEmpty(reason) ? "δ??????" : reason);
        refund.setRefundAmount(order.getPayAmount());
        refund.setStatus("0");
        refund.setOrderStatusBefore(order.getStatus());
        refund.setCreateBy(SecurityUtils.getUsername());
        ctRefundMapper.insertCtRefund(refund);

        order.setStatus("5");
        order.setUpdateBy(SecurityUtils.getUsername());
        ctOrderMapper.updateCtOrder(order);
        return toMap(refund, order);
    }

    @Override
    public Map<String, Object> agree(Long sellerId, String refundNo)
    {
        CtRefund refund = requireSellerRefund(sellerId, refundNo);
        if (!"0".equals(refund.getStatus()))
        {
            throw new ApiException(409, "??????????" + REFUND_STATUS.get(refund.getStatus()) + "?????????");
        }
        refund.setStatus("1");
        refund.setHandleBy(SecurityUtils.getUsername());
        refund.setHandleTime(new Date());
        ctRefundMapper.updateCtRefund(refund);
        return toMap(refund, ctOrderMapper.selectCtOrderByOrderNo(refund.getOrderNo()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reject(Long sellerId, String refundNo, String remark)
    {
        CtRefund refund = requireSellerRefund(sellerId, refundNo);
        if (!"0".equals(refund.getStatus()))
        {
            throw new ApiException(409, "??????????" + REFUND_STATUS.get(refund.getStatus()) + "????????");
        }
        refund.setStatus("2");
        refund.setHandleBy(SecurityUtils.getUsername());
        refund.setHandleTime(new Date());
        refund.setHandleRemark(StringUtils.isEmpty(remark) ? "?????????" : remark);
        ctRefundMapper.updateCtRefund(refund);

        CtOrder order = ctOrderMapper.selectCtOrderByOrderNo(refund.getOrderNo());
        if (order != null && "5".equals(order.getStatus()))
        {
            // ???????????????????
            order.setStatus(refund.getOrderStatusBefore());
            order.setUpdateBy(SecurityUtils.getUsername());
            ctOrderMapper.updateCtOrder(order);
        }
        return toMap(refund, order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> complete(Long sellerId, String refundNo)
    {
        CtRefund refund = requireSellerRefund(sellerId, refundNo);
        if (!"1".equals(refund.getStatus()))
        {
            throw new ApiException(409, "??????????" + REFUND_STATUS.get(refund.getStatus()) + "????????????");
        }
        refund.setStatus("3");
        refund.setHandleBy(SecurityUtils.getUsername());
        refund.setHandleTime(new Date());
        ctRefundMapper.updateCtRefund(refund);

        CtOrder order = ctOrderMapper.selectCtOrderByOrderNo(refund.getOrderNo());
        if (order == null)
        {
            throw new ApiException(404, "??????????");
        }
        if (!"5".equals(order.getStatus()))
        {
            throw new ApiException(409, "???????????" + ORDER_STATUS.get(order.getStatus()) + "????????????");
        }
        order.setStatus("6");
        order.setUpdateBy(SecurityUtils.getUsername());
        ctOrderMapper.updateCtOrder(order);
        restoreStock(order);
        return toMap(refund, order);
    }

    @Override
    public List<Map<String, Object>> listMine(Long userId)
    {
        CtRefund query = new CtRefund();
        query.setUserId(userId);
        return toList(ctRefundMapper.selectCtRefundList(query));
    }

    @Override
    public List<Map<String, Object>> listIncoming(Long sellerId)
    {
        CtRefund query = new CtRefund();
        query.setSellerId(sellerId);
        return toList(ctRefundMapper.selectCtRefundList(query));
    }

    private boolean hasActiveRefund(String orderNo)
    {
        CtRefund query = new CtRefund();
        query.setOrderNo(orderNo);
        for (CtRefund r : ctRefundMapper.selectCtRefundList(query))
        {
            if ("0".equals(r.getStatus()) || "1".equals(r.getStatus()))
            {
                return true;
            }
        }
        return false;
    }

    private CtRefund requireSellerRefund(Long sellerId, String refundNo)
    {
        CtRefund query = new CtRefund();
        query.setRefundNo(refundNo);
        List<CtRefund> list = ctRefundMapper.selectCtRefundList(query);
        if (list.isEmpty() || !sellerId.equals(list.get(0).getSellerId()))
        {
            throw new ApiException(404, "?????????");
        }
        return list.get(0);
    }

    private void restoreStock(CtOrder order)
    {
        if (order.getOrderItems() == null)
        {
            return;
        }
        for (CtOrderItem line : order.getOrderItems())
        {
            // ??????????????????????????????
            productService.restoreStock(line.getProductId(), line.getQuantity() == null ? 1 : line.getQuantity());
        }
    }

    private List<Map<String, Object>> toList(List<CtRefund> refunds)
    {
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (CtRefund refund : refunds)
        {
            list.add(toMap(refund, ctOrderMapper.selectCtOrderByOrderNo(refund.getOrderNo())));
        }
        return list;
    }

    private Map<String, Object> toMap(CtRefund refund, CtOrder order)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("refundNo", refund.getRefundNo());
        m.put("orderNo", refund.getOrderNo());
        m.put("refundAmount", refund.getRefundAmount());
        m.put("reason", refund.getReason());
        m.put("status", refund.getStatus());
        m.put("statusText", REFUND_STATUS.get(refund.getStatus()));
        m.put("buyerId", String.valueOf(refund.getUserId()));
        m.put("sellerId", String.valueOf(refund.getSellerId()));
        m.put("handleBy", refund.getHandleBy());
        m.put("handleTime", refund.getHandleTime());
        m.put("handleRemark", refund.getHandleRemark());
        m.put("createTime", refund.getCreateTime());
        if (order != null)
        {
            m.put("orderStatus", order.getStatus());
            m.put("orderStatusText", ORDER_STATUS.get(order.getStatus()));
        }
        return m;
    }

    private String generateRefundNo()
    {
        return "RF" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + IdUtils.fastSimpleUUID().substring(0, 6).toUpperCase();
    }
}