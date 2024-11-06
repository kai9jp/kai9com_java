package kai9.com.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import kai9.com.dto.m_group1_Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kai9.com.model.m_group1;
import kai9.com.service.m_group1_Service;

/**
 * グループマスタ親 Controller
 */
@RestController
public class m_group1_Controller {

    @Autowired
    private m_group1_Service m_group1_service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedJdbcTemplate;

    /**
     * CUD操作
     */
    @PostMapping(value = { "/api/m_group1_create", "/api/m_group1_update", "/api/m_group1_delete" }, produces = "application/json;charset=utf-8")
    public void create(@RequestBody
    m_group1_Request m_group1, HttpServletResponse res, HttpServletRequest request) throws CloneNotSupportedException, IOException, JSONException {
        try {
            String URL = request.getRequestURI();
            String sql = "";
            m_group1 m_group1_result = null;
            if (URL.toLowerCase().contains("m_group1_create")) {
                // 新規
                m_group1_result = m_group1_service.create(m_group1);
            } else {
                // 排他制御
                // 更新回数が異なる場合エラー
                sql = "select modify_count1 from m_group1_a where group_id = ?";
                String modify_count = jdbcTemplate.queryForObject(sql, String.class, m_group1.getGroup_id());
                if (!modify_count.equals(String.valueOf(m_group1.getModify_count1()))) {
                    // JSON形式でレスポンスを返す
                    JsonResponse json = new JsonResponse();
                    json.setReturn_code(HttpStatus.CONFLICT.value());
                    json.setMsg("グループマスタ親　【" + m_group1.getGroup_id() + ":" + m_group1.getGroup_name() + "】　で排他エラー発生。ページリロード後に再登録して下さい。");
                    json.SetJsonResponse(res);
                    return;
                }

                if (URL.toLowerCase().contains("m_group1_update")) {
                    // 更新
                    m_group1_result = m_group1_service.update(m_group1);
                }

                if (URL.toLowerCase().contains("m_group1_delete")) {
                    // 削除(フラグON/OFF反転)
                    m_group1_result = m_group1_service.delete(m_group1);
                }
            }

            JsonResponse json = new JsonResponse();
            json.setReturn_code(HttpStatus.OK.value());
            json.setMsg("正常に登録しました");
            json.Add("group_id", String.valueOf(m_group1_result.getGroup_id()));
            json.Add("modify_count1", String.valueOf(m_group1_result.getModify_count1()));
            json.SetJsonResponse(res);
            return;

        } catch (Exception e) {
            // JSON形式でレスポンスを返す
            JsonResponse json = new JsonResponse();
            json.setReturn_code(HttpStatus.INTERNAL_SERVER_ERROR.value());// 500 Internal Server Error「何らかのサーバ内で起きたエラー」を返す
            json.setMsg(e.getMessage());
            json.SetJsonResponse(res);
            return;
        }
    }

    /**
     * 件数を返す
     */
    @PostMapping(value = "/api/m_group1_count", produces = "application/json;charset=utf-8")
    public String m_group1_count(String findstr, boolean isDelDataShow, HttpServletResponse res) throws CloneNotSupportedException, IOException {
        String Delflg = "";
        if (!isDelDataShow) Delflg = "and delflg = false ";

        String where = "";
        String all_count = "0";
        if (findstr != "") {
            // あいまい検索準備
            findstr = findstr.replace("　", " ");
            String[] strs = findstr.split(" ");
            Set<String> keys = new HashSet<>();
            for (String i : strs) {
                keys.add("%" + i + "%");
            }
            where = " and("
                    + " group_name like any(array[ :keys ])"
                    + " or"
                    + " note like any(array[ :keys ])"
                    + ")";
            MapSqlParameterSource Param = new MapSqlParameterSource();
            Param.addValue("keys", keys);

            String sql = "select count(*) from m_group1_a where 0 = 0 " + Delflg + where;
            all_count = namedJdbcTemplate.queryForObject(sql, Param, String.class);
        } else {
            String sql = "select count(*) from m_group1_a where 0 = 0 " + Delflg;
            all_count = jdbcTemplate.queryForObject(sql, String.class);
        }

        // レスポンスヘッダーのセット(JSON形式を指定)
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        // レスポンス生成
        String str = "{\"return_code\": " + HttpStatus.OK.value() + ","
                + " \"all_count\": " + all_count
                + "}";
        return str;
    }

//	  	/**
//	     * 専用クラス(m_group1_user_count_findで利用)
//	     */
//	  	@Data
//	  	class m_group2_user_count {
//	  	    public m_group2_user_count(String group_id, String count) {
//	  	    	this.group_id = group_id;
//	  	    	this.count = count;
//			}
//			private String group_id;
//	  	    private String count;
//	  	}
//	  	/**
//	     * グループー毎のユーザ件数を返す
//	  	 * ※ここは未使用だが、技術要素の参考に残す 
//	     */
//	  	@PostMapping(value = "/api/m_group1_user_count_find", produces = "application/json;charset=utf-8")
//		public void m_group1_user_count_find(HttpServletResponse res) throws CloneNotSupportedException, IOException, JSONException {
//	  		String sql = "SELECT group_id,COUNT(*) as count FROM m_group2_a Where delflg = false group by group_id,modify_count1";
//	  		
//	  		//https://loglog.xyz/programming/java/jdbctemplate_query_select#%E8%A4%87%E6%95%B0%E4%BB%B6%E3%82%92%E5%8F%96%E5%BE%97-2
//	  		List<Map<String, Object>> Dataset = jdbcTemplate.queryForList(sql);
//	  		List<m_group2_user_count> results = new ArrayList<m_group2_user_count>();
//	        for(Map<String, Object> data: Dataset) {
//	        	m_group2_user_count result = new m_group2_user_count(
//    			(String) data.get("group_id").toString(),
//    			(String) data.get("count").toString()
//    			);
//	        	results.add(result);
//	        }
//
//	        //JSON形式でレスポンスを返す
//    		JsonResponse json = new JsonResponse();
//    		json.setReturn_code(HttpStatus.OK.value());
//	    	json.AddArray("results",getJsonData(results));
//   	    	json.SetJsonResponse(res);
//	  	    return;
//	    }

    /**
     * 一覧を返す(ユーザマスタ入力画面のグループ選択リストボックス用)
     */
    @PostMapping(value = "/api/m_group1_find_all", produces = "application/json;charset=utf-8")
    public void m_group1_find_all(HttpServletResponse res) throws CloneNotSupportedException, IOException, JSONException {
        String sql = "select * from m_group1_a where delflg = false order by group_id";

        RowMapper<m_group1> rowMapper = new BeanPropertyRowMapper<m_group1>(m_group1.class);
        List<m_group1> m_group1_list = namedJdbcTemplate.query(sql, rowMapper);
        List<String> results = new ArrayList<String>();
        for (m_group1 group1 : m_group1_list) {
            results.add(group1.getGroup_id() + ":" + group1.getGroup_name());
        }

        // JSON形式でレスポンスを返す
        JsonResponse json = new JsonResponse();
        json.setReturn_code(HttpStatus.OK.value());
        json.AddArray("results", getJsonData(results));
        json.SetJsonResponse(res);
        return;
    }

    /**
     * 検索(ページネーション対応)
     */
    @PostMapping(value = "/api/m_group1_find", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String m_group1_find(Integer limit, Integer offset, String findstr, boolean isDelDataShow) throws CloneNotSupportedException, IOException {

        String Delflg = "";

        List<m_group1> m_group1_list = null;
        if (findstr.equals("")) {
            if (!isDelDataShow) {
                Delflg = "where delflg = false";
            }
            String sql = "select * from m_group1_a " + Delflg + " order by group_id asc limit ? offset ?";
            RowMapper<m_group1> rowMapper = new BeanPropertyRowMapper<m_group1>(m_group1.class);
            m_group1_list = jdbcTemplate.query(sql, rowMapper, limit, offset);
        } else {
            // あいまい検索準備
            findstr = findstr.replace("　", " ");
            String[] strs = findstr.split(" ");
            Set<String> keys = new HashSet<>();
            for (String i : strs) {
                keys.add("%" + i + "%");
            }

            if (!isDelDataShow) Delflg = " and delflg = false ";

            String sql = "select * from m_group1_a where 0 = 0" + Delflg + " and ("
                    + " group_name like any(array[ :keys ])" // :keysの前後にスペースが必要なので注意(springのバグ有り)
                    + " or"
                    + " note like any(array[ :keys ])"
                    + " )"
                    + " order by group_id asc limit :limit offset :offset ";
            MapSqlParameterSource Param = new MapSqlParameterSource();
            Param.addValue("limit", limit);
            Param.addValue("offset", offset);
            Param.addValue("keys", keys);

            RowMapper<m_group1> rowMapper = new BeanPropertyRowMapper<m_group1>(m_group1.class);
            // https://loglog.xyz/programming/jdbctemplate_in
            m_group1_list = namedJdbcTemplate.query(sql, Param, rowMapper);
        }

        // グループーデータが取得できなかった場合は、null値を返す
        if (m_group1_list == null || m_group1_list.size() == 0) {
            return null;
        }

        // 各親に対する子データ件数情報を付与する
        String sql2 = "select group_id,count(*) as count from m_group2_a where delflg = false group by group_id,modify_count1";
        List<Map<String, Object>> Dataset = jdbcTemplate.queryForList(sql2);
        for (Map<String, Object> data : Dataset) {
            for (m_group1 group1 : m_group1_list) {
                if (data.get("group_id").toString().matches(String.valueOf(group1.getGroup_id()))) {
                    group1.setUser_count(Integer.valueOf(data.get("count").toString()));
                }
            }
        }

        // 取得データをJSON文字列に変換し返却
        return getJsonData(m_group1_list);
    }

    /**
     * 履歴検索
     */
    @PostMapping(value = "/api/m_group1_history_find", produces = "application/json;charset=utf-8")
    public String m_group1_history_find(Number group_id, HttpServletResponse res) throws CloneNotSupportedException, IOException {
        String sql = "select * from m_group1_b where group_id = :group_id order by modify_count1 desc";
        RowMapper<m_group1> rowMapper = new BeanPropertyRowMapper<m_group1>(m_group1.class);
        MapSqlParameterSource Param = new MapSqlParameterSource()
                .addValue("group_id", group_id);
        List<m_group1> m_group1_list = namedJdbcTemplate.query(sql, Param, rowMapper);

        // データが取得できなかった場合は、null値を返す
        if (m_group1_list == null || m_group1_list.size() == 0) {
            return null;
        }
        // 取得したグループーデータをJSON文字列に変換し返却
        return getJsonData(m_group1_list);
    }

    /**
     * 引数のオブジェクトをJSON文字列に変換
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
