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

import kai9.com.dto.m_jobinfo_Request;
import kai9.com.model.m_jobinfo;
import kai9.com.repository.m_jobinfo_Repository;

@Service
public class m_jobinfo_Service {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private m_jobinfo_Repository m_jobinfo_rep;

    /**
     * ジョブ情報情報 全検索
     * 
     * @return 検索結果
     */
    @Transactional(readOnly = true)
    public List<m_jobinfo> searchAll() {
        String sql = "SELECT * FROM m_jobinfo_a order by jobinfo_id";
        RowMapper<m_jobinfo> rowMapper = new BeanPropertyRowMapper<m_jobinfo>(m_jobinfo.class);
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * ジョブ情報情報 新規登録
     */
//    @Transactional
    public m_jobinfo create(m_jobinfo_Request m_jobinfo_request) throws CloneNotSupportedException {
        m_jobinfo jobinfo = new m_jobinfo();
        jobinfo.setModify_count(1);// 新規登録は1固定
        jobinfo.setJobinfo_id(m_jobinfo_request.getJobinfo_id());
        jobinfo.setJob_name(m_jobinfo_request.getJob_name());
        jobinfo.setUnit_full_name(m_jobinfo_request.getUnit_full_name());
        jobinfo.setLast_exec_datetime(m_jobinfo_request.getLast_exec_datetime());
        jobinfo.setNext_exec_datetime(m_jobinfo_request.getNext_exec_datetime());
        jobinfo.setNote(m_jobinfo_request.getNote());
        jobinfo.setDelflg(m_jobinfo_request.isDelflg());
        jobinfo.setUpdate_date(new Date());
        // 認証サーバ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        jobinfo.setUpdate_u_id(user_id);
        jobinfo = m_jobinfo_rep.save(jobinfo);

        // 履歴の登録:SQL実行
        String sql = "insert into m_jobinfo_b select * from m_jobinfo_a where jobinfo_id = ?";
        jdbcTemplate.update(sql, jobinfo.getJobinfo_id());

        return jobinfo;
    }

    /**
     * ジョブ情報情報 更新
     */
    public m_jobinfo update(m_jobinfo_Request jobinfoUpdateRequest) {
        m_jobinfo jobinfo = findById(jobinfoUpdateRequest.getJobinfo_id());
        jobinfo.setJobinfo_id(jobinfoUpdateRequest.getJobinfo_id());
        jobinfo.setModify_count(jobinfoUpdateRequest.getModify_count() + 1);// 更新回数+1
        jobinfo.setJob_name(jobinfoUpdateRequest.getJob_name());
        jobinfo.setUnit_full_name(jobinfoUpdateRequest.getUnit_full_name());
        jobinfo.setLast_exec_datetime(jobinfoUpdateRequest.getLast_exec_datetime());
        jobinfo.setNext_exec_datetime(jobinfoUpdateRequest.getNext_exec_datetime());
        jobinfo.setNote(jobinfoUpdateRequest.getNote());
        jobinfo.setDelflg(jobinfoUpdateRequest.isDelflg());
        jobinfo.setUpdate_date(new Date());

        // 認証サーバ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        jobinfo.setUpdate_u_id(user_id);
        jobinfo = m_jobinfo_rep.save(jobinfo);

        // 履歴の登録:SQL実行
        String sql = "insert into m_jobinfo_b select * from m_jobinfo_a where jobinfo_id = ?";
        jdbcTemplate.update(sql, jobinfo.getJobinfo_id());

        return jobinfo;
    }

    /**
     * ジョブ情報情報 削除
     */
    public m_jobinfo delete(m_jobinfo_Request jobinfoUpdateRequest) {
        m_jobinfo jobinfo = findById(jobinfoUpdateRequest.getJobinfo_id());
        jobinfo.setDelflg(!jobinfoUpdateRequest.isDelflg());
        jobinfo.setModify_count(jobinfoUpdateRequest.getModify_count() + 1);// 更新回数+1

        // 認証サーバ取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        int user_id = getUserIDByLoginID(name);
        jobinfo.setUpdate_u_id(user_id);
        jobinfo = m_jobinfo_rep.save(jobinfo);

        // 履歴の登録:SQL実行
        String sql = "insert into m_jobinfo_b select * from m_jobinfo_a where jobinfo_id = ?";
        jdbcTemplate.update(sql, jobinfo.getJobinfo_id());

        return jobinfo;
    }

    /**
     * ジョブ情報情報 主キー検索
     */
    public m_jobinfo findById(int jobinfo_id) {
        return m_jobinfo_rep.findById(jobinfo_id).get();
    }

    /**
     * ジョブ情報情報 物理削除
     */
    public void delete(int jobinfo_id) {
        m_jobinfo jobinfo = findById(jobinfo_id);
        m_jobinfo_rep.delete(jobinfo);
    }

    public int getUserIDByLoginID(String login_id) {
        String sql = "SELECT * FROM m_user_a WHERE login_id = ?";
        Map<String, Object> map = jdbcTemplate.queryForMap(sql, login_id);
        int user_id = (int) map.get("user_id");
        return user_id;
    }

}