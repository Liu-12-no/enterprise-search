package com.qych.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.qych.entity.dtos.UserUpdateDTO;
import com.qych.entity.pojos.EntUser;
import com.qych.entity.vo.LoginVO;

public interface IEntUserService extends IService<EntUser> {
    // 登录注册
    public LoginVO loginOrRegister(String username, String password);

    //更新昵称
    public LoginVO updateUserName(UserUpdateDTO dto);
}