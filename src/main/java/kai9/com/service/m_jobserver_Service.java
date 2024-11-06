package kai9.com.service;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import kai9.com.dto.m_jobserver_Request;
import kai9.com.model.m_jobserver;
import kai9.com.repository.m_jobserver_Repository;

@Service
public class m_jobserver_Service {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private m_jobserver_Repository m_jobserver_rep;

    @Autowired
    NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    @Lazy // 追加
    PasswordEncoder passwordEncoder;

    /**
     * 新規登録
     */
//    @Transactional
    public m_jobserver create(m_jobserver_Request m_jobserver_request) throws CloneNotSupportedException {
        m_jobserver jobserver = new m_jobserver();
        jobserver.setJobinfo_id(m_jobserver_request.getJobinfo_id());
        jobserver.setServer_id(m_jobserver_request.getServer_id());
        jobserver.setModify_count(1);// 新規登録は1固定
        jobserver.setDelflg(m_jobserver_request.isDelflg());
        jobserver.setUpdate_date(new Date());
        // 認証グループ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        jobserver.setUpdate_u_id(user_id);
        jobserver = m_jobserver_rep.save(jobserver);

        // 履歴の登録:SQL実行
        String sql = "insert into m_jobserver_b select * from m_jobserver_a where jobinfo_id = ? and server_id = ?";
        jdbcTemplate.update(sql, jobserver.getJobinfo_id(), jobserver.getServer_id());

        return jobserver;
    }

    /**
     * 更新
     */
    public m_jobserver update(m_jobserver_Request group1UpdateRequest) {
        m_jobserver jobserver = findById(group1UpdateRequest.getJobinfo_id(), group1UpdateRequest.getServer_id());

        // チェック状態が変わらない場合、更新しない
        if (jobserver.isDelflg() == group1UpdateRequest.isDelflg()) return jobserver;

        jobserver.setJobinfo_id(group1UpdateRequest.getJobinfo_id());
        jobserver.setServer_id(group1UpdateRequest.getServer_id());
        jobserver.setModify_count(group1UpdateRequest.getModify_count() + 1);// 更新回数+1
        jobserver.setDelflg(group1UpdateRequest.isDelflg());
        jobserver.setUpdate_date(new Date());

        // 認証グループ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        jobserver.setUpdate_u_id(user_id);
        jobserver = m_jobserver_rep.save(jobserver);

        // 履歴の登録:SQL実行
        String sql = "insert into m_jobserver_b select * from m_jobserver_a where jobinfo_id = ? and server_id = ?";
        jdbcTemplate.update(sql, jobserver.getJobinfo_id(), jobserver.getServer_id());

        return jobserver;
    }

    /**
     * 主キー検索
     */
    public m_jobserver findById(int jobinfo_id, int server_id) {
        String sql = "SELECT * FROM m_jobserver_a WHERE jobinfo_id = ? and server_id = ?";
        RowMapper<m_jobserver> rowMapper = new BeanPropertyRowMapper<m_jobserver>(m_jobserver.class);
        m_jobserver jobserver = jdbcTemplate.queryForObject(sql, rowMapper, jobinfo_id, server_id);
        return jobserver;
    }

    /**
     * ログインIDからグループーIDを取得
     * 
     * @return グループーID
     */
    public int getUserIDByLoginID(String login_id) {
        String sql = "SELECT * FROM m_user_a WHERE login_id = ?";
        Map<String, Object> map = jdbcTemplate.queryForMap(sql, login_id);
        int user_id = (int) map.get("user_id");
        return user_id;
    }

}