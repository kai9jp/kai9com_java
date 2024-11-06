package kai9.com.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kai9.com.dto.m_server_Request;
import kai9.com.model.m_server;
import kai9.com.repository.m_server_Repository;

@Service
public class m_server_Service {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private m_server_Repository m_server_rep;

    /**
     * サーバー情報 全検索
     * 
     * @return 検索結果
     */
    @Transactional(readOnly = true)
    public List<m_server> searchAll() {
        String sql = "SELECT * FROM m_server_a order by server_id";
        RowMapper<m_server> rowMapper = new BeanPropertyRowMapper<m_server>(m_server.class);
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * サーバー情報 新規登録
     */
//    @Transactional
    public m_server create(m_server_Request m_server_request) throws CloneNotSupportedException {
        m_server server = new m_server();
        server.setModify_count(1);// 新規登録は1固定
        server.setServer_id(m_server_request.getServer_id());
        server.setHost_name(m_server_request.getHost_name());
        server.setIp(m_server_request.getIp());
        server.setNote(m_server_request.getNote());
        server.setDelflg(m_server_request.isDelflg());
        server.setUpdate_date(new Date());
        // 認証サーバ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        server.setUpdate_u_id(user_id);
        server = m_server_rep.save(server);

        // 履歴の登録:SQL実行
        String sql = "insert into m_server_b select * from m_server_a where server_id = ?";
        jdbcTemplate.update(sql, server.getServer_id());

        return server;
    }

    /**
     * サーバー情報 更新
     */
    public m_server update(m_server_Request serverUpdateRequest) {
        m_server server = findById(serverUpdateRequest.getServer_id());
        server.setServer_id(serverUpdateRequest.getServer_id());
        server.setModify_count(serverUpdateRequest.getModify_count() + 1);// 更新回数+1
        server.setHost_name(serverUpdateRequest.getHost_name());
        server.setIp(serverUpdateRequest.getIp());
        server.setNote(serverUpdateRequest.getNote());
        server.setDelflg(serverUpdateRequest.isDelflg());
        server.setUpdate_date(new Date());

        // 認証サーバ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        server.setUpdate_u_id(user_id);
        server = m_server_rep.save(server);

        // 履歴の登録:SQL実行
        String sql = "insert into m_server_b select * from m_server_a where server_id = ?";
        jdbcTemplate.update(sql, server.getServer_id());

        return server;
    }

    /**
     * サーバー情報 削除
     */
    public m_server delete(m_server_Request serverUpdateRequest) {
        m_server server = findById(serverUpdateRequest.getServer_id());
        server.setDelflg(!serverUpdateRequest.isDelflg());
        server.setModify_count(serverUpdateRequest.getModify_count() + 1);// 更新回数+1

        // 認証サーバ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        server.setUpdate_u_id(user_id);
        server = m_server_rep.save(server);

        // 履歴の登録:SQL実行
        String sql = "insert into m_server_b select * from m_server_a where server_id = ?";
        jdbcTemplate.update(sql, server.getServer_id());

        return server;
    }

    /**
     * サーバー情報 主キー検索
     */
    public m_server findById(int server_id) {
        return m_server_rep.findById(server_id).get();
    }

    /**
     * サーバー情報 物理削除
     */
    public void delete(int server_id) {
        m_server server = findById(server_id);
        m_server_rep.delete(server);
    }

    public int getUserIDByLoginID(String login_id) {
        String sql = "SELECT * FROM m_user_a WHERE login_id = ?";
        Map<String, Object> map = jdbcTemplate.queryForMap(sql, login_id);
        int user_id = (int) map.get("user_id");
        return user_id;
    }

}