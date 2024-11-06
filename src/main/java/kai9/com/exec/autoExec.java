package kai9.com.exec;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import kai9.libs.Kai9Utils;

//アプリ起動時に動く(ApplicationRunner)
@Component
public class autoExec implements ApplicationRunner {

    @Autowired
    @Qualifier("commonjdbc")
    JdbcTemplate jdbcTemplate_com;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 初期ユーザが存在しない場合は作成する
        String sql = "INSERT INTO m_user_a (modify_count, login_id, sei, mei, sei_kana, mei_kana, password, mail, need_password_change, ip, default_g_id, authority_lv, note, update_u_id, update_date, delflg)" +
                " SELECT 1, 'z', 'z', '初期ユーザ', 'ゼット', 'ショキユーザ', '$2a$10$uMpb0Nv9JmTpOdB272FQxOmbKsJFYd6kJkxjyXB3RiuX3soYIIN92', 'z@kai9.com', FALSE, '', 0, 3, '', 0, ?, FALSE" +
                " WHERE NOT EXISTS (" +
                "   SELECT 1 FROM m_user_a WHERE login_id = 'z'" +
                " ) RETURNING user_id;";
        Integer newUserId = null;
        try {
            newUserId = jdbcTemplate_com.queryForObject(sql, new Object[] { Timestamp.valueOf(LocalDateTime.now()) }, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            // INSERT文が実行されず、キーが返されなかった場合、何もしない
        } catch (DataAccessException e) {
            // その他のデータベースアクセスエラー
            Kai9Utils.makeLog("error", "自動実行モード:初期ユーザ(z)の自動作成に失敗しました,", this.getClass());
        }
        if (newUserId != null) {
            // 自動生成した場合だけ、履歴テーブルへコピー
            String copySql = "INSERT INTO m_user_b SELECT * FROM m_user_a WHERE user_id = ?;";
            jdbcTemplate_com.update(copySql, newUserId);
        }

    }

}
