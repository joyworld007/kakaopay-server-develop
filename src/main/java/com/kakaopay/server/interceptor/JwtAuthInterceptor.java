package com.kakaopay.server.interceptor;

import com.kakaopay.server.domain.user.entity.User;
import com.kakaopay.server.repository.user.UserJpaRepository;
import com.kakaopay.server.service.user.UserService;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

  final private UserService userService;

  final private UserJpaRepository userJpaRepository;

  final String HEADER_TOKEN_KEY = "token";
  final String HEADER_USER_ID = "userID";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    if (!Optional.ofNullable(request.getHeader(HEADER_USER_ID)).isPresent()) {
      response.getWriter().write("Header userId Must be not null");
      response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
      return false;
    }
    String userId = request.getHeader(HEADER_USER_ID);

    if (!Optional.ofNullable(request.getHeader(HEADER_TOKEN_KEY)).isPresent()) {
      response.getWriter().write("Header token Must be not null");
      response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
      return false;
    }
    String givenToken = request.getHeader(HEADER_TOKEN_KEY);
    Optional<User> user = userJpaRepository.findById(userId);
    if (!user.isPresent()) {
      response.getWriter().write("Not Found User");
      response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    }
    String userToken = user.get().getToken();

    //사용자가 가지고 있는 토큰인지 검사
    if (!givenToken.equals(userToken)) {
      response.getWriter().write("Invalid Token");
      response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    }
    //토큰의 유효성을 검사
    if (!userService.verifyToken(givenToken, userId)) {
      response.getWriter().write("Invalid Token");
      response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    }
    return true;
  }
}