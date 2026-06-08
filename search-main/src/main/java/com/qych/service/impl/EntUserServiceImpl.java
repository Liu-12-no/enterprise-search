package com.qych.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.qych.entity.dtos.UserUpdateDTO;
import com.qych.entity.pojos.EntUser;
import com.qych.entity.vo.LoginVO;
import com.qych.mapper.EntUserMapper;
import com.qych.service.IEntUserService;
import com.qych.utils.JwtUtils;
import com.qych.utils.UserContext;
import com.qych.utils.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;



@Slf4j
@Service
public class EntUserServiceImpl extends ServiceImpl<EntUserMapper, EntUser> implements IEntUserService {

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 注册/登录
     * @param username
     * @param password
     * @return
     */
    @Override
    public LoginVO loginOrRegister(String username, String password) {

        //密码md5加密
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        EntUser entUser = getOne(new LambdaQueryWrapper<EntUser>().eq(EntUser::getUsername, username));

        if(entUser==null){
            log.info("未查询到账号 [{}], 自动为您注册", username);
            //将该用户保存进数据库
           entUser = saveEntUser(username, md5Password);

        }else{
            log.info("该账号:{}已存在,正在校验密码",username);
            if(entUser.getStatus()!=null && entUser.getStatus()==0){
                throw new BaseException("该账号已被禁用!");
            }
            if(!entUser.getPassword().equals(md5Password)){
                throw new BaseException("密码错误，请重试");
            }
        }
        Long userId = entUser.getId();

        String token = jwtUtils.generateToken(userId, username);

        //昵称
        String trueName = entUser.getTrueName();

        LoginVO loginVO = new LoginVO();
        //昵称
        loginVO.setTrueName(trueName);

        loginVO.setToken(token);

        return loginVO;

    }

    /**
     * 更新昵称
     * @param dto
     * @return
     */
    @Override
    public LoginVO updateUserName(UserUpdateDTO dto) {

        LoginVO loginVO = new LoginVO();
        //获取前端传来的昵称
        String trueName = dto.getTrueName();

        //获取当前用户id
        Long userId = UserContext.getUserId();
        //根据当前用户id查询用户
        EntUser entUser = getById(userId);
        String username = entUser.getUsername();
        //获取token
        String token = jwtUtils.generateToken(userId, username);

        loginVO.setToken(token);
        if(entUser.getTrueName()!=null && entUser.getTrueName().equals(trueName)){
            log.error("新昵称与原昵称相同");
            loginVO.setTrueName(entUser.getTrueName());

            return loginVO;
        }
        loginVO.setTrueName(trueName);
        entUser.setTrueName(trueName);
        //更新数据库
        updateById(entUser);

        return loginVO;
    }

    private EntUser saveEntUser(String username, String md5Password) {
        EntUser user = new EntUser();
        user.setUsername(username);
        user.setPassword(md5Password);
        user.setStatus(1);
        //截取username的前4位
        user.setTrueName("用户_"+ username.substring(0,Math.min(username.length(),4)));
        save(user);
        return user;
    }
}
