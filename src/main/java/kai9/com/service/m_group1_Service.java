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

import kai9.com.dto.m_group1_Request;
import kai9.com.model.m_group1;
import kai9.com.repository.m_group1_Repository;

@Service
public class m_group1_Service {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private m_group1_Repository m_group1_rep;

    // グループマスタのインスタンスを返す
//    @Transactional(readOnly = true)
//    public m_group1 Find_m_group1(String login_id) throws UsernameNotFoundException {
//    	return m_group1_rep.findBylogin_id(login_id).orElseThrow();
//    }

    /**
     * グループー情報 全検索
     * 
     * @return 検索結果
     */
    @Transactional(readOnly = true)
    public List<m_group1> searchAll() {
        String sql = "SELECT * FROM m_group1_a order by group_id";
        RowMapper<m_group1> rowMapper = new BeanPropertyRowMapper<m_group1>(m_group1.class);
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * グループー情報 新規登録
     * 
     * @param group1 グループー情報
     */
//    @Transactional
    public m_group1 create(m_group1_Request m_group1_request) throws CloneNotSupportedException {
        m_group1 m_group1 = new m_group1();
        m_group1.setModify_count1(1);// 新規登録は1固定
        m_group1.setGroup_name(m_group1_request.getGroup_name());
        m_group1.setNote(m_group1_request.getNote());
        m_group1.setDelflg(m_group1_request.isDelflg());
        m_group1.setUpdate_date(new Date());
        // 認証グループ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int group_id = getUserIDByLoginID(name);
        m_group1.setUpdate_u_id(group_id);
        m_group1 = m_group1_rep.save(m_group1);

        // 履歴の登録:SQL実行
        String sql = "insert into m_group1_b select * from m_group1_a where group_id = ?";
        jdbcTemplate.update(sql, m_group1.getGroup_id());

        return m_group1;
    }

    /**
     * 更新
     */
    public m_group1 update(m_group1_Request m_group1_Request) {
        m_group1 m_group1 = findById(m_group1_Request.getGroup_id());

        // 変更対象が無い場合、更新しない
        boolean IsChange = false;
        if (m_group1.getGroup_id() != m_group1_Request.getGroup_id()) IsChange = true;
        if (!m_group1.getGroup_name().equals(m_group1_Request.getGroup_name())) IsChange = true;
        if (!m_group1.getNote().equals(m_group1_Request.getNote())) IsChange = true;
        if (m_group1.isDelflg() != m_group1_Request.isDelflg()) IsChange = true;
        if (!IsChange) return m_group1;

        // 更新処理
        m_group1.setGroup_name(m_group1_Request.getGroup_name());
        m_group1.setModify_count1(m_group1_Request.getModify_count1() + 1);// 更新回数+1
        m_group1.setNote(m_group1_Request.getNote());
        m_group1.setDelflg(m_group1_Request.isDelflg());
        m_group1.setUpdate_date(new Date());

        // 認証グループ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        m_group1.setUpdate_u_id(user_id);
        m_group1 = m_group1_rep.save(m_group1);

        // 履歴の登録:SQL実行
        String sql = "insert into m_group1_b select * from m_group1_a where group_id = ?";
        jdbcTemplate.update(sql, m_group1.getGroup_id());

        return m_group1;
    }

    /**
     * グループー情報 削除
     * 
     * @param group1 グループー情報
     */
    public m_group1 delete(m_group1_Request m_group1_Request) {
        m_group1 m_group1 = findById(m_group1_Request.getGroup_id());
        m_group1.setDelflg(!m_group1_Request.isDelflg());
        m_group1.setModify_count1(m_group1_Request.getModify_count1() + 1);// 更新回数+1

        // 認証グループ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        m_group1.setUpdate_u_id(user_id);
        m_group1 = m_group1_rep.save(m_group1);

        // 履歴の登録:SQL実行
        String sql = "insert into m_group1_b select * from m_group1_a where group_id = ?";
        jdbcTemplate.update(sql, m_group1.getGroup_id());

        return m_group1;
    }

    /**
     * グループー情報 主キー検索
     * 
     * @return 検索結果
     */
    public m_group1 findById(int group_id) {
        String sql = "SELECT * FROM m_group1_a WHERE group_id = ?";
        RowMapper<m_group1> rowMapper = new BeanPropertyRowMapper<m_group1>(m_group1.class);
        m_group1 group1 = jdbcTemplate.queryForObject(sql, rowMapper, group_id);
        return group1;
    }

    /**
     * ログインIDからユーザIDを取得
     * 
     * @return グループーID
     */
    public int getUserIDByLoginID(String login_id) {
        String sql = "SELECT * FROM m_user_a WHERE login_id = ?";
        Map<String, Object> map = jdbcTemplate.queryForMap(sql, login_id);
        int user_id = (int) map.get("user_id");
        return user_id;
    }

    /**
     * グループー情報 物理削除
     * 
     * @param id グループーID
     */
    public void delete(int group_id) {
        m_group1 group1 = findById(group_id);
        m_group1_rep.delete(group1);
    }

}