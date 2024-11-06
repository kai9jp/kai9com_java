package kai9.com.service;

import java.text.Normalizer;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kai9.com.dto.m_user_Request;
import kai9.com.model.m_user;
import kai9.com.repository.m_user_Repository;

@Service
public class m_user_Service {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private m_user_Repository m_user_rep;

    @Autowired
    @Lazy
    PasswordEncoder passwordEncoder;

    // ユーザマスタのインスタンスを返す
//    @Transactional(readOnly = true)
//    public m_user Find_m_user(String login_id) throws UsernameNotFoundException {
//    	return m_user_rep.findBylogin_id(login_id).orElseThrow();
//    }

    /**
     * ユーザー情報 全検索
     * 
     * @return 検索結果
     */
    @Transactional(readOnly = true)
    public List<m_user> searchAll() {
        // カラム名にアンダーバーが含まれるとsortに失敗するspringのバグがあるらしい
        // return m_user_rep.findAll(Sort.by(Sort.Direction.ASC, "user-id"));
        // return m_user_rep.findAll();

        // https://loglog.xyz/programming/java/jdbctemplate_query_select#%E8%A4%87%E6%95%B0%E4%BB%B6%E3%82%92%E5%8F%96%E5%BE%97-3
        String sql = "SELECT * FROM m_user_a order by user_id";
        RowMapper<m_user> rowMapper = new BeanPropertyRowMapper<m_user>(m_user.class);
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * ひらがな→カタカナの変換
     * https://hacknote.jp/archives/5291/
     */
    String ConvKana(String pStr) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < pStr.length(); i++) {
            char code = pStr.charAt(i);
            if ((code >= 0x3041) && (code <= 0x3093)) {
                buf.append((char) (code + 0x60));
            } else {
                buf.append(code);
            }
        }
        return buf.toString();
    }

    /**
     * ユーザー情報 新規登録
     * 
     * @param user ユーザー情報
     */
//    @Transactional
    public m_user create(m_user_Request m_user_request) throws CloneNotSupportedException {
        m_user m_user = new m_user();
        m_user.setModify_count(1);// 新規登録は1固定
        m_user.setLogin_id(m_user_request.getLogin_id());
        m_user.setSei(m_user_request.getSei());
        m_user.setMei(m_user_request.getMei());
        m_user.setSei_kana(m_user_request.getSei_kana());
        m_user.setSei_kana(Normalizer.normalize(m_user.getSei_kana(), Normalizer.Form.NFKC));// 半角を全角に変換
        m_user.setSei_kana(ConvKana(m_user.getSei_kana()));// ひらがな→カタカナの変換
        m_user.setMei_kana(m_user_request.getMei_kana());
        m_user.setMei_kana(Normalizer.normalize(m_user.getMei_kana(), Normalizer.Form.NFKC));// 半角を全角に変換
        m_user.setMei_kana(ConvKana(m_user.getMei_kana()));// ひらがな→カタカナの変換
        m_user.setPassword(passwordEncoder.encode(m_user_request.getPassword()));
        m_user.setNeed_password_change(m_user_request.getNeed_password_change());
        m_user.setMail(m_user_request.getMail());
        m_user.setIp(m_user_request.getIp());
        m_user.setDefault_g_id(m_user_request.getDefault_g_id());
        m_user.setAuthority_lv(m_user_request.getAuthority_lv());
        m_user.setNote(m_user_request.getNote());
        m_user.setDelflg(m_user_request.isDelflg());
        m_user.setUpdate_date(new Date());
        // 認証ユーザ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        m_user.setUpdate_u_id(user_id);
        m_user = m_user_rep.save(m_user);

        // 履歴の登録:SQL実行
        String sql = "insert into m_user_b select * from m_user_a where user_id = ?";
        jdbcTemplate.update(sql, m_user.getUser_id());

        return m_user;
    }

    /**
     * ユーザー情報 更新
     * 
     * @param user ユーザー情報
     */
    public m_user update(m_user_Request m_user_Request) {
        m_user m_user = findById(m_user_Request.getUser_id());
        // 変更対象が無い場合、更新しない
        boolean IsChange = false;
        if (m_user.getUser_id() != m_user_Request.getUser_id()) IsChange = true;
        if (!m_user.getLogin_id().equals(m_user_Request.getLogin_id())) IsChange = true;
        if (!m_user.getSei().equals(m_user_Request.getSei())) IsChange = true;
        if (!m_user.getMei().equals(m_user_Request.getMei())) IsChange = true;
        if (!m_user.getSei_kana().equals(m_user_Request.getSei_kana())) IsChange = true;
        if (!m_user.getMei_kana().equals(m_user_Request.getMei_kana())) IsChange = true;
        if (!m_user.getPassword().equals(m_user_Request.getPassword())) IsChange = true;
        if (m_user.isNeed_password_change() != m_user_Request.getNeed_password_change()) IsChange = true;
        if (!m_user.getMail().equals(m_user_Request.getMail())) IsChange = true;
        if (!m_user.getIp().equals(m_user_Request.getIp())) IsChange = true;
        if (m_user.getDefault_g_id() != m_user_Request.getDefault_g_id()) IsChange = true;
        if (m_user.getAuthority_lv() != m_user_Request.getAuthority_lv()) IsChange = true;
        if (!m_user.getNote().equals(m_user_Request.getNote())) IsChange = true;
        if (m_user.isDelflg() != m_user_Request.isDelflg()) IsChange = true;
        if (!IsChange) return m_user;

        m_user.setLogin_id(m_user_Request.getLogin_id());
        m_user.setModify_count(m_user_Request.getModify_count() + 1);// 更新回数+1
        m_user.setSei(m_user_Request.getSei());
        m_user.setMei(m_user_Request.getMei());
        m_user.setSei_kana(m_user_Request.getSei_kana());
        m_user.setSei_kana(Normalizer.normalize(m_user.getSei_kana(), Normalizer.Form.NFKC));// 半角を全角に変換
        m_user.setSei_kana(ConvKana(m_user.getSei_kana()));// ひらがな→カタカナの変換
        m_user.setMei_kana(m_user_Request.getMei_kana());
        m_user.setMei_kana(Normalizer.normalize(m_user.getMei_kana(), Normalizer.Form.NFKC));// 半角を全角に変換
        m_user.setMei_kana(ConvKana(m_user.getMei_kana()));// ひらがな→カタカナの変換
        m_user.setNeed_password_change(m_user_Request.getNeed_password_change());
        m_user.setMail(m_user_Request.getMail());
        m_user.setIp(m_user_Request.getIp());
        m_user.setDefault_g_id(m_user_Request.getDefault_g_id());
        m_user.setAuthority_lv(m_user_Request.getAuthority_lv());
        m_user.setNote(m_user_Request.getNote());
        m_user.setDelflg(m_user_Request.isDelflg());
        m_user.setUpdate_date(new Date());

        // ユーザ変更時は、ＰＷ変更してないのに、ＰＷを再度暗号化してしまわないよう制御
        String sql_p = "SELECT Password FROM m_user_a WHERE user_id = ?";
        String password = jdbcTemplate.queryForObject(sql_p, String.class, m_user.getUser_id());
        if (password != null && password.equals(m_user_Request.getPassword())) {
            m_user.setPassword(m_user_Request.getPassword());
        } else {
            m_user.setPassword(passwordEncoder.encode(m_user_Request.getPassword()));
        }

        // 認証ユーザ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        m_user.setUpdate_u_id(user_id);
        m_user = m_user_rep.save(m_user);

        // 履歴の登録:SQL実行
        String sql = "insert into m_user_b select * from m_user_a where user_id = ?";
        jdbcTemplate.update(sql, m_user.getUser_id());

        return m_user;
    }

    /**
     * ユーザー情報 削除
     * 
     * @param user ユーザー情報
     */
    public m_user delete(m_user_Request m_user_Request) {
        m_user m_user = findById(m_user_Request.getUser_id());
        m_user.setDelflg(!m_user_Request.isDelflg());
        m_user.setModify_count(m_user_Request.getModify_count() + 1);// 更新回数+1

        // 認証ユーザ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        m_user.setUpdate_u_id(user_id);
        m_user = m_user_rep.save(m_user);

        // 履歴の登録:SQL実行
        String sql = "insert into m_user_b select * from m_user_a where user_id = ?";
        jdbcTemplate.update(sql, m_user.getUser_id());

        return m_user;
    }

    /**
     * ユーザー情報 主キー検索
     * 
     * @return 検索結果
     */
    public m_user findById(int user_id) {
        String sql = "select * from m_user_a where user_id = ?";
        RowMapper<m_user> rowMapper = new BeanPropertyRowMapper<m_user>(m_user.class);
        m_user m_user = jdbcTemplate.queryForObject(sql, rowMapper, user_id);
        return m_user;
    }

    /**
     * ログインIDからユーザーIDを取得
     * 
     * @return ユーザーID
     */
    public int getUserIDByLoginID(String login_id) {
        String sql = "SELECT * FROM m_user_a WHERE login_id = ?";
        Map<String, Object> map = jdbcTemplate.queryForMap(sql, login_id);
        int user_id = (int) map.get("user_id");
        return user_id;
    }

    /**
     * ユーザー情報 物理削除
     * 
     * @param id ユーザーID
     */
    public void delete(int user_id) {
        m_user m_user = findById(user_id);
        m_user_rep.delete(m_user);
    }

}