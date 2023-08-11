package com.sdu.rabbitmq.order.entity.dto;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class RegisterReq {

    //@NotEmpty(message = "用户名不能为空")
    private String username;

    //@NotEmpty(message = "密码不能为空")
    private String password;
}
