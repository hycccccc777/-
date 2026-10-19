package com.ruoyi.campus.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.domain.CtFavorite;
import com.ruoyi.campus.mapper.CtFavoriteMapper;
import com.ruoyi.campus.service.ICtFavoriteService;
import com.ruoyi.common.utils.StringUtils;

/**
 * 商品收藏 服务层实现
 * 
 * @author campus
 */
@Service
public class CtFavoriteServiceImpl implements ICtFavoriteService
{
    @Autowired
    private CtFavoriteMapper ctFavoriteMapper;

    @Override
    public List<CtFavorite> selectCtFavoriteList(CtFavorite ctFavorite)
    {
        return ctFavoriteMapper.selectCtFavoriteList(ctFavorite);
    }

    @Override
    public CtFavorite selectCtFavoriteById(Long favoriteId)
    {
        return ctFavoriteMapper.selectCtFavoriteById(favoriteId);
    }

    @Override
    public boolean isFavorite(CtFavorite ctFavorite)
    {
        return StringUtils.isNotNull(ctFavoriteMapper.selectCtFavoriteByUserAndProduct(ctFavorite));
    }

    @Override
    public int insertCtFavorite(CtFavorite ctFavorite)
    {
        if (isFavorite(ctFavorite))
        {
            return 1;
        }
        return ctFavoriteMapper.insertCtFavorite(ctFavorite);
    }

    @Override
    public int deleteCtFavoriteByIds(Long[] favoriteIds)
    {
        return ctFavoriteMapper.deleteCtFavoriteByIds(favoriteIds);
    }

    @Override
    public int deleteCtFavoriteByUserAndProduct(CtFavorite ctFavorite)
    {
        return ctFavoriteMapper.deleteCtFavoriteByUserAndProduct(ctFavorite);
    }
}
