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
import kai9.com.dto.m_group2_Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kai9.com.model.m_group2;
import kai9.com.service.m_group2_Service;

import lombok.Data;

/**
 * グループー情報 Controller
 */
@RestController
public class m_group2_Controller {

    /**
     * グループーマスタ Service
     */
    @Autowired
    private m_group2_Service m_group2_service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedJdbcTemplate;

    @Data
    public static class m_group2_post_Result {
        private Integer group_id;
        private List<m_group2_Request> m_group2_requests;
    }

    /**
     * グループ2登録
     */
    @PostMapping(value = { "/api/m_group2_post" }, produces = "application/json;charset=utf-8")
    public void m_group2_post(@RequestBody
    m_group2_post_Result reqdata, HttpServletResponse res) throws CloneNotSupportedException, IOException, JSONException {

        try {
            // 同一グループのgroup2を全件取得
            String sql = "SELECT * FROM m_group2_a Where group_id = :group_id order by group_id";
            MapSqlParameterSource Param = new MapSqlParameterSource().addValue("group_id", reqdata.group_id);
            RowMapper<m_group2> rowMapper = new BeanPropertyRowMapper<m_group2>(m_group2.class);
            List<m_group2> m_group2_list = namedJdbcTemplate.query(sql, Param, rowMapper);

            // 処理別の分配器を用意
            List<m_group2_Request> m_group2_list_create = new ArrayList<m_group2_Request>();
            List<m_group2_Request> m_group2_list_update = new ArrayList<m_group2_Request>();
            List<m_group2_Request> m_group2_list_ng = new ArrayList<m_group2_Request>();

            // 処理の振り分け
            for (m_group2_Request m_group2_Request : reqdata.m_group2_requests) {
                boolean hitFlg = false;
                for (m_group2 db : m_group2_list) {
                    if (m_group2_Request.getUser_id() == db.getUser_id()) {
                        hitFlg = true;
                        if (m_group2_Request.getModify_count2() == db.getModify_count2()) {
                            // 既存データがある場合更新対象とする
                            m_group2_list_update.add(m_group2_Request);
                        } else {
                            // 既存データの更新回数が異なる場合、排他NGとする
                            m_group2_list_ng.add(m_group2_Request);
                        }
                        break;
                    }
                }
                if (!hitFlg) {
                    // 既存データが無い場合、新規対象とする
                    m_group2_list_create.add(m_group2_Request);
                }
            }

            // データを登録
            List<m_group2> m_group2_list_result = new ArrayList<m_group2>();
            for (m_group2_Request m_group2_Request : m_group2_list_create) {
                m_group2_list_result.add(m_group2_service.create(m_group2_Request));
            }
            for (m_group2_Request m_group2_Request : m_group2_list_update) {
                m_group2_list_result.add(m_group2_service.update(m_group2_Request));
            }
            List<String> error_str_list = new ArrayList<String>();
            for (m_group2_Request m_group2_Request : m_group2_list_ng) {
                m_group2 m_group2_result = new m_group2();
                m_group2_result.setGroup_id(m_group2_Request.getGroup_id());
                m_group2_result.setModify_count1(m_group2_Request.getModify_count1());
                m_group2_result.setUser_id(m_group2_Request.getUser_id());
                m_group2_result.setModify_count2(m_group2_Request.getModify_count2());
                m_group2_result.setUpdate_u_id(m_group2_Request.getUpdate_u_id());
                m_group2_result.setUpdate_date(m_group2_Request.getUpdate_date());
                m_group2_result.setDelflg(m_group2_Request.isDelflg());
                m_group2_list_result.add(m_group2_result);
                error_str_list.add(String.valueOf(m_group2_Request.getUser_id()));
            }
            JsonResponse json = new JsonResponse();
            if (error_str_list.size() != 0) {
                // 更新回数が異なる場合エラー
                // JSON形式でレスポンスを返す
                json.setReturn_code(HttpStatus.CONFLICT.value());
                json.setMsg("<BR>排他エラー発生。ページリロード後に再登録して下さい。<BR>NG対象:user_id=" + String.join(",", error_str_list));
            } else {
                json.setReturn_code(HttpStatus.OK.value());
                json.setMsg("正常に登録しました");
            }

            json.AddArray("results", getJsonData(m_group2_list_result));
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
     * 子の件数を返す
     */
    @PostMapping(value = "/api/m_group2_count", produces = "application/json;charset=utf-8")
    public void m_group2_count(HttpServletResponse res) throws CloneNotSupportedException, IOException, JSONException {
        String sql = "SELECT group_id,COUNT(*) as count FROM m_group2_a Where delflg = false group by group_id,modify_count1";
        List<Map<String, Object>> sql_result = jdbcTemplate.queryForList(sql);

        List<JSONObject> jso_list = new ArrayList<JSONObject>();
        for (Map<String, Object> sqlStr : sql_result) {
            JSONObject jso = new JSONObject("");
            jso.put("group_id", sqlStr.get("group_id"));
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
     * 子データをリストで返す
     */
    @PostMapping(value = "/api/m_group2_find", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String m_group2_find(Integer group_id) throws CloneNotSupportedException, IOException {

        String sql = "SELECT * FROM m_group2_a where group_id = :group_id order by user_id";
        MapSqlParameterSource Param = new MapSqlParameterSource().addValue("group_id", group_id);
        RowMapper<m_group2> rowMapper = new BeanPropertyRowMapper<m_group2>(m_group2.class);
        List<m_group2> m_group2_list = namedJdbcTemplate.query(sql, Param, rowMapper);

        // グループーデータが取得できなかった場合は、null値を返す
        if (m_group2_list == null || m_group2_list.size() == 0) {
            return null;
        }
        // 取得したグループーデータをJSON文字列に変換し返却
        return getJsonData(m_group2_list);
    }

    /**
     * グループー履歴を取得する
     */
    @PostMapping(value = "/api/m_group2_history_find", produces = "application/json;charset=utf-8")
    public String m_group2_history_find(Number group_id, Number user_id, HttpServletResponse res) throws CloneNotSupportedException, IOException {
        String sql = "SELECT * FROM m_group2_b where group_id = :group_id and user_id = :user_id  order by modify_count2 desc";
        RowMapper<m_group2> rowMapper = new BeanPropertyRowMapper<m_group2>(m_group2.class);
        MapSqlParameterSource Param = new MapSqlParameterSource()
                .addValue("group_id", group_id)
                .addValue("user_id", user_id);
        List<m_group2> m_group2_list = namedJdbcTemplate.query(sql, Param, rowMapper);

        // グループーデータが取得できなかった場合は、null値を返す
        if (m_group2_list == null || m_group2_list.size() == 0) {
            return null;
        }
        // 取得したグループーデータをJSON文字列に変換し返却
        return getJsonData(m_group2_list);
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
