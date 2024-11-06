package kai9.com.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
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
import kai9.com.dto.m_jobserver_Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kai9.com.model.m_jobserver;
import kai9.com.service.m_jobserver_Service;

import lombok.Data;

/**
 * ジョブ対象サーバ Controller
 */
@RestController
public class m_jobserver_Controller {

    @Autowired
    private m_jobserver_Service m_jobserver_service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedJdbcTemplate;

    @Data
    public static class m_jobserver_post_Result {
        private Integer jobinfo_id;
        private List<m_jobserver_Request> m_jobserver_requests;
    }

    /**
     * 登録
     */
    @PostMapping(value = { "/api/m_jobserver_post" }, produces = "application/json;charset=utf-8")
    public void m_jobserver_post(@RequestBody
    m_jobserver_post_Result reqdata, HttpServletResponse res) throws CloneNotSupportedException, IOException, JSONException {

        try {
            // 同一ジョブ情報IDのm_jobserver_aを全件取得
            String sql = "SELECT * FROM m_jobserver_a Where jobinfo_id = :jobinfo_id order by jobinfo_id";
            MapSqlParameterSource Param = new MapSqlParameterSource().addValue("jobinfo_id", reqdata.jobinfo_id);
            RowMapper<m_jobserver> rowMapper = new BeanPropertyRowMapper<m_jobserver>(m_jobserver.class);
            List<m_jobserver> m_jobserver_list = namedJdbcTemplate.query(sql, Param, rowMapper);

            // 処理別の分配器を用意
            List<m_jobserver_Request> m_jobserver_list_create = new ArrayList<m_jobserver_Request>();
            List<m_jobserver_Request> m_jobserver_list_update = new ArrayList<m_jobserver_Request>();
            List<m_jobserver_Request> m_jobserver_list_ng = new ArrayList<m_jobserver_Request>();

            // 処理の振り分け
            for (m_jobserver_Request m_jobserver_req : reqdata.m_jobserver_requests) {
                boolean hitFlg = false;
                for (m_jobserver m_jobserver_db : m_jobserver_list) {
                    if (m_jobserver_req.getServer_id() == m_jobserver_db.getServer_id()) {
                        hitFlg = true;
                        if (m_jobserver_req.getModify_count() == m_jobserver_db.getModify_count()) {
                            // 既存データがある場合更新対象とする
                            m_jobserver_list_update.add(m_jobserver_req);
                        } else {
                            // 既存データの更新回数が異なる場合、排他NGとする
                            m_jobserver_list_ng.add(m_jobserver_req);
                        }
                        break;
                    }
                }
                if (!hitFlg) {
                    // 既存データが無い場合、新規対象とする
                    m_jobserver_list_create.add(m_jobserver_req);
                }
            }

            // データを登録
            List<m_jobserver> m_jobserver_list_result = new ArrayList<m_jobserver>();
            for (m_jobserver_Request m_jobserver_req : m_jobserver_list_create) {
                m_jobserver_list_result.add(m_jobserver_service.create(m_jobserver_req));
            }
            for (m_jobserver_Request m_jobserver_req : m_jobserver_list_update) {
                m_jobserver_list_result.add(m_jobserver_service.update(m_jobserver_req));
            }
            List<String> error_str_list = new ArrayList<String>();
            for (m_jobserver_Request m_jobserver_req : m_jobserver_list_ng) {
                m_jobserver m_jobserver_result = new m_jobserver();
                m_jobserver_result.setJobinfo_id(m_jobserver_req.getJobinfo_id());
                m_jobserver_result.setServer_id(m_jobserver_req.getServer_id());
                m_jobserver_result.setModify_count(m_jobserver_req.getModify_count());
                m_jobserver_result.setUpdate_u_id(m_jobserver_req.getUpdate_u_id());
                m_jobserver_result.setUpdate_date(m_jobserver_req.getUpdate_date());
                m_jobserver_result.setDelflg(m_jobserver_req.isDelflg());
                m_jobserver_list_result.add(m_jobserver_result);
                error_str_list.add(String.valueOf(m_jobserver_req.getServer_id()));
            }
            JsonResponse json = new JsonResponse();
            if (error_str_list.size() != 0) {
                // 更新回数が異なる場合エラー
                // JSON形式でレスポンスを返す
                json.setReturn_code(HttpStatus.CONFLICT.value());
                json.setMsg("<BR>排他エラー発生。ページリロード後に再登録して下さい。<BR>NG対象:server_id=" + String.join(",", error_str_list));
            } else {
                json.setReturn_code(HttpStatus.OK.value());
                json.setMsg("正常に登録しました");
            }

            json.AddArray("results", getJsonData(m_jobserver_list_result));
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
     * 件数を返す
     */
    @PostMapping(value = "/api/m_jobserver_count", produces = "application/json;charset=utf-8")
    public void m_jobserver_count(HttpServletResponse res) throws CloneNotSupportedException, IOException, JSONException {
        String sql = "SELECT jobinfo_id,COUNT(*) as count FROM m_jobserver_a Where delflg = false group by jobinfo_id";
        List<Map<String, Object>> sql_result = jdbcTemplate.queryForList(sql);

        List<JSONObject> jso_list = new ArrayList<JSONObject>();
        for (Map<String, Object> sqlStr : sql_result) {
            JSONObject jso = new JSONObject("");
            jso.put("jobinfo_id", sqlStr.get("jobinfo_id"));
            jso.put("count", sqlStr.get("count"));
            jso_list.add(jso);
        }

        // JSON形式でレスポンスを返す
        JsonResponse json = new JsonResponse();
        json.setReturn_code(HttpStatus.OK.value());
        json.Add("results", getJsonData(jso_list));
        json.SetJsonResponse(res);
        return;
    }

    /**
     * データ取得
     */
    @PostMapping(value = "/api/m_jobserver_find", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String m_jobserver_find(Integer jobinfo_id) throws CloneNotSupportedException, IOException {

        String sql = "SELECT * FROM m_jobserver_a where jobinfo_id = :jobinfo_id order by server_id";
        MapSqlParameterSource Param = new MapSqlParameterSource().addValue("jobinfo_id", jobinfo_id);
        RowMapper<m_jobserver> rowMapper = new BeanPropertyRowMapper<m_jobserver>(m_jobserver.class);
        List<m_jobserver> m_jobserver_list = namedJdbcTemplate.query(sql, Param, rowMapper);

        // グループーデータが取得できなかった場合は、null値を返す
        if (m_jobserver_list == null || m_jobserver_list.size() == 0) {
            return null;
        }
        // 取得したグループーデータをJSON文字列に変換し返却
        return getJsonData(m_jobserver_list);
    }

    /**
     * 履歴取得
     */
    @PostMapping(value = "/api/m_jobserver_history_find", produces = "application/json;charset=utf-8")
    public String m_jobserver_history_find(Number jobinfo_id, Number server_id, HttpServletResponse res) throws CloneNotSupportedException, IOException {
        String sql = "SELECT * FROM m_jobserver_b where jobinfo_id = :jobinfo_id and server_id = :server_id  order by modify_count desc";
        RowMapper<m_jobserver> rowMapper = new BeanPropertyRowMapper<m_jobserver>(m_jobserver.class);
        MapSqlParameterSource Param = new MapSqlParameterSource()
                .addValue("jobinfo_id", jobinfo_id)
                .addValue("server_id", server_id);
        List<m_jobserver> m_jobserver_list = namedJdbcTemplate.query(sql, Param, rowMapper);

        // データが取得できなかった場合は、null値を返す
        if (m_jobserver_list == null || m_jobserver_list.size() == 0) {
            return null;
        }
        // 取得したデータをJSON文字列に変換し返却
        return getJsonData(m_jobserver_list);
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
