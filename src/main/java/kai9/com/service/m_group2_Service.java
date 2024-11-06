package kai9.com.service;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import kai9.com.dto.m_group2_Request;
import kai9.com.model.m_group2;
import kai9.com.repository.m_group2_Repository;

@Service
public class m_group2_Service {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private m_group2_Repository m_group2_rep;

    /**
     * グループー情報 新規登録
     * 
     * @param group1 グループー情報
     */
//    @Transactional
    public m_group2 create(m_group2_Request m_group2_request) throws CloneNotSupportedException {
        m_group2 group2 = new m_group2();
        group2.setGroup_id(m_group2_request.getGroup_id());
        group2.setModify_count1(m_group2_request.getModify_count1());
        group2.setUser_id(m_group2_request.getUser_id());
        group2.setModify_count2(1);// 新規登録は1固定
        group2.setDelflg(m_group2_request.isDelflg());
        group2.setUpdate_date(new Date());
        // 認証グループ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        group2.setUpdate_u_id(user_id);
        group2 = m_group2_rep.save(group2);

        // 履歴の登録:SQL実行
        String sql = "insert into m_group2_b select * from m_group2_a where group_id = ? and user_id = ?";
        jdbcTemplate.update(sql, group2.getGroup_id(), group2.getUser_id());

        return group2;
    }

    /**
     * グループー情報 更新
     * 
     * @param group1 グループー情報
     */
    public m_group2 update(m_group2_Request group1UpdateRequest) {
        m_group2 group2 = findById(group1UpdateRequest.getGroup_id(), group1UpdateRequest.getUser_id());

        // チェック状態が変わらない場合、更新しない
        if (group2.isDelflg() == group1UpdateRequest.isDelflg()) return group2;

        group2.setGroup_id(group1UpdateRequest.getGroup_id());
        group2.setModify_count1(group1UpdateRequest.getModify_count1());
        group2.setUser_id(group1UpdateRequest.getUser_id());
        group2.setModify_count2(group1UpdateRequest.getModify_count2() + 1);// 更新回数+1
        group2.setDelflg(group1UpdateRequest.isDelflg());
        group2.setUpdate_date(new Date());

        // 認証グループ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        group2.setUpdate_u_id(user_id);
        group2 = m_group2_rep.save(group2);

        // 履歴の登録:SQL実行
        String sql = "insert into m_group2_b select * from m_group2_a where group_id = ? and user_id = ?";
        jdbcTemplate.update(sql, group2.getGroup_id(), group2.getUser_id());

        return group2;
    }

    /**
     * グループー情報 主キー検索
     * 
     * @return 検索結果
     */
    public m_group2 findById(int group_id, int user_id) {
        String sql = "SELECT * FROM m_group2_a WHERE group_id = ? and user_id = ?";
        RowMapper<m_group2> rowMapper = new BeanPropertyRowMapper<m_group2>(m_group2.class);
        m_group2 group2 = jdbcTemplate.queryForObject(sql, rowMapper, group_id, user_id);
        return group2;
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