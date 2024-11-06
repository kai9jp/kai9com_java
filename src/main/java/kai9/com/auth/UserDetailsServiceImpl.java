package kai9.com.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy // 追加
    PasswordEncoder passwordEncoder;

    // DB認証を行うのでloadUserByUsernameをオーバーライドする
    // ユーザマスタのインスタンスを返す
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            String sql = "SELECT * FROM m_user_a WHERE login_id = ?";
            Map<String, Object> map = jdbcTemplate.queryForMap(sql, username);
            String password = (String) map.get("password");
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            // ロールをGrantedAuthority用に変換し格納
            switch ((int) map.get("authority_lv")) {
            case 1:
                // 1:一般
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                break;
            case 2:
                // 2:参照専用
                authorities.add(new SimpleGrantedAuthority("ROLE_READ_ONLY"));
                break;
            case 3:
                // 3:管理者
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            return new UserDetailsImpl(username, password, authorities);
        } catch (Exception e) {
            throw new UsernameNotFoundException("user not found.", e);
        }
    }

}