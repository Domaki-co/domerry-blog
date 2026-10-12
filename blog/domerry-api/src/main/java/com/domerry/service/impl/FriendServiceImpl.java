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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
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
}
