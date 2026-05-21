package com.qych.utils;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    //密钥
    @Value("${qysearch.jwt.secret-key:defaultSecretKey}")
    private String secretKey;

    //过期时间，默认1小时
    @Value("${qysearch.jwt.expiration-time:3600000}")
    private long expirationTime;

    /**
     * 生成Token
     * @param userId
     * @param userName
     * @return
     */
    public String generateToken(Long userId,String userName){
        Map<String, Object> claims = new HashMap<>();

        claims.put("userId",userId);
        claims.put("userName",userName);

        return Jwts.builder()
                //核心数据：用户信息
                .setClaims(claims)
                //生成时间
                .setIssuedAt(new Date())
                //到期时间
                .setExpiration(new Date(System.currentTimeMillis()+expirationTime))
                //使用 HS256 算法和“私有密钥”进行签名
                .signWith(SignatureAlgorithm.HS256,secretKey)
                //完成封装
                .compact();
    }


    /**
     * 解析Token
     * @param token
     * @return
     */
    public Long getUserIdFromToken(String token){
        //启动解析器
        Claims claims = Jwts.parser()
                //根据secretKey 签名解析
                .setSigningKey(secretKey)
                //开始验证并解析
                .parseClaimsJws(token)
                //获取Claims对象
                .getBody();
        String userId = claims.get("userId").toString();

        return Long.parseLong(userId);
    }


}
