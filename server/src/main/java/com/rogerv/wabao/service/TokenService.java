package com.rogerv.wabao.service;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenService {
  private static final SecureRandom RND = new SecureRandom();

  public static String randomToken(int bytes) {
    byte[] b = new byte[bytes];
    RND.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }
}
