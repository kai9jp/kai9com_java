package kai9.com.controller;

import java.io.IOException;
import java.util.List;

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
import kai9.com.dto.m_server_Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kai9.com.model.m_server;
import kai9.com.service.m_server_Service;

/**
 * サーバー情報 Controller
 */
@RestController
public class m_server_Controller {

    /**
     * サーバーマスタ Service
     */
    @Autowired
    private m_server_Service m_server_service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedJdbcTemplate;

    /**
     * サーバー新規登録
     */
    @PostMapping(value = { "/api/m_server_create", "/api/m_server_update", "/api/m_server_delete" }, produces = "application/json;charset=utf-8")
    public void create(@RequestBody
    m_server_Request m_server_request, HttpServletResponse res, HttpServletRequest request) throws CloneNotSupportedException, IOException, JSONException {

        try {
            String URL = request.getRequestURI();
            String sql = "";
            m_server m_server_result = null;
            if (URL.toLowerCase().contains("m_server_create")) {
                // IPログインIDの重複チェック
                sql = "SELECT COUNT(*) FROM m_server_a WHERE ip = ?";
                int count1 = jdbcTemplate.queryForObject(sql, Integer.class, new Object[] { m_server_request.getIp() });
                if (count1 != 0) {
                    // JSON形式でレスポンスを返す
                    JsonResponse json = new JsonResponse();
                    json.setReturn_code(HttpStatus.CONFLICT.value());
                    json.setMsg("IP　【" + m_server_request.getIp() + "】　は既に登録されています");
                    json.SetJsonResponse(res);
                    return;
                }

                // ホスト名の重複チェック
                sql = "SELECT COUNT(*) FROM m_server_a WHERE host_name = ?";
                int count2 = jdbcTemplate.queryForObject(sql, Integer.class, new Object[] { m_server_request.getHost_name() });
                if (count2 != 0) {
                    // JSON形式でレスポンスを返す
                    JsonResponse json = new JsonResponse();
                    json.setReturn_code(HttpStatus.CONFLICT.value());
                    json.setMsg("ホスト名　【" + m_server_request.getHost_name() + "】　は既に登録されています");
                    json.SetJsonResponse(res);
                    return;
                }

                // サーバー情報の登録
                m_server_result = m_server_service.create(m_server_request);
            } else {
                // 排他制御
                // 更新回数が異なる場合エラー
                sql = "SELECT modify_count FROM m_server_a WHERE server_id = ?";
                String modify_count = jdbcTemplate.queryForObject(sql, String.class, m_server_request.getServer_id());
                if (!modify_count.equals(String.valueOf(m_server_request.getModify_count()))) {
                    // JSON形式でレスポンスを返す
                    JsonResponse json = new JsonResponse();
                    json.setReturn_code(HttpStatus.CONFLICT.value());
                    json.setMsg("ホスト名　【" + m_server_request.getHost_name() + "】　で排他エラー発生。ページリロード後に再登録して下さい。");
                    json.SetJsonResponse(res);
                    return;
                }

                if (URL.toLowerCase().contains("m_server_update")) {
                    // サーバー情報の更新
                    m_server_result = m_server_service.update(m_server_request);
                }

                if (URL.toLowerCase().contains("m_server_delete")) {
                    // 削除フラグをON/OFF反転する
                    m_server_result = m_server_service.delete(m_server_request);
                }
            }

            JsonResponse json = new JsonResponse();
            json.setReturn_code(HttpStatus.OK.value());
            json.setMsg("正常に登録しました");
            json.Add("server_id", String.valueOf(m_server_result.getServer_id()));
            json.Add("modify_count", String.valueOf(m_server_result.getModify_count()));
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
     * サーバーの各件数を返す
     * 
     * @return サーバー各件数
     */
    @PostMapping(value = "/api/m_server_count", produces = "application/json;charset=utf-8")
    public String m_server_count(String findstr, boolean isDelDataShow, HttpServletResponse res) throws CloneNotSupportedException, IOException {
        String where = "";
        if (findstr != "") {
            findstr = findstr.replace("　", " ");
            String[] strs = findstr.split(" ");
            String str = "";
            for (int i = 0; i < strs.length; i++) {
                if (str != "") {
                    str = str + ",";
                }
                str = str + "'%" + strs[i] + "%'";
            }
            where = " AND ( host_name ~~* any(array[" + str + "]) OR "
                    + "ip ~~* any(array[" + str + "]) OR "
                    + "note ~~* any(array[" + str + "])  )";
        }

        String Delflg = "";
        if (!isDelDataShow) {
            Delflg = "AND Delflg = false ";
        }

        String sql = "SELECT COUNT(*) FROM m_server_a Where 0 = 0 " + Delflg + where;
        String all_count = jdbcTemplate.queryForObject(sql, String.class);

        // JSON形式でレスポンスを返す
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        String str = "{\"return_code\": " + HttpStatus.OK.value() + ","
                + " \"count\": " + all_count
                + "}";
        return str;
    }

    /**
     * サーバーデータをページネーションで取得する
     * 
     * @return サーバーデータリスト
     */
    @PostMapping(value = "/api/m_server_find", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String m_server_find(Integer limit, Integer offset, String findstr, boolean isDelDataShow) throws CloneNotSupportedException, IOException {

        String Delflg = "";

        List<m_server> m_server_list = null;
        if (findstr == "") {
            if (!isDelDataShow) {
                Delflg = "Where Delflg = false";
            }
            String sql = "SELECT * FROM m_server_a " + Delflg + " order by server_id asc limit ? offset ?";
            RowMapper<m_server> rowMapper = new BeanPropertyRowMapper<m_server>(m_server.class);
            m_server_list = jdbcTemplate.query(sql, rowMapper, limit, offset);
        } else {
            findstr = findstr.replace("　", " ");
            String[] strs = findstr.split(" ");
            String str = "";
            for (int i = 0; i < strs.length; i++) {
                if (str != "") {
                    str = str + ",";
                }
                str = str + "'%" + strs[i] + "%'";
            }

            if (!isDelDataShow) {
                Delflg = " Delflg = false AND ";
            }

            // https://loglog.xyz/programming/jdbctemplate_in
            // https://qiita.com/naohide_a/items/3c78837ac7a1e05c6134
            // str を配列渡しすると、カンマが入るのでインジェクションチェックでNGになりSQL発行が出来ない。スペース区切りで%を付与するよう加工しているので、危険な構文は書けないと判断
            String sql = "SELECT * FROM m_server_a where " + Delflg + " ("
                    + "host_name ~~* any(array[" + str + "]) OR "
                    + "ip ~~* any(array[" + str + "]) OR "
                    + "note ~~* any(array[" + str + "]) )"
                    + "order by server_id asc limit :limit offset :offset ";
            MapSqlParameterSource Param = new MapSqlParameterSource()
                    .addValue("limit", limit)
                    .addValue("offset", offset);

            RowMapper<m_server> rowMapper = new BeanPropertyRowMapper<m_server>(m_server.class);
            // https://loglog.xyz/programming/jdbctemplate_in
            m_server_list = namedJdbcTemplate.query(sql, Param, rowMapper);
        }

        // サーバーデータが取得できなかった場合は、null値を返す
        if (m_server_list == null || m_server_list.size() == 0) {
            return null;
        }
        // 取得したサーバーデータをJSON文字列に変換し返却
        return getJsonData(m_server_list);
    }

    /**
     * サーバー履歴を取得する
     */
    @PostMapping(value = "/api/m_server_history_find", produces = "application/json;charset=utf-8")
    public String m_server_history_find(Number server_id, HttpServletResponse res) throws CloneNotSupportedException, IOException {
        String sql = "SELECT * FROM m_server_b where server_id = :server_id order by modify_count desc";
        RowMapper<m_server> rowMapper = new BeanPropertyRowMapper<m_server>(m_server.class);
        // https://loglog.xyz/programming/jdbctemplate_in
        MapSqlParameterSource Param = new MapSqlParameterSource()
                .addValue("server_id", server_id);
        List<m_server> m_server_list = namedJdbcTemplate.query(sql, Param, rowMapper);

        // サーバーデータが取得できなかった場合は、null値を返す
        if (m_server_list == null || m_server_list.size() == 0) {
            return null;
        }
        // 取得したサーバーデータをJSON文字列に変換し返却
        return getJsonData(m_server_list);
    }

    /**
     * サーバーデータを全件取得する
     * 
     * @return サーバーデータリスト(JSON形式)
     */
    @PostMapping(value = "/api/m_server_find_all", produces = "application/json;charset=utf-8")
    public String find_all(HttpServletResponse res) throws CloneNotSupportedException, IOException {
        List<m_server> m_server_list = m_server_service.searchAll();
        // サーバーデータが取得できなかった場合は、null値を返す
        if (m_server_list == null || m_server_list.size() == 0) {
            return null;
        }
        // 取得したサーバーデータをJSON文字列に変換し返却
        return getJsonData(m_server_list);
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
