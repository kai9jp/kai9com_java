package kai9.com.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import kai9.libs.JsonResponse;
import kai9.com.dto.m_user_Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kai9.com.model.m_user;
import kai9.com.service.m_user_Service;

/**
 * ユーザー情報 Controller
 */
@RestController
public class m_user_Controller {

    /**
     * ユーザーマスタ Service
     */
    @Autowired
    private m_user_Service m_user_service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedJdbcTemplate;

    /**
     * ユーザー新規登録
     * 
     * @param userRequest リクエストデータ
     * @param model Model
     * @return ユーザー情報一覧画面
     * @throws CloneNotSupportedException
     * @throws IOException
     * @throws JSONException
     */
    @PostMapping(value = { "/api/m_user_create", "/api/m_user_update", "/api/m_user_delete" }, produces = "application/json;charset=utf-8")
    public void create(@RequestBody
    m_user_Request m_user, HttpServletResponse res, HttpServletRequest request) throws CloneNotSupportedException, IOException, JSONException {

        try {
            String URL = request.getRequestURI();
            String sql = "";
            m_user m_user_result = null;
            if (URL.toLowerCase().contains("m_user_create")) {
                // ログインIDの重複チェック
                sql = "SELECT COUNT(*) FROM m_user_a WHERE login_id = ?";
                int count = jdbcTemplate.queryForObject(sql, Integer.class, new Object[] { m_user.getLogin_id() });
                if (count != 0) {
                    // JSON形式でレスポンスを返す
                    JsonResponse json = new JsonResponse();
                    json.setReturn_code(HttpStatus.CONFLICT.value());
                    json.setMsg("ログインID　【" + m_user.getLogin_id() + "】　は既に登録されています");
                    json.SetJsonResponse(res);
                    return;
                }

                // ユーザー情報の登録
                m_user_result = m_user_service.create(m_user);
            } else {
                // 排他制御
                // 更新回数が異なる場合エラー
                sql = "SELECT modify_count FROM m_user_a WHERE user_id = ?";
                String modify_count = jdbcTemplate.queryForObject(sql, String.class, m_user.getUser_id());
                if (!modify_count.equals(String.valueOf(m_user.getModify_count()))) {
                    // JSON形式でレスポンスを返す
                    JsonResponse json = new JsonResponse();
                    json.setReturn_code(HttpStatus.CONFLICT.value());
                    json.setMsg("ログインID　【" + m_user.getLogin_id() + "】　で排他エラー発生。ページリロード後に再登録して下さい。");
                    json.SetJsonResponse(res);
                    return;
                }

                if (URL.toLowerCase().contains("m_user_update")) {
                    // ユーザー情報の更新
                    m_user_result = m_user_service.update(m_user);
                }

                if (URL.toLowerCase().contains("m_user_delete")) {
                    // 削除フラグをON/OFF反転する
                    m_user_result = m_user_service.delete(m_user);
                }
            }

            JsonResponse json = new JsonResponse();
            json.setReturn_code(HttpStatus.OK.value());
            json.setMsg("正常に登録しました");
            json.Add("user_id", String.valueOf(m_user_result.getUser_id()));
            json.Add("modify_count", String.valueOf(m_user_result.getModify_count()));
            json.Add("sei_kana", m_user_result.getSei_kana());// 自動変換結果を戻す
            json.Add("mei_kana", m_user_result.getMei_kana());// 自動変換結果を戻す
            json.SetJsonResponse(res);
            return;

        } catch (Exception e) {
            // JSON形式でレスポンスを返す
            JsonResponse json = new JsonResponse();
            json.setReturn_code(HttpStatus.INTERNAL_SERVER_ERROR.value());// 500 Internal Server Error 何らかのサーバ内で起きたエラー
            json.setMsg(e.getMessage());
            json.SetJsonResponse(res);
            return;
        }
    }

    /**
     * ユーザーの各件数を返す
     * 
     * @return ユーザー各件数
     */
    @PostMapping(value = "/api/m_user_count", produces = "application/json;charset=utf-8")
    public String m_user_count(String findstr, boolean isDelDataShow, HttpServletResponse res) throws CloneNotSupportedException, IOException {
        String Delflg = "";
        if (!isDelDataShow) Delflg = "and delflg = false ";

        String where = "";
        String all_count = "0";
        String admin_count = "0";
        String normal_count = "0";
        String readonly_count = "0";
        if (findstr != "") {
            findstr = findstr.replace("　", " ");
            String[] strs = findstr.split(" ");
            Set<String> keys = new HashSet<>();
            for (String i : strs) {
                keys.add("%" + i + "%");
            }
            where = " and("
                    + " login_id like any(array[ :keys ])"
                    + " or"
                    + " sei like any(array[ :keys ])"
                    + " or"
                    + " mei like any(array[ :keys ])"
                    + " or"
                    + " sei_kana like any(array[ :keys ])"
                    + " or"
                    + " mei_kana like any(array[ :keys ])"
                    + " or"
                    + " mail like any(array[ :keys ])"
                    + " or"
                    + " ip like any(array[ :keys ])"
                    + " or"
                    + " note like any(array[ :keys ])"
                    + ")";
            MapSqlParameterSource Param = new MapSqlParameterSource();
            Param.addValue("keys", keys);

            String sql = "select count(*) from m_user_a where 0 = 0 " + Delflg + where;
            all_count = namedJdbcTemplate.queryForObject(sql, Param, String.class);

            sql = "select count(*) from m_user_a where authority_lv = 3 " + Delflg + where;
            admin_count = namedJdbcTemplate.queryForObject(sql, Param, String.class);

            sql = "select count(*) from m_user_a where authority_lv = 1 " + Delflg + where;
            normal_count = namedJdbcTemplate.queryForObject(sql, Param, String.class);

            sql = "select count(*) from m_user_a where authority_lv = 2 " + Delflg + where;
            readonly_count = namedJdbcTemplate.queryForObject(sql, Param, String.class);
        } else {
            String sql = "select count(*) from m_user_a where 0 = 0 " + Delflg;
            all_count = jdbcTemplate.queryForObject(sql, String.class);

            sql = "select count(*) from m_user_a where authority_lv = 3 " + Delflg;
            admin_count = jdbcTemplate.queryForObject(sql, String.class);

            sql = "select count(*) from m_user_a where authority_lv = 1 " + Delflg;
            normal_count = jdbcTemplate.queryForObject(sql, String.class);

            sql = "select count(*) from m_user_a where authority_lv = 2 " + Delflg;
            readonly_count = jdbcTemplate.queryForObject(sql, String.class);
        }
        // レスポンスヘッダーのセット(JSON形式を指定)
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        // レスポンス生成
        String str = "{\"return_code\": " + HttpStatus.OK.value() + ","
                + " \"all_count\": " + all_count + ","
                + " \"admin_count\": " + admin_count + ","
                + " \"normal_count\": " + normal_count + ","
                + " \"readonly_count\": " + readonly_count
                + "}";
        return str;
    }

    /**
     * ユーザーデータをページネーションで取得する
     * 
     * @return ユーザーデータリスト
     */
    @PostMapping(value = "/api/m_user_find", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String m_user_find(Integer limit, Integer offset, String findstr, boolean isDelDataShow) throws CloneNotSupportedException, IOException {

        String Delflg = "";

        List<m_user> m_user_list = null;
        if (findstr.equals("")) {
            if (!isDelDataShow) {
                Delflg = "where delflg = false";
            }
            String sql = "select * from m_user_a " + Delflg + " order by user_id asc limit ? offset ?";
            RowMapper<m_user> rowMapper = new BeanPropertyRowMapper<m_user>(m_user.class);
            m_user_list = jdbcTemplate.query(sql, rowMapper, limit, offset);
        } else {
            // あいまい検索準備
            findstr = findstr.replace("　", " ");
            String[] strs = findstr.split(" ");
            Set<String> keys = new HashSet<>();
            for (String i : strs) {
                keys.add("%" + i + "%");
            }
            if (!isDelDataShow) Delflg = " and delflg = false ";

            // https://loglog.xyz/programming/jdbctemplate_in
            // https://qiita.com/naohide_a/items/3c78837ac7a1e05c6134
            String sql = "select * from m_user_a where 0 = 0" + Delflg + " and ("
                    + " login_id like any(array[ :keys ])"
                    + " or"
                    + " sei like any(array[ :keys ])"
                    + " or"
                    + " mei like any(array[ :keys ])"
                    + " or"
                    + " sei_kana like any(array[ :keys ])"
                    + " or"
                    + " mei_kana like any(array[ :keys ])"
                    + " or"
                    + " mail like any(array[ :keys ])"
                    + " or"
                    + " ip like any(array[ :keys ])"
                    + " or"
                    + " note like any(array[ :keys ])"
                    + ")"
                    + " order by user_id asc limit :limit offset :offset ";
            MapSqlParameterSource Param = new MapSqlParameterSource();
            Param.addValue("limit", limit);
            Param.addValue("offset", offset);
            Param.addValue("keys", keys);

            RowMapper<m_user> rowMapper = new BeanPropertyRowMapper<m_user>(m_user.class);
            // https://loglog.xyz/programming/jdbctemplate_in
            m_user_list = namedJdbcTemplate.query(sql, Param, rowMapper);
        }

        // データが取得できなかった場合は、null値を返す
        if (m_user_list == null || m_user_list.size() == 0) {
            return null;
        }
        // 取得データをJSON文字列に変換し返却
        return getJsonData(m_user_list);
    }

    /**
     * ユーザー履歴を取得する
     */
    @PostMapping(value = "/api/m_user_history_find", produces = "application/json;charset=utf-8")
    public String m_user_history_find(Number user_id, HttpServletResponse res) throws CloneNotSupportedException, IOException {
        String sql = "SELECT * FROM m_user_b where user_id = :user_id order by modify_count desc";
        RowMapper<m_user> rowMapper = new BeanPropertyRowMapper<m_user>(m_user.class);
        // https://loglog.xyz/programming/jdbctemplate_in
        MapSqlParameterSource Param = new MapSqlParameterSource()
                .addValue("user_id", user_id);
        List<m_user> m_user_list = namedJdbcTemplate.query(sql, Param, rowMapper);

        // ユーザーデータが取得できなかった場合は、null値を返す
        if (m_user_list == null || m_user_list.size() == 0) {
            return null;
        }
        // 取得したユーザーデータをJSON文字列に変換し返却
        return getJsonData(m_user_list);
    }

    /**
     * 引数のオブジェクトをJSON文字列に変換する
     * 
     * @param data オブジェクトのデータ
     * @return 変換後JSON文字列
     */
    private String getJsonData(Object data) {
        String retVal = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            retVal = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            System.err.println(e);
        }
        return retVal;
    }

}
