package com.domerry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.domerry.service.FriendService;
import com.domerry.entity.SysFriend;
import com.domerry.enums.FriendStatusEnum;
import com.domerry.exception.ServiceException;
import com.domerry.mapper.SysFriendMapper;
import com.domerry.utils.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final SysFriendMapper friendMapper;

    private final BusinessEmailService emailService;
    @Autowired
    private EmailUtil emailUtil;

    /** 友链申请通知收件邮箱（占位默认值，请通过环境变量 FRIEND_NOTIFY_EMAIL 配置） */
    @Value("${FRIEND_NOTIFY_EMAIL:friend-notify@your-domain.example}")
    private String friendNotifyEmail;

    @Override
    public List<SysFriend> getFriendList() {
        return friendMapper.selectList(new LambdaQueryWrapper<SysFriend>()
                .select(SysFriend::getId,SysFriend::getName,SysFriend::getInfo,SysFriend::getAvatar
                        ,SysFriend::getUrl)
                .eq(SysFriend::getStatus, FriendStatusEnum.UP.getCode())
                .orderByAsc(SysFriend::getSort));
    }

    @Override
    public Boolean apply(SysFriend sysFriend) {
        // 友链地址会原样入库并渲染成可点击链接，这里只接受 http/https，
        // 挡掉绕过前端校验直接 POST 进来的 javascript: 等伪协议
        if (!isHttpUrl(sysFriend.getUrl())) {
            throw new ServiceException("申请友链失败，网站链接必须以 http:// 或 https:// 开头");
        }

        SysFriend obj = friendMapper.selectOne(new LambdaQueryWrapper<SysFriend>()
                .eq(SysFriend::getUrl, sysFriend.getUrl()));
        if (ObjectUtils.isNotEmpty(obj)) {
            throw new ServiceException("申请友链失败，该网站已存在");
        }

        sysFriend.setStatus(FriendStatusEnum.APPLY.getCode());
        friendMapper.insert(sysFriend);
        // 发送邮箱提示审核
        try {
            emailUtil.send(friendNotifyEmail, "友联申请","有新的友链申请，请及时处理，网站："+sysFriend.getUrl());
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 只接受 http/https 协议，其余（含 javascript: 等伪协议）一律视为非法链接
     */
    private boolean isHttpUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        try {
            String protocol = new URL(url).getProtocol();
            return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
