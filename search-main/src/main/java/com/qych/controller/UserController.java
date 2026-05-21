package com.qych.controller;


import com.qych.entity.dtos.UserUpdateDTO;
import com.qych.entity.vo.LoginVO;
import com.qych.service.IEntChatSessionService;
import com.qych.service.IEntUserService;
import com.qych.utils.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private IEntUserService entUserService;



    /**
     * 更新用户昵称
     * @param dto
     * @return
     */
    @PutMapping("/update")
    public BaseResponse updateUserInfo(@RequestBody UserUpdateDTO dto){

        LoginVO loginVO = entUserService.updateUserName(dto);

        return BaseResponse.success(loginVO);
    }



}
