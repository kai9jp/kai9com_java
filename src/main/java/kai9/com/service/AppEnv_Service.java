package kai9.com.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kai9.com.dto.AppEnv_Request;
import kai9.com.model.AppEnv;
import kai9.com.repository.AppEnv_Repository;

/**
 * 環境マスタ:サービス
 */
@Service
public class AppEnv_Service {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("commonjdbc")
    JdbcTemplate jdbcTemplate_com;

    @Autowired
    private AppEnv_Repository app_env_rep;

    /**
     * 全検索
     */
    @Transactional(readOnly = true)
    public List<AppEnv> searchAll() {
        String sql = "select * from app_env_a";
        RowMapper<AppEnv> rowMapper = new BeanPropertyRowMapper<AppEnv>(AppEnv.class);
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * 新規登録、更新
     */
    public AppEnv create(AppEnv_Request AppEnv_Request) throws CloneNotSupportedException {
        AppEnv m_env = new AppEnv();
        if (AppEnv_Request.getModify_count() == 0) {
            m_env.setModify_count(1);// 新規登録は1固定
        } else {
            boolean IsChange = false;
            m_env = findById();
            if (!m_env.getDir_tmp().equals(AppEnv_Request.getDir_tmp())) IsChange = true;
            if (!m_env.getDel_days_tmp().equals(AppEnv_Request.getDel_days_tmp())) IsChange = true;
            if (!m_env.getSvn_react_dir().equals(AppEnv_Request.getSvn_react_dir())) IsChange = true;
            if (!m_env.getSvn_react_url().equals(AppEnv_Request.getSvn_react_url())) IsChange = true;
            if (!m_env.getSvn_scenario_dir().equals(AppEnv_Request.getSvn_scenario_dir())) IsChange = true;
            if (!m_env.getSvn_testdata_dir().equals(AppEnv_Request.getSvn_testdata_dir())) IsChange = true;
            if (!m_env.getSvn_spring_dir().equals(AppEnv_Request.getSvn_spring_dir())) IsChange = true;
            if (!m_env.getSvn_spring_url().equals(AppEnv_Request.getSvn_spring_url())) IsChange = true;
            if (!m_env.getSvn_scenario_url().equals(AppEnv_Request.getSvn_scenario_url())) IsChange = true;
            if (!m_env.getSvn_testdata_url().equals(AppEnv_Request.getSvn_testdata_url())) IsChange = true;
            // カラムをDBに途中追加した場合、nullなのでm_env.getSvn_idで例外がスローされる。避けるためにObjectsを使って比較するサンプル(DBのデフォルト値を空欄で入れれば良いだけだがサンプルの記録として残す)
            if (!Objects.equals(m_env.getSvn_id(), AppEnv_Request.getSvn_id())) IsChange = true;
            if (!Objects.equals(m_env.getSvn_pw(), AppEnv_Request.getSvn_pw())) IsChange = true;
            // 変更が無い場合は何もしない
            if (!IsChange) return m_env;

            // 更新回数+1
            m_env.setModify_count(AppEnv_Request.getModify_count() + 1);
        }
        m_env.setDir_tmp(AppEnv_Request.getDir_tmp());
        m_env.setDel_days_tmp(AppEnv_Request.getDel_days_tmp());
        m_env.setSvn_react_dir(AppEnv_Request.getSvn_react_dir());
        m_env.setSvn_react_url(AppEnv_Request.getSvn_react_url());
        m_env.setSvn_scenario_dir(AppEnv_Request.getSvn_scenario_dir());
        m_env.setSvn_testdata_dir(AppEnv_Request.getSvn_testdata_dir());
        m_env.setSvn_spring_dir(AppEnv_Request.getSvn_spring_dir());
        m_env.setSvn_spring_url(AppEnv_Request.getSvn_spring_url());
        m_env.setSvn_scenario_url(AppEnv_Request.getSvn_scenario_url());
        m_env.setSvn_testdata_url(AppEnv_Request.getSvn_testdata_url());
        m_env.setSvn_id(AppEnv_Request.getSvn_id());

        // 変更時は、ＰＷ変更してないのに、ＰＷを再度暗号化してしまわないよう制御
        boolean isEncrypt = true;
        if (AppEnv_Request.getModify_count() != 0) {
            String sql_p = "SELECT svn_pw FROM app_env_a";
            String password = jdbcTemplate.queryForObject(sql_p, String.class);
            if (password != null && password.equals(AppEnv_Request.getSvn_pw())) {
                m_env.setSvn_pw(AppEnv_Request.getSvn_pw());
                isEncrypt = false;
            }
        }
        if (isEncrypt) {
            m_env.setSvn_pw(AppEnv_Request.getSvn_pw());
            // 暗号化
            String salt = KeyGenerators.string().generateKey();
            String secretKey = "kai9SecretKey"; // 実際のアプリケーションでは安全な方法で管理する事(make_source_code_Controllerと対)
            TextEncryptor encryptor = Encryptors.text(secretKey, salt);
            m_env.setSvn_pw(encryptor.encrypt(AppEnv_Request.getSvn_pw()));
            m_env.setSvn_pw_salt(salt);
        }

        m_env.setUpdate_date(new Date());
        // 更新ユーザ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        m_env.setUpdate_u_id(user_id);

        // delete & insert
        jdbcTemplate.update("DELETE FROM app_env_a");

        m_env = app_env_rep.save(m_env);

        // 履歴の登録:SQL実行
        String sql = "insert into app_env_b select * from app_env_a ";
        jdbcTemplate.update(sql);

        return m_env;
    }

    /**
     * 主キー検索
     */
    public AppEnv findById() {
        String sql = "select * from app_env_a";
        RowMapper<AppEnv> rowMapper = new BeanPropertyRowMapper<AppEnv>(AppEnv.class);
        AppEnv m_env = jdbcTemplate.queryForObject(sql, rowMapper);
        return m_env;
    }

    /**
     * ログインIDからユーザIDを取得
     */
    public int getUserIDByLoginID(String login_id) {
        String sql = "select * from m_user_a where login_id = ?";
        Map<String, Object> map = jdbcTemplate_com.queryForMap(sql, login_id);
        int user_id = (int) map.get("user_id");
        return user_id;
    }

}
