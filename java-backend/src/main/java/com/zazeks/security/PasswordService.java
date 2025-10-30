package com.zazeks.security;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * Java-аналог функций {@code get_password_hash} и {@code verify_password}
 * из Python-модуля {@code backend/src/security.py}.
 */
@Component
public class PasswordService {

    public String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
