package com.qych.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qych.entity.dtos.LoginDTO;
import com.qych.entity.pojos.EntUser;
import com.qych.entity.vo.LoginVO;
import com.qych.mapper.EntUserMapper;
import com.qych.service.IEntUserService;
import com.qych.utils.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class EntLoginController {

    @Autowired
    IEntUserService entUserService;
    

    @PostMapping("/login")
    public BaseResponse<LoginVO> login(@RequestBody LoginDTO dto){

        //获得token
        LoginVO loginVO = entUserService.loginOrRegister(dto.getUsername(), dto.getPassword());

        return BaseResponse.success(loginVO);
    }
}
