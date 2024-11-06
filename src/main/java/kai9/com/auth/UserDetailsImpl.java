package kai9.com.auth;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@SuppressWarnings("serial")
public class UserDetailsImpl implements UserDetails {

    /**
     * 
     */
    private String username;
    private String password;
    private Collection<GrantedAuthority> authorities;

    public UserDetailsImpl(String username, String password, Collection<GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // TODO 自動生成されたメソッド・スタブ
        return authorities;
    }

    @Override
    public String getPassword() {
        // TODO 自動生成されたメソッド・スタブ
        return password;
    }

    @Override
    public String getUsername() {
        // TODO 自動生成されたメソッド・スタブ
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        // TODO 自動生成されたメソッド・スタブ
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // TODO 自動生成されたメソッド・スタブ
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // TODO 自動生成されたメソッド・スタブ
        return true;
    }

    @Override
    public boolean isEnabled() {
        // TODO 自動生成されたメソッド・スタブ
        return true;
    }

}
