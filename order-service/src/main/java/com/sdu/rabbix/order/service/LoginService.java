package com.sdu.rabbix.order.service;

public interface LoginService {

    String login(Long uid);

    boolean authenticate(String token);

    Long getUseridByToken(String token);
}
